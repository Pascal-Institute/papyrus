package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.parser.BaseSecReportParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Form 4 Insider Trading Report Parser
 *
 * Parses Statement of Changes in Beneficial Ownership of Securities. This parser primarily handles
 * the HTML version of Form 4.
 */
class Form4Parser : BaseSecReportParser<Form4ParseResult>(SecReportType.FORM_4) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form4ParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        val doc = Jsoup.parse(htmlContent)

        return Form4ParseResult(
                metadata = metadata,
                rawContent = htmlContent,
                sections = extractSections(cleanedContent),
                issuerName = extractIssuerName(doc),
                issuerTicker = extractIssuerTicker(doc),
                reportingOwner = extractReportingOwner(doc),
                nonDerivativeTransactions = extractNonDerivativeTransactions(doc),
                derivativeTransactions = extractDerivativeTransactions(doc),
                signatures = extractSignatures(doc)
        )
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form4ParseResult {
        // Text parsing is limited for Form 4 due to complex table structures
        return Form4ParseResult(
                metadata = metadata,
                rawContent = textContent,
                sections = extractSections(textContent),
                issuerName = null,
                issuerTicker = null,
                reportingOwner = null,
                nonDerivativeTransactions = emptyList(),
                derivativeTransactions = emptyList(),
                signatures = emptyList()
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        // Form 4 typically doesn't have standard "Item" sections like 10-K
        // But we can extract generic sections if present
        return mapOf("Full Content" to content)
    }

    // --- Extraction Helpers ---

    private fun extractIssuerName(doc: Document): String? {
        // Try to find issuer name in header or standard fields
        return doc.select("span:containsOwn(Issuer Name), td:containsOwn(Issuer Name)")
                .first()
                ?.nextElementSibling()
                ?.text()
                ?: doc.select(".FormData:contains(Issuer Name)").first()?.text()
                        ?: "Unknown Issuer" // Placeholder logic
    }

    private fun extractIssuerTicker(doc: Document): String? {
        return doc.select(
                        "span:containsOwn(Ticker or Trading Symbol), td:containsOwn(Ticker or Trading Symbol)"
                )
                .first()
                ?.nextElementSibling()
                ?.text()
    }

    private fun extractReportingOwner(doc: Document): ReportingOwner? {
        val name =
                doc.select(
                                "span:containsOwn(Name of Reporting Person), td:containsOwn(Name of Reporting Person)"
                        )
                        .first()
                        ?.nextElementSibling()
                        ?.text()

        val address =
                doc.select("span:containsOwn(Address of Reporting Person)")
                        .first()
                        ?.nextElementSibling()
                        ?.text()

        return ReportingOwner(
                cik = null, // Difficult to extract from HTML reliable without regex on CIK field
                name = name,
                address = address,
                relationship = extractRelationship(doc)
        )
    }

    private fun extractRelationship(doc: Document): OwnerRelationship {
        val relText = doc.select("span:containsOwn(Relationship of Reporting Person)").text()
        return OwnerRelationship(
                isDirector = relText.contains("Director", ignoreCase = true),
                isOfficer = relText.contains("Officer", ignoreCase = true),
                isTenPercentOwner = relText.contains("10% Owner", ignoreCase = true),
                isOther = relText.contains("Other", ignoreCase = true)
        )
    }

    private fun extractNonDerivativeTransactions(doc: Document): List<InsiderTransaction> {
        val transactions = mutableListOf<InsiderTransaction>()

        // Find Table I - Non-Derivative Securities
        val table1 = doc.select("table:contains(Table I)").first() ?: return emptyList()

        // This is a naive implementation; robust parsing requires handling rowspans and complex
        // headers
        val rows = table1.select("tr")

        for (row in rows.drop(1)) { // Skip header
            val cells = row.select("td")
            if (cells.size >= 8) {
                // Try to map columns (layout varies)
                // Assuming standard layout: Title, Date, Code, V, Amount, A/D, Price, Owned, Form,
                // Nature
                try {
                    val title = cells[0].text()
                    val date = cells[1].text()
                    val code = cells[2].text()
                    val amount = cells[3].text() // Adjust index based on actual layout inspection

                    if (title.isNotBlank() && date.isNotBlank()) {
                        transactions.add(
                                InsiderTransaction(
                                        titleOfSecurity = title,
                                        transactionDate = date,
                                        transactionCode = code,
                                        isAcquisition =
                                                cells.text().contains("A"), // Simplified check
                                        amount = amount,
                                        pricePerShare = "0.0", // Placeholder
                                        sharesOwnedFollowing = "0", // Placeholder
                                        ownershipForm = "D",
                                        natureOfIndirectOwnership = null
                                )
                        )
                    }
                } catch (e: Exception) {
                    // Skip malformed rows
                }
            }
        }

        return transactions
    }

    private fun extractDerivativeTransactions(doc: Document): List<InsiderTransaction> {
        // Table II parsing logic (similar to Table I)
        return emptyList()
    }

    private fun extractSignatures(doc: Document): List<String> {
        val signatures = mutableListOf<String>()
        doc.select("span:contains(Signature)").forEach { signatures.add(it.text()) }
        return signatures
    }
}
