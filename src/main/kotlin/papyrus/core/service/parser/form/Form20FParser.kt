package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * 20-F 외국 기업 연간 보고서 전문 파서
 *
 * 20-F는 외국 기업의 연간 보고서로, 10-K와 유사하지만:
 * - IFRS 또는 US GAAP 재무제표
 * - 외국 기업 특화 정보
 */
class Form20FParser : BaseSecReportParser<Form20FParseResult>(SecReportType.FORM_20F) {

        override fun parseHtml(
                htmlContent: String,
                metadata: SecReportMetadata
        ): Form20FParseResult {
                val cleanedContent = cleanHtml(htmlContent)
                return parseContent(cleanedContent, htmlContent, metadata)
        }

        override fun parseText(
                textContent: String,
                metadata: SecReportMetadata
        ): Form20FParseResult {
                return parseContent(textContent, textContent, metadata)
        }

        private fun parseContent(
                cleanedContent: String,
                rawContent: String,
                metadata: SecReportMetadata
        ): Form20FParseResult {
                val sections = extractSections(cleanedContent)

                return Form20FParseResult(
                        metadata = metadata,
                        rawContent = rawContent,
                        sections = sections,
                        businessDescription = sections["Item 4"] ?: sections["Business Overview"],
                        riskFactors = extractRiskFactors(cleanedContent),
                        financialStatements = extractFinancialStatements(cleanedContent),
                        mdAndA = extractMdAndA(cleanedContent),
                        corporateGovernance = sections["Corporate Governance"],
                        countryOfIncorporation = extractCountry(cleanedContent),
                        accountingStandard = extractAccountingStandard(cleanedContent)
                )
        }

        override fun extractSections(content: String): Map<String, String> {
                val sections = mutableMapOf<String, String>()

                // 20-F Item 패턴들 (10-K와 유사하지만 번호가 다름)
                val sectionPatterns =
                        listOf(
                                "Item 3" to
                                        Regex(
                                                "(?i)item\\s+3[.:\\-\\s]+(key\\s+information)",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Item 4" to
                                        Regex(
                                                "(?i)item\\s+4[.:\\-\\s]+(information\\s+on\\s+the\\s+company)",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Item 5" to
                                        Regex(
                                                "(?i)item\\s+5[.:\\-\\s]+(operating\\s+and\\s+financial)",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Item 8" to
                                        Regex(
                                                "(?i)item\\s+8[.:\\-\\s]+(financial\\s+information)",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Item 11" to
                                        Regex(
                                                "(?i)item\\s+11[.:\\-\\s]+(quantitative.*?market\\s+risk)",
                                                RegexOption.IGNORE_CASE
                                        )
                        )

                val headerMatches =
                        sectionPatterns
                                .mapNotNull { (sectionName, pattern) ->
                                        pattern.find(content)?.let {
                                                Triple(it.range.first, sectionName, it.value)
                                        }
                                }
                                .sortedBy { it.first }

                for (i in headerMatches.indices) {
                        val (startIndex, sectionName, _) = headerMatches[i]
                        val endIndex =
                                if (i < headerMatches.size - 1) {
                                        headerMatches[i + 1].first
                                } else {
                                        null
                                }

                        val sectionContent = extractSection(content, startIndex, endIndex)
                        sections[sectionName] = sectionContent
                }

                return sections
        }

        private fun extractMdAndA(content: String): ManagementDiscussion? {
                val mdaSection = extractSections(content)["Item 5"]
                if (mdaSection.isNullOrBlank()) return null

                return ManagementDiscussion(
                        keyBusinessDrivers = listOf(mdaSection.take(1000)),
                        marketConditions = "Market conditions extracted from 20-F",
                        futureOutlook = "Future outlook extracted from 20-F",
                        criticalAccountingPolicies = emptyList()
                )
        }

        private fun extractCountry(content: String): String? {
                val countryPattern =
                        Regex(
                                "(?i)incorporated\\s+(?:in|under\\s+the\\s+laws\\s+of)\\s+([A-Za-z\\s]+)",
                                RegexOption.IGNORE_CASE
                        )

                val match = countryPattern.find(content)
                return match?.groupValues?.get(1)?.trim()
        }

        private fun extractAccountingStandard(content: String): String? {
                return when {
                        content.contains("IFRS", ignoreCase = true) -> "IFRS"
                        content.contains(
                                "International Financial Reporting Standards",
                                ignoreCase = true
                        ) -> "IFRS"
                        content.contains("US GAAP", ignoreCase = true) -> "US GAAP"
                        content.contains(
                                "Generally Accepted Accounting Principles",
                                ignoreCase = true
                        ) -> "US GAAP"
                        else -> null
                }
        }

        override fun extractFinancialStatements(content: String): StructuredFinancialData? {
                val statements = EnhancedFinancialParser.parseFinancialStatements(content)

                val incomeStatement =
                        statements.find { it.type == StatementType.INCOME_STATEMENT }?.let {
                                FinancialDataMapper.convertToStructuredIncome(it)
                        }

                val balanceSheet =
                        statements.find { it.type == StatementType.BALANCE_SHEET }?.let {
                                FinancialDataMapper.convertToStructuredBalance(it)
                        }

                val cashFlow =
                        statements.find { it.type == StatementType.CASH_FLOW_STATEMENT }?.let {
                                FinancialDataMapper.convertToStructuredCashFlow(it)
                        }

                return StructuredFinancialData(
                        companyName = null,
                        reportType = "20-F",
                        fiscalYear = "",
                        fiscalPeriod = "Annual",
                        incomeStatement = incomeStatement,
                        balanceSheet = balanceSheet,
                        cashFlowStatement = cashFlow,
                        dataQuality =
                                if (statements.isNotEmpty()) DataQuality.HIGH
                                else DataQuality.MEDIUM
                )
        }
}
