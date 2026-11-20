package app.studydesk.core.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import app.studydesk.core.reader.markdown.Chemistry
import app.studydesk.core.reader.markdown.Inline
import app.studydesk.core.reader.markdown.plain

/** Colours the reader paints on top of text. */
data class TextMarks(
    val highlights: List<Pair<IntRange, Color>> = emptyList(),
    val matches: List<IntRange> = emptyList(),
    val currentMatch: IntRange? = null,
    val speaking: IntRange? = null,
    val matchColor: Color,
    val currentMatchColor: Color,
    val speakingColor: Color,
    val codeBackground: Color,
    val linkColor: Color,
)

/**
 * Builds the styled text of one block. Offsets line up with the block's plain text, so
 * highlights, find matches and the sentence being read can be drawn on top.
 */
fun buildBlockText(runs: List<Inline>, marks: TextMarks): AnnotatedString = buildAnnotatedString {
    for (run in runs) {
        val start = length
        when (run) {
            is Inline.Text -> {
                append(run.text)
                if (run.bold) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, length)
                if (run.italic) addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
            }
            is Inline.Code -> {
                append(run.text)
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = marks.codeBackground), start, length)
            }
            is Inline.Link -> {
                withLink(
                    LinkAnnotation.Url(
                        run.url,
                        TextLinkStyles(SpanStyle(color = marks.linkColor, textDecoration = TextDecoration.Underline)),
                    ),
                ) { append(run.text) }
            }
        }
    }
    for (mark in Chemistry.marks(runs.plain())) {
        addStyle(
            SpanStyle(baselineShift = if (mark.superscript) BaselineShift.Superscript else BaselineShift.Subscript, fontSize = 0.72.em),
            mark.start,
            mark.end,
        )
    }
    for ((range, color) in marks.highlights) addStyle(SpanStyle(background = color), range.first, range.last + 1)
    for (range in marks.matches) addStyle(SpanStyle(background = marks.matchColor), range.first, range.last + 1)
    marks.currentMatch?.let { addStyle(SpanStyle(background = marks.currentMatchColor), it.first, it.last + 1) }
    marks.speaking?.let { addStyle(SpanStyle(background = marks.speakingColor, textDecoration = TextDecoration.Underline), it.first, it.last + 1) }
}

fun plainAnnotated(text: String, marks: TextMarks): AnnotatedString = buildBlockText(listOf(Inline.Text(text)), marks)

/** Keeps ranges inside the text so a stale highlight never crashes the reader. */
internal fun IntRange.clampTo(length: Int): IntRange? {
    val s = first.coerceIn(0, length)
    val e = (last + 1).coerceIn(s, length)
    return if (e > s) s until e else null
}

internal val NoMarks = TextMarks(
    matchColor = Color.Transparent,
    currentMatchColor = Color.Transparent,
    speakingColor = Color.Transparent,
    codeBackground = Color.Transparent,
    linkColor = Color.Unspecified,
)

