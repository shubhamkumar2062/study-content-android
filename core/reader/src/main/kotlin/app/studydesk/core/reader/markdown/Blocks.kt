package app.studydesk.core.reader.markdown

/**
 * A note split into blocks the reader draws one by one (so long units scroll smoothly).
 * Inline text is kept as [Inline] runs so it can be styled, highlighted and searched.
 */
sealed interface Block {
    /** The block's text as the student sees it, used for search, highlights and read aloud. */
    val plainText: String

    data class Heading(val level: Int, val runs: List<Inline>, val anchor: String) : Block {
        override val plainText = runs.plain()
    }

    data class Paragraph(val runs: List<Inline>) : Block {
        override val plainText = runs.plain()
    }

    /** One list item; nesting is kept as [depth] so the list can be drawn flat in a lazy list. */
    data class ListItem(val runs: List<Inline>, val depth: Int, val marker: String) : Block {
        override val plainText = runs.plain()
    }

    data class Quote(val runs: List<Inline>) : Block {
        override val plainText = runs.plain()
    }

    /** Fenced code: in these notes, text diagrams. Never wrapped; scrolls sideways. */
    data class Code(val text: String) : Block {
        override val plainText = text
    }

    data class Table(val header: List<List<Inline>>, val rows: List<List<List<Inline>>>) : Block {
        override val plainText = (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(" | ") { it.plain() } }
    }

    data class Image(val source: String, val alt: String) : Block {
        override val plainText = alt
    }

    data object Rule : Block {
        override val plainText = ""
    }
}

sealed interface Inline {
    val text: String

    data class Text(override val text: String, val bold: Boolean = false, val italic: Boolean = false) : Inline
    data class Code(override val text: String) : Inline
    data class Link(override val text: String, val url: String) : Inline
}

fun List<Inline>.plain(): String = joinToString("") { it.text }
