package papyrus.core.service.parser

import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.FinancialMetric

/**
 * HTML Document Parser
 *
 * Specialized parser for HTML and HTM formatted SEC filings. Handles HTML tags, table structures,
 * and XBRL data.
 */
class HtmlParser : DocumentParser {

        override fun parse(content: String, documentName: String): ParseResult {
                // Clean HTML tags first
                val cleanedContent = cleanHtml(content)

                // Extract financial metrics from cleaned content using existing parser
                val extendedMetrics = EnhancedFinancialParser.parsePdfTextTable(cleanedContent)

                // Convert ExtendedFinancialMetric to FinancialMetric
                val metrics = extendedMetrics.map { it.toFinancialMetric() }

                return ParseResult(
                        metrics = metrics,
                        documentName = documentName,
                        parserType = "HTML",
                        rawContent = content,
                        cleanedContent = cleanedContent,
                        metadata =
                                mapOf(
                                        "hasXbrl" to detectXbrl(content).toString(),
                                        "hasTables" to detectTables(content).toString(),
                                        "encoding" to detectEncoding(content)
                                )
                )
        }

        override fun canParse(content: String): Boolean {
                return content.trim().startsWith("<", ignoreCase = true) ||
                        content.contains("<!DOCTYPE", ignoreCase = true) ||
                        content.contains("<html", ignoreCase = true)
        }

        override fun getSupportedExtension(): String = "html"

        /** Clean HTML content by removing tags and extracting text */
        private fun cleanHtml(html: String): String {
                var cleaned = html

                // Remove common SEC filing metadata
                cleaned =
                        cleaned.replace(
                                Regex(
                                        "<(SCRIPT|script)[^>]*>.*?</(SCRIPT|script)>",
                                        RegexOption.DOT_MATCHES_ALL
                                ),
                                ""
                        )
                cleaned =
                        cleaned.replace(
                                Regex(
                                        "<(STYLE|style)[^>]*>.*?</(STYLE|style)>",
                                        RegexOption.DOT_MATCHES_ALL
                                ),
                                ""
                        )
                cleaned =
                        cleaned.replace(
                                Regex(
                                        "<(HEAD|head)[^>]*>.*?</(HEAD|head)>",
                                        RegexOption.DOT_MATCHES_ALL
                                ),
                                ""
                        )

                // Remove XBRL-specific tags but keep content
                cleaned = cleaned.replace(Regex("</?[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+[^>]*>"), " ")

                // Remove all remaining HTML tags
                cleaned = cleaned.replace(Regex("<[^>]+>"), " ")

                // Decode HTML entities
                cleaned = decodeHtmlEntities(cleaned)

                // Normalize whitespace
                return SecTextNormalization.normalizeWhitespace(cleaned)
        }

        /** Detect if document contains XBRL data */
        private fun detectXbrl(content: String): Boolean {
                return content.contains("xbrl", ignoreCase = true) ||
                        content.contains("xmlns:", ignoreCase = true)
        }

        /** Detect if document contains HTML tables */
        private fun detectTables(content: String): Boolean {
                return content.contains("<table", ignoreCase = true)
        }

        /** Detect document encoding */
        private fun detectEncoding(content: String): String {
                val charsetMatch =
                        Regex("charset=[\"']?([a-zA-Z0-9-]+)", RegexOption.IGNORE_CASE)
                                .find(content)
                return charsetMatch?.groupValues?.get(1) ?: "UTF-8"
        }

        /** Decode common HTML entities */
        private fun decodeHtmlEntities(text: String): String {
                val basicDecoded = SecTextNormalization.decodeBasicEntities(text)

                return basicDecoded
                        .replace("&apos;", "'")
                        .replace("&#39;", "'")
                        .replace("&#160;", " ")
                        .replace(Regex("&#(\\d+);")) { matchResult ->
                                val code = matchResult.groupValues[1].toIntOrNull()
                                if (code != null && code in 32..126) {
                                        code.toChar().toString()
                                } else {
                                        " "
                                }
                        }
        }
}

/** Extension function to convert ExtendedFinancialMetric to FinancialMetric */
private fun ExtendedFinancialMetric.toFinancialMetric(): FinancialMetric {
        return FinancialMetric(
                name = this.name,
                value = this.value,
                rawValue = this.rawValue,
                context = this.context
        )
}
