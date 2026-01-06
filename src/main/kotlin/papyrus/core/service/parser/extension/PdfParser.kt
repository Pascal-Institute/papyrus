package papyrus.core.service.parser

import org.jsoup.Jsoup
import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.FinancialMetric

/**
 * PDF Document Parser
 *
 * Specialized parser for PDF formatted SEC filings. Handles SEC viewer HTML responses for PDF
 * documents. Note: True PDF binary parsing requires PDFBox dependency.
 */
class PdfParser : DocumentParser {

        override fun parse(content: String, documentName: String): ParseResult {
                // For PDF content from SEC viewer, we typically get HTML representation
                val cleanedContent = cleanHtmlFromSecViewer(content)

                // Extract financial metrics using existing parser
                val extendedMetrics = EnhancedFinancialParser.parsePdfTextTable(cleanedContent)
                val metrics = extendedMetrics.map { it.toFinancialMetric() }

                val isPdfViewer = content.contains("sec.gov/cgi-bin/viewer", ignoreCase = true)

                return ParseResult(
                        metrics = metrics,
                        documentName = documentName,
                        parserType = "PDF",
                        rawContent = content,
                        cleanedContent = cleanedContent,
                        metadata =
                                mapOf(
                                        "isSecViewer" to isPdfViewer.toString(),
                                        "contentType" to
                                                (if (isPdfViewer) "SEC_VIEWER_HTML" else "UNKNOWN")
                                )
                )
        }

        override fun canParse(content: String): Boolean {
                // Check for PDF magic number or SEC viewer
                return content.startsWith("%PDF") ||
                        content.contains("sec.gov/cgi-bin/viewer", ignoreCase = true)
        }

        override fun getSupportedExtension(): String = "pdf"

        /** Clean HTML content from SEC PDF viewer SEC's PDF viewer returns HTML representation */
        private fun cleanHtmlFromSecViewer(html: String): String {
                val doc = Jsoup.parse(html)
                doc.select("script, style").remove()

                // Extract text content
                val text = decodeHtmlEntities(doc.text())
                return SecTextNormalization.normalizeWhitespace(text)
        }

        /** Decode HTML entities */
        private fun decodeHtmlEntities(text: String): String {
                return SecTextNormalization.decodeBasicEntities(text).replace("&apos;", "'")
        }
}
