package papyrus.core.service.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.FinancialMetric

/**
 * HTML Document Parser (Enhanced with Jsoup)
 *
 * Specialized parser for HTML and HTM formatted SEC filings. Uses Jsoup for robust HTML parsing,
 * table structure preservation, and XBRL data handling.
 */
class HtmlParser : DocumentParser {

        override fun parse(content: String, documentName: String): ParseResult {
                // Parse HTML with Jsoup
                val document = Jsoup.parse(content)

                // Extract financial tables for better parsing
                val financialTables = extractFinancialTables(document)

                // Clean HTML content
                val cleanedContent = cleanHtml(document, financialTables)

                // Extract financial metrics from cleaned content using existing parser
                val extendedMetrics = EnhancedFinancialParser.parsePdfTextTable(cleanedContent)

                // Convert ExtendedFinancialMetric to FinancialMetric
                val metrics = extendedMetrics.map { it.toFinancialMetric() }

                return ParseResult(
                        metrics = metrics,
                        documentName = documentName,
                        parserType = "HTML (Jsoup)",
                        rawContent = content,
                        cleanedContent = cleanedContent,
                        metadata =
                                mapOf(
                                        "hasXbrl" to detectXbrl(document).toString(),
                                        "tableCount" to financialTables.size.toString(),
                                        "encoding" to detectEncoding(document),
                                        "hasFinancialTables" to
                                                (financialTables.isNotEmpty()).toString()
                                )
                )
        }

        override fun canParse(content: String): Boolean {
                return content.trim().startsWith("<", ignoreCase = true) ||
                        content.contains("<!DOCTYPE", ignoreCase = true) ||
                        content.contains("<html", ignoreCase = true)
        }

        override fun getSupportedExtension(): String = "html"

        /**
         * Extract financial tables from document Looks for tables with financial keywords in
         * headers or content
         */
        private fun extractFinancialTables(doc: Document): List<Element> {
                val tables = doc.select("table")

                // Financial keywords to identify relevant tables
                val financialKeywords =
                        listOf(
                                "revenue",
                                "income",
                                "expense",
                                "asset",
                                "liability",
                                "equity",
                                "cash",
                                "operating",
                                "investing",
                                "financing",
                                "balance",
                                "consolidated",
                                "statement",
                                "fiscal",
                                "quarter",
                                "earnings"
                        )

                return tables.filter { table ->
                        val tableText = table.text().lowercase()
                        financialKeywords.any { keyword -> tableText.contains(keyword) }
                }
        }

        /**
         * Clean HTML content using Jsoup Removes unnecessary elements while preserving financial
         * data structure
         */
        private fun cleanHtml(doc: Document, financialTables: List<Element>): String {
                // Clone document to avoid modifying original
                val cleaned = doc.clone()

                // Remove irrelevant elements
                cleaned.select("script, style, noscript, iframe").remove()
                cleaned.select("header, footer, nav").remove()

                // Remove XBRL hidden elements (often have display:none)
                cleaned.select("[style*=display:none], [style*=visibility:hidden]").remove()

                // Remove common SEC metadata tags
                cleaned.select("SEC-HEADER, IMS-HEADER").remove()

                // Keep XBRL tags but remove the namespace prefix for cleaner text
                cleaned.select("*").forEach { element ->
                        if (element.tagName().contains(":")) {
                                element.tagName("span") // Convert XBRL tags to span
                        }
                }

                // Extract text with better table formatting
                val textBuilder = StringBuilder()

                // Add financial tables with preserved structure
                financialTables.forEach { table ->
                        textBuilder.append("\n=== FINANCIAL TABLE ===\n")
                        table.select("tr").forEach { row ->
                                val cells = row.select("td, th")
                                val rowText = cells.joinToString(" | ") { it.text().trim() }
                                if (rowText.isNotBlank()) {
                                        textBuilder.append(rowText).append("\n")
                                }
                        }
                        textBuilder.append("=== END TABLE ===\n\n")
                }

                // Add remaining text content
                textBuilder.append(cleaned.text())

                // Normalize whitespace
                return SecTextNormalization.normalizeWhitespace(textBuilder.toString())
        }

        /** Detect if document contains XBRL data using Jsoup */
        private fun detectXbrl(doc: Document): Boolean {
                // Check for XBRL namespace declarations
                val hasXmlns = doc.select("[xmlns*=xbrl]").isNotEmpty()

                // Check for XBRL-specific tags (they contain colons)
                val hasXbrlTags = doc.select("*").any { it.tagName().contains(":") }

                // Check for XBRL context or unit references
                val hasXbrlAttributes = doc.select("[contextRef], [unitRef]").isNotEmpty()

                return hasXmlns || hasXbrlTags || hasXbrlAttributes
        }

        /** Detect document encoding from meta tags */
        private fun detectEncoding(doc: Document): String {
                // Check meta charset tag
                val charset = doc.select("meta[charset]").attr("charset")
                if (charset.isNotBlank()) return charset

                // Check meta http-equiv content-type
                val contentType = doc.select("meta[http-equiv=Content-Type]").attr("content")
                if (contentType.isNotBlank()) {
                        val charsetMatch =
                                Regex("charset=([a-zA-Z0-9-]+)", RegexOption.IGNORE_CASE)
                                        .find(contentType)
                        charsetMatch?.groupValues?.get(1)?.let {
                                return it
                        }
                }

                return "UTF-8"
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
