package app.studydesk.core.reader.export

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import app.studydesk.core.model.Question
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Lays notes out as a clean printable page and opens Android's print screen, where
 * "Save as PDF" works on every phone from Android 8 with no storage permission
 * (the student picks the folder). Needs an Activity context.
 */
object ContentPrinter {
    private val extensions = listOf(TablesExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build()

    // The print adapter needs its WebView alive until printing finishes; keep the latest one.
    private var keepAlive: WebView? = null

    /**
     * [sections]: title and Markdown of each part (a unit, or every unit of a subject).
     * [notice]: printed under the title, e.g. that a teacher hasn't checked the notes yet.
     */
    fun printNotes(
        context: Context,
        jobTitle: String,
        subtitle: String,
        sections: List<Pair<String, String>>,
        notice: String? = null,
        baseUrl: String? = null,
    ) {
        val body = sections.joinToString("\n") { (_, md) -> "<section>${renderer.render(parser.parse(md))}</section>" }
        print(context, jobTitle, page(jobTitle, subtitle, body, notice), baseUrl)
    }

    fun printQuestions(
        context: Context,
        jobTitle: String,
        subtitle: String,
        questions: List<Question>,
        notice: String? = null,
        probabilityLabel: (Question) -> String,
    ) {
        val rows = questions.groupBy { it.unit }.toSortedMap().entries.joinToString("\n") { (unit, qs) ->
            "<h2>Unit $unit</h2>" + qs.joinToString("\n") { q ->
                "<div class=\"q\"><p class=\"meta\">${q.marks} marks · ${escape(probabilityLabel(q))}</p>" +
                    "<p class=\"question\">${escape(q.question)}</p>" +
                    "<p class=\"answer\">${escape(q.answer)}</p></div>"
            }
        }
        print(context, jobTitle, page(jobTitle, subtitle, rows, notice), null)
    }

    private fun print(context: Context, jobTitle: String, html: String, baseUrl: String?) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(
                    jobTitle,
                    view.createPrintDocumentAdapter(jobTitle),
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build(),
                )
            }
        }
        keepAlive = webView
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
    }

    private fun page(title: String, subtitle: String, body: String, notice: String?) = """
        <!doctype html><html><head><meta charset="utf-8"><title>${escape(title)}</title>
        <style>
          body { font-family: serif; font-size: 11pt; line-height: 1.5; color: #1b1b21; margin: 0 6mm; }
          header { border-bottom: 2px solid #3949ab; margin-bottom: 12px; padding-bottom: 6px; }
          header h1 { font-family: sans-serif; font-size: 18pt; margin: 0; color: #3949ab; }
          header p { margin: 2px 0 0; font-size: 9pt; color: #555; font-family: sans-serif; }
          h1, h2, h3 { font-family: sans-serif; color: #1b1b21; page-break-after: avoid; }
          section + section { page-break-before: always; }
          table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 9.5pt; page-break-inside: avoid; }
          th, td { border: 1px solid #c7c5d0; padding: 4px 6px; text-align: left; vertical-align: top; }
          th { background: #eeeef6; }
          pre { font-family: monospace; font-size: 8.5pt; background: #f5f5f8; padding: 8px; white-space: pre; overflow: hidden; page-break-inside: avoid; }
          code { font-family: monospace; }
          blockquote { border-left: 3px solid #c7c5d0; margin: 8px 0; padding: 2px 10px; color: #444; }
          img { max-width: 100%; }
          .q { page-break-inside: avoid; margin-bottom: 10px; }
          .q .meta { font-size: 8.5pt; color: #666; margin: 0; font-family: sans-serif; }
          .q .question { font-weight: bold; margin: 2px 0; }
          .q .answer { margin: 0; }
          .notice { font-family: sans-serif; font-size: 9pt; background: #fbe9e7; border: 1px solid #e0b4ad; padding: 6px 10px; margin: 0 0 12px; }
          footer { margin-top: 16px; font-size: 8pt; color: #777; font-family: sans-serif; }
        </style></head><body>
        <header><h1>${escape(title)}</h1><p>${escape(subtitle)}</p></header>
        ${notice?.let { "<p class=\"notice\">${escape(it)}</p>" }.orEmpty()}
        $body
        <footer>Printed from StudyDesk, an independent study app not affiliated with any college or exam board.</footer>
        </body></html>
    """.trimIndent()

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
