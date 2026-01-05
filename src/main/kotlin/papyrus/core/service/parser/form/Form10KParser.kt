package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * 10-K 연간 보고서 전문 파서
 *
 * 10-K는 가장 포괄적인 연간 재무 보고서로, 다음을 포함합니다:
 * - Part I: Business, Risk Factors, Properties, Legal Proceedings
 * - Part II: Market Data, MD&A, Financial Statements
 * - Part III: Directors, Executive Compensation
 * - Part IV: Exhibits
 */
class Form10KParser : BaseSecReportParser<Form10KParseResult>(SecReportType.FORM_10K) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form10KParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return parseContent(cleanedContent, htmlContent, metadata)
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form10KParseResult {
        return parseContent(textContent, textContent, metadata)
    }

    private fun parseContent(
            cleanedContent: String,
            rawContent: String,
            metadata: SecReportMetadata
    ): Form10KParseResult {
        val sections = extractSections(cleanedContent)

        return Form10KParseResult(
                metadata = metadata,
                rawContent = rawContent,
                sections = sections,
                businessDescription = sections["Item 1"] ?: sections["ITEM 1"],
                riskFactors = extractRiskFactors(cleanedContent),
                properties = sections["Item 2"] ?: sections["ITEM 2"],
                legalProceedings = sections["Item 3"] ?: sections["ITEM 3"],
                mdAndA = extractMdAndA(cleanedContent),
                financialStatements = extractFinancialStatements(cleanedContent),
                controlsAndProcedures = sections["Item 9A"] ?: sections["ITEM 9A"],
                executiveCompensation = sections["Item 11"] ?: sections["ITEM 11"],
                directorInfo = sections["Item 10"] ?: sections["ITEM 10"],
                exhibits = extractExhibits(cleanedContent)
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        // 10-K Item 패턴들
        val itemPatterns =
                Form10KItem.values().map { item ->
                    // "Item 1.", "ITEM 1.", "Item 1 -", "Item 1:" 등 다양한 형식 지원
                    Regex(
                            "(?i)item\\s+${Regex.escape(item.itemNumber)}[.:\\-\\s]+${item.title}",
                            RegexOption.IGNORE_CASE
                    )
                }

        val headerMatches = findSectionHeader(content, itemPatterns)

        val sections =
                extractSectionsFromHeaderMatches(content, headerMatches) { _, headerText ->
                    val itemMatch = Regex("(?i)item\\s+(\\d+[a-z]?)").find(headerText)
                    val itemNumber = itemMatch?.groupValues?.get(1)?.uppercase() ?: return@extractSectionsFromHeaderMatches null
                    "Item $itemNumber"
                }.toMutableMap()

        // Part 기반 섹션도 추출
        extractPartSections(content, sections)

        return sections
    }

    private fun extractPartSections(content: String, sections: MutableMap<String, String>) {
        val partPattern = Regex("(?i)part\\s+([IVX]+)[.:\\-\\s]+([^\n]+)", RegexOption.IGNORE_CASE)
        val matches = partPattern.findAll(content).toList()

        for (i in matches.indices) {
            val match = matches[i]
            val partNumber = match.groupValues[1].uppercase()
            val partTitle = match.groupValues[2].trim()

            val startIndex = match.range.first
            val endIndex =
                    if (i < matches.size - 1) {
                        matches[i + 1].range.first
                    } else {
                        null
                    }

            val sectionContent = extractSection(content, startIndex, endIndex)
            sections["Part $partNumber"] = sectionContent
        }
    }

    private fun extractMdAndA(content: String): ManagementDiscussion? {
        val mdaSection = extractSections(content)["Item 7"] ?: return null

        val execSummary = extractExecutiveSummary(mdaSection)
        val results = extractResultsOfOperations(mdaSection)
        val liquidity = extractLiquiditySection(mdaSection)
        val policies = extractCriticalPolicies(mdaSection)

        return ManagementDiscussion(
                keyBusinessDrivers = listOf(execSummary, results),
                marketConditions = liquidity,
                futureOutlook = "See MD&A section for details",
                criticalAccountingPolicies = listOf(policies)
        )
    }

    private fun extractExecutiveSummary(mdaContent: String): String {
        // "Overview", "Executive Summary" 등의 섹션 찾기
        val summaryPattern =
                Regex(
                        "(?i)(overview|executive\\s+summary|introduction)[.:\\-\\s]*",
                        RegexOption.IGNORE_CASE
                )
        val match = summaryPattern.find(mdaContent) ?: return mdaContent.take(1000)

        // 다음 섹션까지 추출 (최대 2000자)
        val nextSectionPattern =
                Regex("(?i)(results\\s+of\\s+operations|liquidity)", RegexOption.IGNORE_CASE)
        val endMatch = nextSectionPattern.find(mdaContent, match.range.last)

        val summary = extractSection(mdaContent, match.range.first, endMatch?.range?.first)
        return summary.take(2000)
    }

    private fun extractResultsOfOperations(mdaContent: String): String {
        val pattern = Regex("(?i)results\\s+of\\s+operations", RegexOption.IGNORE_CASE)
        val match = pattern.find(mdaContent) ?: return ""

        val endPattern = Regex("(?i)(liquidity|capital\\s+resources)", RegexOption.IGNORE_CASE)
        val endMatch = endPattern.find(mdaContent, match.range.last)

        return extractSection(mdaContent, match.range.first, endMatch?.range?.first).take(3000)
    }

    private fun extractLiquiditySection(mdaContent: String): String {
        val pattern =
                Regex("(?i)liquidity\\s+(and\\s+)?capital\\s+resources", RegexOption.IGNORE_CASE)
        val match = pattern.find(mdaContent) ?: return ""

        val endPattern =
                Regex(
                        "(?i)(critical\\s+accounting|contractual\\s+obligations)",
                        RegexOption.IGNORE_CASE
                )
        val endMatch = endPattern.find(mdaContent, match.range.last)

        return extractSection(mdaContent, match.range.first, endMatch?.range?.first).take(2000)
    }

    private fun extractCriticalPolicies(mdaContent: String): String {
        val pattern =
                Regex("(?i)critical\\s+accounting\\s+(policies|estimates)", RegexOption.IGNORE_CASE)
        val match = pattern.find(mdaContent) ?: return ""

        return extractSection(mdaContent, match.range.first, null).take(2000)
    }

    private fun extractExhibits(content: String): List<String> {
        val exhibits = mutableListOf<String>()

        // "Exhibit 10.1", "Exhibit 31.1" 등 찾기
        val exhibitPattern =
                Regex("(?i)exhibit\\s+(\\d+\\.\\d+)[.:\\-\\s]*([^\n]+)", RegexOption.IGNORE_CASE)
        exhibitPattern.findAll(content).forEach { match ->
            val exhibitNumber = match.groupValues[1]
            val exhibitDescription = match.groupValues[2].trim().take(200)
            exhibits.add("Exhibit $exhibitNumber: $exhibitDescription")
        }

        return exhibits
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
                reportType = "10-K",
                fiscalYear = extractFiscalYear(content),
                fiscalPeriod = "FY",
                incomeStatement = incomeStatement,
                balanceSheet = balanceSheet,
                cashFlowStatement = cashFlow,
                dataQuality = if (statements.isNotEmpty()) DataQuality.HIGH else DataQuality.MEDIUM
        )
    }

    private fun extractPeriod(content: String): String {
        val periodPattern =
                Regex(
                        "(?i)for\\s+the\\s+(fiscal\\s+)?year\\s+ended\\s+([^\n,]+)",
                        RegexOption.IGNORE_CASE
                )
        val match = periodPattern.find(content)
        return match?.groupValues?.get(2)?.trim() ?: "Annual"
    }

    private fun extractFiscalYear(content: String): String {
        val yearPattern =
                Regex(
                        "(?i)(fiscal\\s+year|year\\s+ended)\\s+\\w+\\s+\\d+,?\\s+(\\d{4})",
                        RegexOption.IGNORE_CASE
                )
        val match = yearPattern.find(content)
        return match?.groupValues?.get(2) ?: ""
    }
}
