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

        println("🔍 Form4Parser: Starting transaction extraction")

        // Try multiple strategies to find transactions

        // Strategy 1: Look for Table I in traditional HTML format
        val table1 = doc.select("table:contains(Table I)").first()
        if (table1 != null) {
            println("📊 Found Table I - parsing rows")
            val rows = table1.select("tr")
            println("   Total rows in table: ${rows.size}")

            var rowIndex = 0
            for (row in rows.drop(1)) { // Skip header
                rowIndex++
                val cells = row.select("td")
                println("   Row $rowIndex: ${cells.size} cells")

                if (cells.size >= 4) {
                    try {
                        val title = cells[0].text().trim()
                        if (title.isNotBlank() && title != "Title of Security") {
                            val date = if (cells.size > 1) cells[1].text().trim() else ""
                            val code = if (cells.size > 2) cells[2].text().trim() else ""
                            val amount = if (cells.size > 4) cells[4].text().trim() else ""
                            val pricePerShare = if (cells.size > 6) cells[6].text().trim().ifEmpty { null } else null
                            val sharesOwned = if (cells.size > 7) cells[7].text().trim() else ""
                            val ownershipForm = if (cells.size > 8) cells[8].text().trim().firstOrNull()?.toString() ?: "D" else "D"

                            // Extract nature of indirect ownership (handles "By ..." patterns)
                            val nature = if (cells.size > 9) {
                                val text = cells[9].text().trim()
                                if (text.isNotEmpty()) text else null
                            } else null

                            // Determine if acquisition or disposition
                            val isAcquisition = if (cells.size > 5) {
                                cells[5].text().trim().equals("A", ignoreCase = true)
                            } else {
                                code.contains("P", ignoreCase = true) || code.contains("A", ignoreCase = true)
                            }

                            println("   ✅ Adding transaction: $title | Date: $date | Amount: $amount | Ownership: $ownershipForm")

                            transactions.add(
                                InsiderTransaction(
                                    titleOfSecurity = title,
                                    transactionDate = date,
                                    transactionCode = code,
                                    isAcquisition = isAcquisition,
                                    amount = amount,
                                    pricePerShare = pricePerShare,
                                    sharesOwnedFollowing = sharesOwned,
                                    ownershipForm = ownershipForm,
                                    natureOfIndirectOwnership = nature
                                )
                            )
                        }
                    } catch (e: Exception) {
                        println("   ⚠️ Error parsing row $rowIndex: ${e.message}")
                    }
                }
            }
        }

        // Strategy 2: Parse XML-style Form 4 (when viewing raw XML)
        // Look for nonDerivativeTransaction or nonDerivativeHolding elements
        val xmlTransactions = doc.select("nonDerivativeTransaction, nonDerivativeHolding")
        if (xmlTransactions.isNotEmpty()) {
            println("🔖 Found ${xmlTransactions.size} XML transactions")
        }

        for (txn in xmlTransactions) {
            try {
                val title = txn.select("securityTitle value").text().trim()
                val date = txn.select("transactionDate value").text().trim()
                val code = txn.select("transactionCode").text().trim()
                val amount = txn.select("transactionShares value, posttransactionAmounts sharesOwnedFollowingTransaction value").text().trim()
                val pricePerShare = txn.select("transactionPricePerShare value").text().trim()
                val sharesOwned = txn.select("sharesOwnedFollowingTransaction value").text().trim()
                val ownershipForm = txn.select("directOrIndirectOwnership value").text().trim().ifEmpty { "D" }
                val nature = txn.select("natureOfOwnership value").text().trim().ifEmpty { null }

                val isAcquisition = txn.select("transactionAcquiredDisposedCode value").text().trim().equals("A", ignoreCase = true)

                if (title.isNotBlank()) {
                    println("   ✅ Adding XML transaction: $title")
                    transactions.add(
                        InsiderTransaction(
                            titleOfSecurity = title,
                            transactionDate = date,
                            transactionCode = code,
                            isAcquisition = isAcquisition,
                            amount = amount,
                            pricePerShare = pricePerShare.ifEmpty { null },
                            sharesOwnedFollowing = sharesOwned,
                            ownershipForm = ownershipForm,
                            natureOfIndirectOwnership = nature
                        )
                    )
                }
            } catch (e: Exception) {
                println("   ⚠️ Error parsing XML transaction: ${e.message}")
            }
        }

        // Strategy 3: Look for all table rows that contain Common Stock or other securities
        if (transactions.isEmpty()) {
            println("🔎 Fallback: Searching all table rows for securities")
            val allRows = doc.select("tr")
            for (row in allRows) {
                val cells = row.select("td")
                if (cells.size >= 4) {
                    val firstCell = cells[0].text().trim()
                    // Check if this looks like a security title
                    if (firstCell.contains("Common Stock", ignoreCase = true) ||
                        firstCell.contains("Preferred Stock", ignoreCase = true) ||
                        firstCell.contains("Option", ignoreCase = true)) {

                        try {
                            val title = firstCell
                            val date = if (cells.size > 1) cells[1].text().trim() else ""
                            val code = if (cells.size > 2) cells[2].text().trim() else ""
                            val amount = if (cells.size > 4) cells[4].text().trim() else cells.getOrNull(3)?.text()?.trim() ?: ""
                            val pricePerShare = if (cells.size > 6) cells[6].text().trim().ifEmpty { null } else null
                            val sharesOwned = if (cells.size > 7) cells[7].text().trim() else ""
                            val ownershipForm = if (cells.size > 8) cells[8].text().trim().firstOrNull()?.toString() ?: "D" else "D"
                            val nature = if (cells.size > 9) cells[9].text().trim().ifEmpty { null } else null

                            val isAcquisition = if (cells.size > 5) {
                                cells[5].text().trim().equals("A", ignoreCase = true)
                            } else {
                                code.contains("A", ignoreCase = true)
                            }

                            println("   ✅ Adding fallback transaction: $title")

                            transactions.add(
                                InsiderTransaction(
                                    titleOfSecurity = title,
                                    transactionDate = date,
                                    transactionCode = code,
                                    isAcquisition = isAcquisition,
                                    amount = amount,
                                    pricePerShare = pricePerShare,
                                    sharesOwnedFollowing = sharesOwned,
                                    ownershipForm = ownershipForm,
                                    natureOfIndirectOwnership = nature
                                )
                            )
                        } catch (e: Exception) {
                            println("   ⚠️ Error parsing fallback row: ${e.message}")
                        }
                    }
                }
            }
        }

        println("📝 Total transactions extracted: ${transactions.size}")
        return transactions.distinctBy { "${it.titleOfSecurity}_${it.transactionDate}_${it.amount}_${it.ownershipForm}_${it.natureOfIndirectOwnership}" }
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
