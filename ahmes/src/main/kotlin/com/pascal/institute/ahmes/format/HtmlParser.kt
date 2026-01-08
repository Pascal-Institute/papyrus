package com.pascal.institute.ahmes.format

import com.pascal.institute.ahmes.parser.*
import com.pascal.institute.ahmes.model.FinancialMetric
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * HTML Document Parser
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

        // Detect XBRL
        val hasXbrl = detectXbrl(document)

        // Clean HTML content
        val cleanedContent = cleanHtml(document, financialTables)

        // Extract inline XBRL facts (high confidence, structured)
        val xbrlMetrics = InlineXbrlExtractor.extractMetrics(document)

        // Extract financial metrics from cleaned content
        val parsedMetrics = EnhancedFinancialParser.parsePdfTextTable(cleanedContent)

        // Merge: prefer iXBRL for the same metric name (more precise)
        val mergedExtended = mergeByNamePreferFirst(xbrlMetrics, parsedMetrics)
        val metrics = mergedExtended.map { it.toFinancialMetric() }

        return ParseResult(
            metrics = metrics,
            documentName = documentName,
            parserType = "HTML (Jsoup)",
            rawContent = content,
            cleanedContent = cleanedContent,
            metadata = mapOf(
                "hasXbrl" to hasXbrl.toString(),
                "tableCount" to financialTables.size.toString(),
                "xbrlMetricCount" to xbrlMetrics.size.toString(),
                "encoding" to detectEncoding(document),
                "hasFinancialTables" to (financialTables.isNotEmpty()).toString(),
                "originalSize" to "${content.length} chars",
                "cleanedSize" to "${cleanedContent.length} chars"
            )
        )
    }

    override fun canParse(content: String): Boolean {
        return content.trim().startsWith("<", ignoreCase = true) ||
            content.contains("<!DOCTYPE", ignoreCase = true) ||
            content.contains("<html", ignoreCase = true)
    }

    override fun getSupportedExtension(): String = "html"

    private fun extractFinancialTables(doc: Document): List<Element> {
        val tables = doc.select("table")

        val financialKeywords = listOf(
            "revenue", "income", "expense", "asset", "liability", "equity",
            "cash", "operating", "investing", "financing", "balance",
            "consolidated", "statement", "fiscal", "quarter", "earnings"
        )

        return tables.filter { table ->
            val tableText = table.text().lowercase()
            financialKeywords.any { keyword -> tableText.contains(keyword) }
        }
    }

    private fun cleanHtml(doc: Document, financialTables: List<Element>): String {
        val cleaned = doc.clone()

        cleaned.select("script, style, noscript, iframe").remove()
        cleaned.select("header, footer, nav").remove()
        cleaned.select("[style*=display:none], [style*=visibility:hidden]").remove()
        cleaned.select("SEC-HEADER, IMS-HEADER").remove()

        cleaned.select("*").forEach { element ->
            if (element.tagName().contains(":")) {
                element.tagName("span")
            }
        }

        val textBuilder = StringBuilder()

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

        textBuilder.append(cleaned.text())

        return SecTextNormalization.normalizeWhitespace(textBuilder.toString())
    }

    private fun detectXbrl(doc: Document): Boolean {
        return doc.selectFirst("ix\\:header, ix\\:nonfraction, ix\\:nonnumeric") != null ||
            doc.outerHtml().contains("xmlns:ix=", ignoreCase = true)
    }

    private fun detectEncoding(doc: Document): String {
        val meta = doc.selectFirst("meta[charset]")
        if (meta != null) {
            return meta.attr("charset")
        }

        val contentType = doc.selectFirst("meta[http-equiv=Content-Type]")
        if (contentType != null) {
            val charset = contentType.attr("content")
            val match = Regex("charset=([^;\\s]+)").find(charset)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return "utf-8"
    }

    private fun mergeByNamePreferFirst(
        first: List<com.pascal.institute.ahmes.model.ExtendedFinancialMetric>,
        second: List<com.pascal.institute.ahmes.model.ExtendedFinancialMetric>
    ): List<com.pascal.institute.ahmes.model.ExtendedFinancialMetric> {
        val byName = mutableMapOf<String, com.pascal.institute.ahmes.model.ExtendedFinancialMetric>()
        first.forEach { byName[it.name.lowercase()] = it }
        second.forEach { m ->
            val key = m.name.lowercase()
            if (!byName.containsKey(key)) {
                byName[key] = m
            }
        }
        return byName.values.toList()
    }
}
