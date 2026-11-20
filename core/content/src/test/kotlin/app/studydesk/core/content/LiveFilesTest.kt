package app.studydesk.core.content

import app.studydesk.core.model.Catalog
import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.ContentManifest
import app.studydesk.core.model.ExamSchedule
import app.studydesk.core.model.SubjectPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The real files published on 2026-11-18, read with the app's own model classes. */
class LiveFilesTest {
    private fun bytes(path: String) = javaClass.getResourceAsStream("/live/v1/$path")!!.readBytes()
    private val manifest = ContentJson.decodeFromString<ContentManifest>(bytes("manifest.json").decodeToString())

    @Test
    fun everyFileMatchesItsManifestChecksum() {
        for (entry in manifest.textFiles) {
            val b = bytes(entry.path)
            assertEquals(entry.path, entry.bytes, b.size.toLong())
            assertEquals(entry.path, entry.sha256, sha256(b))
        }
    }

    @Test
    fun catalogReadsAndLinksPracticalToTheoryPack() {
        val catalog = ContentJson.decodeFromString<Catalog>(bytes("catalog.json").decodeToString())
        assertEquals(listOf("degree", "education", "nursing", "pharmacy"), catalog.streams.map { it.id })
        val practical = catalog.allSubjects.first { it.subject.code == "BP305P" }.subject
        assertEquals("pharmacy.bpharm.pci.sem3.bp301t", practical.pack)
        assertTrue(practical.isPractical)
        assertNotNull(catalog.examBody("hptu"))
    }

    @Test
    fun packReads() {
        val pack = ContentJson.decodeFromString<SubjectPack>(bytes("packs/pharmacy.bpharm.pci.sem3.bp301t.json").decodeToString())
        assertEquals(5, pack.units.size)
        assertEquals(65, pack.questions.size)
        assertEquals(54, pack.mcqs.size)
        assertEquals(10, pack.practicals.size)
        assertTrue(pack.mcqs.all { it.correct in it.options.indices })
    }

    @Test
    fun examsRead() {
        val hptu = ContentJson.decodeFromString<ExamSchedule>(bytes("exams/hptu.json").decodeToString())
        val paper = hptu.sessions.single().papers.first { it.subjectCode == "BP301T" && it.schemeId == "pci" }
        assertEquals("2026-12-03", paper.date)
        assertTrue(hptu.sessions.single().isTentative)
    }
}
