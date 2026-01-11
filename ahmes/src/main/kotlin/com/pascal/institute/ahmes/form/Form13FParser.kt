package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.parser.BaseSecReportParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Form 13F Institutional Holdings Parser
 *
 * Parses Form 13F-HR reports to extract institutional holdings. Supports HTML Information Tables.
 */
class Form13FParser : BaseSecReportParser<Form13FParseResult>(SecReportType.FORM_13F) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form13FParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        val doc = Jsoup.parse(htmlContent)

        val reportInfo = extractReportInfo(doc)
        val holdings = extractHoldings(doc)
        val summary = calculateSummary(holdings)

        return Form13FParseResult(
                metadata = metadata,
                rawContent = htmlContent,
                sections = extractSections(cleanedContent),
                reportInfo = reportInfo,
                holdings = holdings,
                summary = summary
        )
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form13FParseResult {
        // Text parsing for 13F is complex due to column alignment
        return Form13FParseResult(
                metadata = metadata,
                rawContent = textContent,
                sections = extractSections(textContent),
                reportInfo = null,
                holdings = emptyList(),
                summary = null
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        return mapOf(
                "Cover Page" to (content.substringBefore("INFORMATION TABLE") ?: ""),
                "Information Table" to (content.substringAfter("INFORMATION TABLE") ?: "")
        )
    }

    // --- Extraction Helpers ---

    private fun extractReportInfo(doc: Document): Form13FReportInfo {
        // Basic extraction attempts using common labels
        val period =
                doc.select("span:containsOwn(Period of Report), td:containsOwn(Period of Report)")
                        .first()
                        ?.nextElementSibling()
                        ?.text()

        val manager =
                doc.select(
                                "span:containsOwn(Name of Institutional Investment Manager), td:containsOwn(Name of Institutional Investment Manager)"
                        )
                        .first()
                        ?.nextElementSibling()
                        ?.text()

        return Form13FReportInfo(
                periodOfReport = period,
                filingManagerName = manager,
                filingManagerAddress = null, // Parsing address is often messy
                signatureDate = null,
                reportType = "13F HOLDINGS REPORT"
        )
    }

    private fun extractHoldings(doc: Document): List<HoldingEntry> {
        val holdings = mutableListOf<HoldingEntry>()

        // Find the Information Table
        // Often identifiable by headers: "Name of Issuer", "Title of Class", "CUSIP", "Value"
        val table =
                doc.select("table").find { table ->
                    val text = table.text()
                    text.contains("Name of Issuer", ignoreCase = true) &&
                            text.contains("CUSIP", ignoreCase = true) &&
                            text.contains("Value", ignoreCase = true)
                }
                        ?: return emptyList()

        val rows = table.select("tr")

        // Skip headers (simple heuristic: first few rows usually)
        // Better approach: Find header row and map indexes
        var headerFound = false

        for (row in rows) {
            val cells = row.select("td")
            if (cells.isEmpty()) continue

            // Heuristic to skip header
            if (!headerFound) {
                if (cells.text().contains("Name of Issuer", ignoreCase = true)) {
                    headerFound = true
                }
                continue
            }

            // Skip rows that look like headers or empty
            if (cells.size < 5 || cells[0].text().isBlank()) continue

            try {
                // Assuming generic column order (often varies, but typical):
                // 1. Issuer Name, 2. Class, 3. CUSIP, 4. Value, 5. Shares, 6. SH/PRN, 7. Put/Call,
                // 8. Discretion, 9. Managers, 10. Voting(Sole), 11. Voting(Shared), 12.
                // Voting(None)
                // This index mapping is fragile and needs robust header mapping logic in production

                // For this MVP, we try to detect typical column positions
                // Or simplified list if we just grab non-empty texts

                val cols = cells.map { it.text().trim() }.filter { it.isNotEmpty() }
                if (cols.size >= 5) {
                    val issuer = cols[0]
                    val classTitle = cols.getOrNull(1)
                    val cusip = cols.getOrNull(2)

                    // Value is usually numeric
                    val valueStr = cols.firstOrNull { it.matches(Regex("[\\d,]+")) }
                    val value = valueStr?.replace(",", "")?.toLongOrNull()

                    // Shares often follows value
                    // This logic is extremely simplified for demonstration

                    holdings.add(
                            HoldingEntry(
                                    nameOfIssuer = issuer,
                                    titleOfClass = classTitle,
                                    cusip = cusip,
                                    value = value,
                                    sharesOrPrincipalAmount = null,
                                    sharesOrPrincipalType = "SH",
                                    investmentDiscretion = "OLE",
                                    votingAuthority = VotingAuthority()
                            )
                    )
                }
            } catch (e: Exception) {
                // Ignore malformed rows
            }
        }

        return holdings
    }

    private fun calculateSummary(holdings: List<HoldingEntry>): HoldingsSummary {
        val totalValue = holdings.mapNotNull { it.value }.sum()
        val topHoldings = holdings.sortedByDescending { it.value ?: 0 }.take(10)

        return HoldingsSummary(
                totalHoldingsCount = holdings.size,
                totalValue = totalValue,
                topHoldings = topHoldings
        )
    }
}
