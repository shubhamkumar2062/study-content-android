package app.studydesk.core.reader

import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.Question
import app.studydesk.core.model.SubjectPack
import app.studydesk.core.reader.markdown.AskYourself
import app.studydesk.core.reader.markdown.Block
import app.studydesk.core.reader.markdown.Chemistry
import app.studydesk.core.reader.markdown.Inline
import app.studydesk.core.reader.markdown.MarkdownParser
import app.studydesk.core.reader.markdown.ReaderDocs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun headingsParagraphsListsAndStyles() {
        val blocks = MarkdownParser.parse(
            """
            # Unit 1: Benzene
            Benzene is **very stable** and *planar*.

            - first
            - second
              1. nested
            """.trimIndent(),
        )
        val heading = blocks[0] as Block.Heading
        assertEquals(1, heading.level)
        assertEquals("unit-1-benzene", heading.anchor)
        val para = blocks[1] as Block.Paragraph
        assertEquals("Benzene is very stable and planar.", para.plainText)
        assertTrue(para.runs.any { it is Inline.Text && it.bold && it.text == "very stable" })
        val items = blocks.filterIsInstance<Block.ListItem>()
        assertEquals(listOf("first" to 0, "second" to 0, "nested" to 1), items.map { it.plainText to it.depth })
        assertEquals("1.", items[2].marker)
    }

    @Test
    fun tablesCodeImagesAndDuplicateAnchors() {
        val blocks = MarkdownParser.parse(
            """
            ## Uses
            | Name | Value |
            | --- | --- |
            | Benzene | 1.39 Å |

            ```
              /\
             /  \
            ```

            ![Ring](media/ring.svg)

            ## Uses
            """.trimIndent(),
        )
        val table = blocks.filterIsInstance<Block.Table>().single()
        assertEquals(listOf("Name", "Value"), table.header.map { cell -> cell.joinToString("") { it.text } })
        assertEquals("1.39 Å", table.rows.single()[1].joinToString("") { it.text })
        assertEquals("  /\\\n /  \\", blocks.filterIsInstance<Block.Code>().single().text)
        assertEquals("media/ring.svg", blocks.filterIsInstance<Block.Image>().single().source)
        assertEquals(listOf("uses", "uses-2"), blocks.filterIsInstance<Block.Heading>().map { it.anchor })
    }

    @Test
    fun chemicalFormulasGetSubscriptsButCodesDoNot() {
        fun formatted(text: String) = Chemistry.marks(text).map { text.substring(it.start, it.end) to it.superscript }
        assertEquals(listOf("2" to false, "4" to false), formatted("conc. H2SO4 is added"))
        assertEquals(listOf("2" to false, "+" to true), formatted("the NO2+ ion"))
        assertEquals(listOf("6" to false, "5" to false, "2" to false), formatted("C6H5NH2"))
        assertEquals(listOf("3" to false), formatted("AlCl3"))
        assertEquals(listOf("2" to false), formatted("Cl2 in light"))
        assertEquals(emptyList<Pair<String, Boolean>>(), formatted("BP301T vitamin B12 and K3 via SN1, BHC, DDT"))
    }

    @Test
    fun realBp301tNotesParse() {
        val pack = ContentJson.decodeFromString<SubjectPack>(
            javaClass.getResourceAsStream("/bp301t.json")!!.readBytes().decodeToString(),
        )
        val docs = ReaderDocs.all(pack).flatten()
        assertEquals(5 + 2 + 10, docs.size)
        for (doc in docs) {
            val blocks = MarkdownParser.parse(doc.markdown)
            assertTrue("${doc.id} has blocks", blocks.size > 3)
        }
        val unit1 = MarkdownParser.parse(docs.first { it.id == "unit-1" }.markdown)
        assertTrue(unit1.any { it is Block.Table })
        assertTrue(unit1.any { it is Block.Code })
    }

    @Test
    fun askYourselfPutsQuestionsUnderTheMatchingSection() {
        val blocks = MarkdownParser.parse("# Unit 1\n## 1.1 Structure of benzene\ntext\n## 1.4 Nitration and sulfonation\ntext")
        fun q(id: String, text: String, p: String = "high") = Question(id, 1, "short", 5, p, text, "answer")
        val placed = AskYourself.place(
            blocks,
            listOf(
                q("a", "Explain the orbital structure of benzene."),
                q("b", "Write the mechanism of nitration and sulfonation of benzene."),
                q("c", "Structure of benzene in detail", "low"),
                q("d", "Name two uses of DDT."),
            ),
        )
        assertEquals(listOf("a"), placed["1-1-structure-of-benzene"]?.map { it.id })
        assertEquals(listOf("b"), placed["1-4-nitration-and-sulfonation"]?.map { it.id })
        assertEquals(2, placed.values.sumOf { it.size })
    }
}
