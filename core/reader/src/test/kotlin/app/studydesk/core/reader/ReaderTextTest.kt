package app.studydesk.core.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.BaselineShift
import app.studydesk.core.reader.markdown.Inline
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTextTest {
    private val marks = TextMarks(
        matchColor = Color.Yellow,
        currentMatchColor = Color.Red,
        speakingColor = Color.Blue,
        codeBackground = Color.Gray,
        linkColor = Color.Blue,
    )

    @Test
    fun formulaDigitsAreSubscriptInTheStyledText() {
        val text = buildBlockText(listOf(Inline.Text("cold KMnO4 and "), Inline.Text("C6H6", bold = true)), marks)
        val subscripts = text.spanStyles
            .filter { it.item.baselineShift == BaselineShift.Subscript }
            .map { text.text.substring(it.start, it.end) }
        assertEquals(listOf("4", "6", "6"), subscripts)
    }

    @Test
    fun highlightAndMatchRangesLineUpWithPlainText() {
        val text = buildBlockText(
            listOf(Inline.Text("Benzene is "), Inline.Text("aromatic", bold = true)),
            marks.copy(highlights = listOf(11..18 to Color.Green), matches = listOf(0..6)),
        )
        val green = text.spanStyles.single { it.item.background == Color.Green }
        assertEquals("aromatic", text.text.substring(green.start, green.end))
        val match = text.spanStyles.single { it.item.background == Color.Yellow }
        assertEquals("Benzene", text.text.substring(match.start, match.end))
    }
}
