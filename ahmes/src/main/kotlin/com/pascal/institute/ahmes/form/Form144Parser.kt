package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.parser.BaseSecReportParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Form 144 - Notice of Proposed Sale of Securities Parser
 *
 * Parses notices filed by insiders and affiliates who intend to sell restricted or control securities.
 * Form 144 serves as an intent notice rather than an actual transaction report.
 */
class Form144Parser : BaseSecReportParser<Form144ParseResult>(SecReportType.FORM_144) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form144ParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        val doc = Jsoup.parse(htmlContent)

        println("🔍 Form144Parser: Parsing document...")
        println("   Content length: ${htmlContent.length}")

        val plainText = doc.text()
        println("   Contains 'Issuer Information': ${plainText.contains("Issuer Information", ignoreCase = true)}")
        println("   Contains 'Person for Whose Account': ${plainText.contains("Person for Whose Account", ignoreCase = true)}")
        println("   Contains 'Securities Information': ${plainText.contains("Securities Information", ignoreCase = true)}")

        val issuerName = extractIssuerName(doc, plainText)
        val personSelling = extractPersonSelling(doc, plainText)
        val proposedSaleInfo = extractProposedSaleInfo(doc, plainText)
        val remarks = extractRemarks(doc)

        println("   ✓ Issuer Name: $issuerName")
        println("   ✓ Person Selling: ${personSelling?.name} (${personSelling?.relationship})")
        println("   ✓ Shares: ${proposedSaleInfo?.numberOfShares}")
        println("   ✓ Market Value: ${proposedSaleInfo?.aggregateMarketValue}")
        println("   ✓ Broker: ${proposedSaleInfo?.brokerName}")

        return Form144ParseResult(
                metadata = metadata,
                rawContent = htmlContent,
                sections = extractSections(cleanedContent),
                issuerName = issuerName,
                issuerTicker = null,
                personSelling = personSelling,
                proposedSaleInfo = proposedSaleInfo,
                remarks = remarks
        )
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form144ParseResult {
        return Form144ParseResult(
                metadata = metadata,
                rawContent = textContent,
                sections = extractSections(textContent),
                issuerName = null,
                issuerTicker = null,
                personSelling = null,
                proposedSaleInfo = null,
                remarks = null
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        return mapOf("Form Content" to content.take(5000))
    }

    private fun extractIssuerName(doc: Document, fullText: String): String? {
        // Regex pattern search
        Regex("Name of Issuer\\s+([A-Z][A-Z0-9 /&.,'-]+?)(?=\\s+SEC File Number|\\s+Address|\\s+Phone|$)", RegexOption.IGNORE_CASE)
                .find(fullText)?.groupValues?.getOrNull(1)?.trim()?.let { return it }

        // XML format
        doc.selectFirst("issuerName")?.text()?.takeIf { it.isNotBlank() }?.let { return it.trim() }

        // HTML table search
        doc.getElementsContainingOwnText("Issuer Information").firstOrNull()?.parent()?.text()?.let { sectionText ->
            Regex("(?:Name of Issuer|Issuer)[:\\s]+([A-Z][A-Z0-9 /&.,'-]+?)(?=\\s+SEC|\\s+Address|\\s+Phone|$)")
                    .find(sectionText)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
        }

        val selectors = listOf("td:contains(Name of Issuer)", "*:matchesOwn(Name of Issuer)", "th:contains(Issuer)")
        for (selector in selectors) {
            doc.selectFirst(selector)?.let { element ->
                element.nextElementSibling()?.text()?.takeIf { it.isNotBlank() && it.length > 2 }?.let { return it.trim() }
                element.parent()?.nextElementSibling()?.text()?.takeIf { it.isNotBlank() && it.length > 2 }?.let { return it.trim() }
                element.parent()?.select("td")?.getOrNull(1)?.text()?.takeIf { it.isNotBlank() }?.let { return it.trim() }
            }
        }

        return null
    }

    private fun extractPersonSelling(doc: Document, fullText: String): SellerInfo? {
        var sellerName: String? = null
        var sellerRelationship: String? = null

        // Pattern search
        Regex("Name of Person for Whose Account.*?(?:To Be Sold|are to be sold)\\s+([A-Z][A-Z ]+[A-Z])\\s+",
              setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(fullText)?.groupValues?.getOrNull(1)?.trim()?.let { sellerName = it }

        // XML format
        if (sellerName == null) {
            doc.selectFirst("personName, sellerName")?.text()?.takeIf { it.isNotBlank() }?.let { sellerName = it.trim() }
        }

        // HTML search
        if (sellerName == null) {
            doc.getElementsContainingOwnText("Person for Whose Account").firstOrNull()?.parent()?.text()?.let { text ->
                Regex("(?:To Be Sold|are to be sold)\\s+([A-Z][A-Z ]+[A-Z])\\s+(?:See|Relationship)", RegexOption.IGNORE_CASE)
                        .find(text)?.groupValues?.getOrNull(1)?.trim()?.let { sellerName = it }
            }
        }

        // Extract relationship
        Regex("Relationship to Issuer\\s+(Director|Officer|10% Owner|Other)", RegexOption.IGNORE_CASE)
                .find(fullText)?.groupValues?.getOrNull(1)?.let { sellerRelationship = it.trim() }

        if (sellerRelationship == null) {
            doc.selectFirst("relationship, relationshipToIssuer")?.text()?.takeIf { it.isNotBlank() }?.let { sellerRelationship = it.trim() }
        }

        if (sellerRelationship == null) {
            when {
                fullText.contains("Director", ignoreCase = true) && !fullText.contains("DirectorY", ignoreCase = true) -> sellerRelationship = "Director"
                fullText.contains("Officer", ignoreCase = true) -> sellerRelationship = "Officer"
                fullText.contains("10%") -> sellerRelationship = "10% Owner"
            }
        }

        return if (sellerName != null) {
            SellerInfo(name = sellerName, cik = null, relationship = sellerRelationship ?: "Unknown")
        } else {
            null
        }
    }

    private fun extractProposedSaleInfo(doc: Document, fullText: String): ProposedSaleInfo? {
        var securityType: String? = null
        var numberOfShares: String? = null
        var aggregateMarketValue: String? = null
        var proposedSaleDate: String? = null
        var brokerName: String? = null

        // XML format
        doc.selectFirst("securityTitle, titleOfClass")?.text()?.takeIf { it.isNotBlank() }?.let { securityType = it.trim() }
        doc.selectFirst("numberOfShares, sharesToBeSold")?.text()?.takeIf { it.isNotBlank() }?.let { numberOfShares = it.trim() }
        doc.selectFirst("aggregateMarketValue, marketValue")?.text()?.takeIf { it.isNotBlank() }?.let { aggregateMarketValue = it.trim() }
        doc.selectFirst("approximateDateOfSale, proposedSaleDate, saleDate")?.text()?.takeIf { it.isNotBlank() }?.let { proposedSaleDate = it.trim() }
        doc.selectFirst("brokerName, nameOfBroker")?.text()?.takeIf { it.isNotBlank() }?.let { brokerName = it.trim() }

        // Text pattern search
        if (securityType == null) {
            Regex("Title of the Class.*?To Be Sold\\s+(Common|Preferred|Class [AB])",
                  setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(fullText)?.groupValues?.getOrNull(1)?.let { securityType = it.trim() }
        }

        if (numberOfShares == null) {
            Regex("Number of Shares.*?To Be Sold\\s+(\\d{1,3}(?:,\\d{3})*)",
                  setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(fullText)?.groupValues?.getOrNull(1)?.let { numberOfShares = it }
        }

        if (aggregateMarketValue == null) {
            Regex("Aggregate Market Value\\s+(?:\\$)?([\\d,]+(?:\\.\\d{2})?)", RegexOption.IGNORE_CASE)
                    .find(fullText)?.groupValues?.getOrNull(1)?.let { aggregateMarketValue = it }
        }

        if (proposedSaleDate == null) {
            Regex("Approximate Date of Sale\\s+(\\d{1,2}/\\d{1,2}/\\d{4})", RegexOption.IGNORE_CASE)
                    .find(fullText)?.groupValues?.getOrNull(1)?.let { proposedSaleDate = it }
        }

        if (brokerName == null) {
            Regex("(?:Name and Address of the Broker|Broker)\\s+([A-Z][A-Za-z ]+(?:Lynch|Stanley|Sachs|Morgan|Fargo))",
                  setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(fullText)?.groupValues?.getOrNull(1)?.trim()?.split("\\s+".toRegex())?.take(3)?.joinToString(" ")?.let { brokerName = it }
        }

        return ProposedSaleInfo(
                securityType = securityType ?: "Common Stock",
                numberOfShares = numberOfShares ?: "N/A",
                aggregateMarketValue = aggregateMarketValue,
                proposedSaleDate = proposedSaleDate,
                brokerName = brokerName
        )
    }

    private fun extractRemarks(doc: Document): String? {
        val selectors = listOf("td:contains(Remarks)", "td:contains(REMARKS)", "div:contains(Additional Information)")
        for (selector in selectors) {
            doc.selectFirst(selector)?.let { element ->
                val text = element.parent()?.text() ?: element.text()
                if (text.isNotBlank() && text.length > 10) {
                    return text.trim()
                }
            }
        }
        return null
    }
}
