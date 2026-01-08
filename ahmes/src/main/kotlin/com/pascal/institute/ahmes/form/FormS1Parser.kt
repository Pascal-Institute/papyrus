package com.pascal.institute.ahmes.form

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.parser.*

/**
 * S-1 IPO Registration Statement Parser
 *
 * S-1 is the IPO registration statement containing:
 * - Prospectus
 * - Business overview and strategy
 * - Risk factors
 * - Use of proceeds
 * - Financial statements (3-5 years)
 * - Management and shareholder information
 */
class FormS1Parser : BaseSecReportParser<FormS1ParseResult>(SecReportType.FORM_S1) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): FormS1ParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return parseContent(cleanedContent, htmlContent, metadata)
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): FormS1ParseResult {
        return parseContent(textContent, textContent, metadata)
    }

    private fun parseContent(
        cleanedContent: String,
        rawContent: String,
        metadata: SecReportMetadata
    ): FormS1ParseResult {
        val sections = extractSections(cleanedContent)

        return FormS1ParseResult(
            metadata = metadata,
            rawContent = rawContent,
            sections = sections,
            prospectus = extractProspectus(cleanedContent),
            businessDescription = sections["Business"] ?: sections["Our Business"],
            riskFactors = extractRiskFactors(cleanedContent),
            useOfProceeds = sections["Use of Proceeds"],
            dilution = sections["Dilution"],
            financialStatements = extractFinancialStatements(cleanedContent),
            mdAndA = extractMdAndA(cleanedContent),
            underwriting = sections["Underwriting"],
            offeringPrice = extractOfferingPrice(cleanedContent),
            sharesOffered = extractSharesOffered(cleanedContent)
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        val sectionPatterns = listOf(
            "Prospectus Summary" to Regex("(?i)prospectus\\s+summary", RegexOption.IGNORE_CASE),
            "Risk Factors" to Regex("(?i)risk\\s+factors", RegexOption.IGNORE_CASE),
            "Use of Proceeds" to Regex("(?i)use\\s+of\\s+proceeds", RegexOption.IGNORE_CASE),
            "Dividend Policy" to Regex("(?i)dividend\\s+policy", RegexOption.IGNORE_CASE),
            "Dilution" to Regex("(?i)dilution", RegexOption.IGNORE_CASE),
            "Capitalization" to Regex("(?i)capitalization", RegexOption.IGNORE_CASE),
            "Business" to Regex("(?i)(our\\s+)?business", RegexOption.IGNORE_CASE),
            "Management" to Regex("(?i)management'?s?\\s+discussion", RegexOption.IGNORE_CASE),
            "Executive Compensation" to Regex("(?i)executive\\s+compensation", RegexOption.IGNORE_CASE),
            "Directors and Officers" to Regex("(?i)directors\\s+(and|,)\\s+executive\\s+officers", RegexOption.IGNORE_CASE),
            "Principal Stockholders" to Regex("(?i)principal\\s+(stockholders|shareholders)", RegexOption.IGNORE_CASE),
            "Underwriting" to Regex("(?i)underwriting", RegexOption.IGNORE_CASE),
            "Legal Matters" to Regex("(?i)legal\\s+matters", RegexOption.IGNORE_CASE)
        )

        return extractNamedSections(content, sectionPatterns)
    }

    private fun extractProspectus(content: String): String? {
        val prospectusSection = extractSections(content)["Prospectus Summary"]
        return prospectusSection?.take(3000)
    }

    private fun extractMdAndA(content: String): ManagementDiscussion? {
        val mdaSection = extractSections(content)["Management"] ?: return null

        return ManagementDiscussion(
            keyBusinessDrivers = listOf(mdaSection.take(1000)),
            marketConditions = "Market conditions extracted from S-1",
            futureOutlook = "Future outlook extracted from S-1",
            criticalAccountingPolicies = emptyList()
        )
    }

    private fun extractOfferingPrice(content: String): String? {
        val pricePattern = Regex(
            "(?i)\\$([0-9,]+\\.\\d{2})\\s+to\\s+\\$([0-9,]+\\.\\d{2})\\s+per\\s+share",
            RegexOption.IGNORE_CASE
        )

        val match = pricePattern.find(content)
        return match?.value
    }

    private fun extractSharesOffered(content: String): String? {
        val sharesPattern = Regex(
            "(?i)([0-9,]+)\\s+shares\\s+of\\s+common\\s+stock",
            RegexOption.IGNORE_CASE
        )

        val match = sharesPattern.find(content)
        return match?.groupValues?.get(1)
    }

    override fun extractFinancialStatements(content: String): StructuredFinancialData? {
        val statements = EnhancedFinancialParser.parseFinancialStatements(content)

        val incomeStatement = statements.find { it.type == StatementType.INCOME_STATEMENT }?.let {
            FinancialDataMapper.convertToStructuredIncome(it)
        }

        val balanceSheet = statements.find { it.type == StatementType.BALANCE_SHEET }?.let {
            FinancialDataMapper.convertToStructuredBalance(it)
        }

        val cashFlow = statements.find { it.type == StatementType.CASH_FLOW_STATEMENT }?.let {
            FinancialDataMapper.convertToStructuredCashFlow(it)
        }

        return StructuredFinancialData(
            companyName = null,
            reportType = "S-1",
            fiscalYear = "Multi-year",
            fiscalPeriod = "Historical",
            incomeStatement = incomeStatement,
            balanceSheet = balanceSheet,
            cashFlowStatement = cashFlow,
            dataQuality = DataQuality.HIGH
        )
    }
}
