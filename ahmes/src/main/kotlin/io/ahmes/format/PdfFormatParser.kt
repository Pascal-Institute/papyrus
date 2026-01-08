package io.ahmes.format

import io.ahmes.parser.*
import io.ahmes.model.FinancialMetric
import org.jsoup.Jsoup

/**
 * PDF Document Parser (Format Version)
 *
 * Specialized parser for PDF formatted SEC filings.
 * Handles SEC viewer HTML responses for PDF documents.
 *
 * Note: For actual PDF binary parsing, use io.ahmes.util.PdfParser
 */
class PdfFormatParser : DocumentParser {

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
            metadata = mapOf(
                "isSecViewer" to isPdfViewer.toString(),
                "contentType" to (if (isPdfViewer) "SEC_VIEWER_HTML" else "UNKNOWN")
            )
        )
    }

    override fun canParse(content: String): Boolean {
        return content.startsWith("%PDF") ||
            content.contains("sec.gov/cgi-bin/viewer", ignoreCase = true)
    }

    override fun getSupportedExtension(): String = "pdf"

    private fun cleanHtmlFromSecViewer(html: String): String {
        val doc = Jsoup.parse(html)
        doc.select("script, style").remove()

        val text = SecTextNormalization.decodeBasicEntities(doc.text())
        return SecTextNormalization.normalizeWhitespace(text)
    }
}
