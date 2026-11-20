package app.studydesk.core.reader.markdown

import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

/** Turns a note's Markdown into [Block]s. Pure Kotlin + commonmark, so it is unit-tested on the JVM. */
object MarkdownParser {
    private val parser: Parser = Parser.builder().extensions(listOf(TablesExtension.create())).build()

    fun parse(markdown: String): List<Block> {
        val out = mutableListOf<Block>()
        val anchors = mutableMapOf<String, Int>()
        var node = parser.parse(markdown).firstChild
        while (node != null) {
            block(node, out, depth = 0, anchors)
            node = node.next
        }
        return out
    }

    private fun block(node: Node, out: MutableList<Block>, depth: Int, anchors: MutableMap<String, Int>) {
        when (node) {
            is Heading -> {
                val runs = inlines(node)
                val base = slug(runs.plain())
                val n = anchors.merge(base, 1, Int::plus)!!
                out += Block.Heading(node.level, runs, if (n == 1) base else "$base-$n")
            }
            is Paragraph -> {
                // A paragraph that is only an image becomes an image block.
                val only = node.firstChild
                if (only is Image && only.next == null) {
                    out += Block.Image(only.destination, inlines(only).plain())
                } else {
                    splitImages(node, out)
                }
            }
            is BulletList -> listItems(node, out, depth, anchors) { "•" }
            is OrderedList -> {
                val start = node.markerStartNumber ?: 1
                listItems(node, out, depth, anchors) { index -> "${start + index}." }
            }
            is BlockQuote -> {
                var child = node.firstChild
                while (child != null) {
                    if (child is Paragraph) out += Block.Quote(inlines(child)) else block(child, out, depth, anchors)
                    child = child.next
                }
            }
            is FencedCodeBlock -> out += Block.Code(node.literal.trimEnd('\n'))
            is IndentedCodeBlock -> out += Block.Code(node.literal.trimEnd('\n'))
            is ThematicBreak -> out += Block.Rule
            is TableBlock -> out += table(node)
            is HtmlBlock -> Unit // notes are plain Markdown; ignore stray HTML
            else -> {
                var child = node.firstChild
                while (child != null) {
                    block(child, out, depth, anchors)
                    child = child.next
                }
            }
        }
    }

    private fun listItems(list: Node, out: MutableList<Block>, depth: Int, anchors: MutableMap<String, Int>, marker: (Int) -> String) {
        var item = list.firstChild
        var index = 0
        while (item != null) {
            if (item is ListItem) {
                var child = item.firstChild
                var first = true
                while (child != null) {
                    when {
                        child is Paragraph && first -> out += Block.ListItem(inlines(child), depth, marker(index))
                        child is Paragraph -> out += Block.ListItem(inlines(child), depth, "")
                        child is BulletList || child is OrderedList -> block(child, out, depth + 1, anchors)
                        else -> block(child, out, depth, anchors)
                    }
                    first = false
                    child = child.next
                }
                index++
            }
            item = item.next
        }
    }

    /** A paragraph that mixes text and images is split so each image is its own zoomable block. */
    private fun splitImages(paragraph: Paragraph, out: MutableList<Block>) {
        val runs = mutableListOf<Inline>()
        var child = paragraph.firstChild
        while (child != null) {
            if (child is Image) {
                if (runs.plain().isNotBlank()) out += Block.Paragraph(runs.toList())
                runs.clear()
                out += Block.Image(child.destination, inlines(child).plain())
            } else {
                collect(child, runs, bold = false, italic = false)
            }
            child = child.next
        }
        if (runs.plain().isNotBlank()) out += Block.Paragraph(merge(runs))
    }

    private fun table(node: TableBlock): Block.Table {
        var header: List<List<Inline>> = emptyList()
        val rows = mutableListOf<List<List<Inline>>>()
        fun row(r: TableRow): List<List<Inline>> {
            val cells = mutableListOf<List<Inline>>()
            var c = r.firstChild
            while (c != null) {
                if (c is TableCell) cells += inlines(c)
                c = c.next
            }
            return cells
        }
        var section = node.firstChild
        while (section != null) {
            var r = section.firstChild
            while (r != null) {
                if (r is TableRow) {
                    if (section is TableHead) header = row(r) else rows += row(r)
                }
                r = r.next
            }
            section = section.next
        }
        return Block.Table(header, rows)
    }

    private fun inlines(node: Node): List<Inline> {
        val runs = mutableListOf<Inline>()
        var child = node.firstChild
        while (child != null) {
            collect(child, runs, bold = false, italic = false)
            child = child.next
        }
        return merge(runs)
    }

    private fun collect(node: Node, runs: MutableList<Inline>, bold: Boolean, italic: Boolean) {
        when (node) {
            is Text -> runs += Inline.Text(node.literal, bold, italic)
            is Code -> runs += Inline.Code(node.literal)
            is SoftLineBreak -> runs += Inline.Text(" ", bold, italic)
            is HardLineBreak -> runs += Inline.Text("\n", bold, italic)
            is StrongEmphasis -> children(node) { collect(it, runs, true, italic) }
            is Emphasis -> children(node) { collect(it, runs, bold, true) }
            is Link -> {
                val text = mutableListOf<Inline>()
                children(node) { collect(it, text, bold, italic) }
                runs += Inline.Link(text.plain(), node.destination)
            }
            is Image -> runs += Inline.Text(inlines(node).plain(), bold, italic)
            is HtmlInline -> Unit
            else -> children(node) { collect(it, runs, bold, italic) }
        }
    }

    private inline fun children(node: Node, action: (Node) -> Unit) {
        var child = node.firstChild
        while (child != null) {
            action(child)
            child = child.next
        }
    }

    /** Joins neighbouring runs with the same style so highlights and search see whole words. */
    private fun merge(runs: List<Inline>): List<Inline> {
        val out = mutableListOf<Inline>()
        for (r in runs) {
            val last = out.lastOrNull()
            if (r is Inline.Text && last is Inline.Text && last.bold == r.bold && last.italic == r.italic) {
                out[out.lastIndex] = last.copy(text = last.text + r.text)
            } else {
                out += r
            }
        }
        return out
    }

    internal fun slug(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "section" }
}
