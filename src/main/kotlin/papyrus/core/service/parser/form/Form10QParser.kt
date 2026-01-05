package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * 10-Q 분기 보고서 전문 파서
 *
 * 10-Q는 분기별 재무 보고서로, 다음을 포함합니다:
 * - Part I: Financial Information (재무제표, MD&A)
 * - Part II: Other Information (법적 절차, 리스크 요인)
 */
class Form10QParser : BaseSecReportParser<Form10QParseResult>(SecReportType.FORM_10Q) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form10QParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return parseContent(cleanedContent, htmlContent, metadata)
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form10QParseResult {
        return parseContent(textContent, textContent, metadata)
    }

    private fun parseContent(
            cleanedContent: String,
            rawContent: String,
            metadata: SecReportMetadata
    ): Form10QParseResult {
        val sections = extractSections(cleanedContent)
        val quarterInfo = extractQuarterInfo(cleanedContent)

        return Form10QParseResult(
                metadata = metadata,
                rawContent = rawContent,
                sections = sections,
                financialStatements = extractFinancialStatements(cleanedContent),
                mdAndA = extractMdAndA(cleanedContent),
                marketRiskDisclosures = sections["Part I - Item 3"],
                controlsAndProcedures = sections["Part I - Item 4"],
                legalProceedings = sections["Part II - Item 1"],
                riskFactors = extractRiskFactors(cleanedContent),
                exhibits = extractExhibits(cleanedContent),
                quarter = quarterInfo.first,
                fiscalYear = quarterInfo.second
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        // 10-Q Part + Item 패턴들
        val itemPatterns =
                listOf(
                        // Part I
                        Regex(
                                "(?i)part\\s+i[^i].*?item\\s+1[.:\\-\\s]+(financial\\s+statements|condensed)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)part\\s+i[^i].*?item\\s+2[.:\\-\\s]+(management'?s\\s+discussion|md&a)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)part\\s+i[^i].*?item\\s+3[.:\\-\\s]+(quantitative|market\\s+risk)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)part\\s+i[^i].*?item\\s+4[.:\\-\\s]+(controls|procedures)",
                                RegexOption.IGNORE_CASE
                        ),

                        // Part II
                        Regex(
                                "(?i)part\\s+ii.*?item\\s+1[.:\\-\\s]+(legal\\s+proceedings)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)part\\s+ii.*?item\\s+1a[.:\\-\\s]+(risk\\s+factors)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)part\\s+ii.*?item\\s+2[.:\\-\\s]+(unregistered\\s+sales)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)part\\s+ii.*?item\\s+6[.:\\-\\s]+(exhibits)",
                                RegexOption.IGNORE_CASE
                        )
                )

        val headerMatches = findSectionHeader(content, itemPatterns)

                return extractSectionsFromHeaderMatches(content, headerMatches) { _, headerText ->
                        val partMatch = Regex("(?i)part\\s+(i+)").find(headerText)
                        val itemMatch = Regex("(?i)item\\s+(\\d+[a-z]?)").find(headerText)

                        if (partMatch == null || itemMatch == null) return@extractSectionsFromHeaderMatches null
                        val partNumber = partMatch.groupValues[1].uppercase()
                        val itemNumber = itemMatch.groupValues[1].uppercase()
                        "Part $partNumber - Item $itemNumber"
                }
    }

    private fun extractMdAndA(content: String): ManagementDiscussion? {
        val mdaSection = extractSections(content)["Part I - Item 2"] ?: return null

        val execSummary = extractExecutiveSummary(mdaSection)
        val results = extractResultsOfOperations(mdaSection)
        val liquidity = extractLiquiditySection(mdaSection)

        return ManagementDiscussion(
                keyBusinessDrivers = listOf(execSummary, results),
                marketConditions = liquidity,
                futureOutlook = "See MD&A section for details",
                criticalAccountingPolicies = emptyList()
        )
    }

    private fun extractExecutiveSummary(mdaContent: String): String {
        val summaryPattern =
                Regex("(?i)(overview|executive\\s+summary)[.:\\-\\s]*", RegexOption.IGNORE_CASE)
        val match = summaryPattern.find(mdaContent) ?: return mdaContent.take(1000)

        val nextSectionPattern =
                Regex("(?i)(results\\s+of\\s+operations|three\\s+months)", RegexOption.IGNORE_CASE)
        val endMatch = nextSectionPattern.find(mdaContent, match.range.last)

        return extractSection(mdaContent, match.range.first, endMatch?.range?.first).take(1500)
    }

    private fun extractResultsOfOperations(mdaContent: String): String {
        val pattern = Regex("(?i)results\\s+of\\s+operations", RegexOption.IGNORE_CASE)
        val match = pattern.find(mdaContent) ?: return ""

        val endPattern =
                Regex(
                        "(?i)(liquidity|capital\\s+resources|financial\\s+condition)",
                        RegexOption.IGNORE_CASE
                )
        val endMatch = endPattern.find(mdaContent, match.range.last)

        return extractSection(mdaContent, match.range.first, endMatch?.range?.first).take(2500)
    }

    private fun extractLiquiditySection(mdaContent: String): String {
        val pattern =
                Regex("(?i)liquidity\\s+(and\\s+)?capital\\s+resources", RegexOption.IGNORE_CASE)
        val match = pattern.find(mdaContent) ?: return ""

        return extractSection(mdaContent, match.range.first, null).take(1500)
    }

    private fun extractExhibits(content: String): List<String> {
        val exhibits = mutableListOf<String>()

        val exhibitPattern =
                Regex("(?i)exhibit\\s+(\\d+\\.\\d+)[.:\\-\\s]*([^\n]+)", RegexOption.IGNORE_CASE)
        exhibitPattern.findAll(content).forEach { match ->
            val exhibitNumber = match.groupValues[1]
            val exhibitDescription = match.groupValues[2].trim().take(150)
            exhibits.add("Exhibit $exhibitNumber: $exhibitDescription")
        }

        return exhibits.distinct() // 중복 제거
    }

    private fun extractQuarterInfo(content: String): Pair<String?, String?> {
        // "For the quarterly period ended March 31, 2024" 형식 찾기
        val quarterPattern =
                Regex(
                        "(?i)for\\s+the\\s+(?:quarterly|three\\s+months)\\s+period\\s+ended\\s+(\\w+)\\s+(\\d+),?\\s+(\\d{4})",
                        RegexOption.IGNORE_CASE
                )
        val match = quarterPattern.find(content)

        if (match != null) {
            val month = match.groupValues[1]
            val year = match.groupValues[3]

            val quarter =
                    when (month.uppercase()) {
                        "MARCH", "MAR" -> "Q1"
                        "JUNE", "JUN" -> "Q2"
                        "SEPTEMBER", "SEP", "SEPT" -> "Q3"
                        "DECEMBER", "DEC" -> "Q4"
                        else -> null
                    }

            return Pair(quarter, year)
        }

        // 대체 패턴: "Q1 2024", "First Quarter 2024"
        val altPattern =
                Regex(
                        "(?i)(q[1-4]|first|second|third)\\s+quarter\\s+(?:of\\s+)?(\\d{4})",
                        RegexOption.IGNORE_CASE
                )
        val altMatch = altPattern.find(content)

        if (altMatch != null) {
            val quarterText = altMatch.groupValues[1].uppercase()
            val year = altMatch.groupValues[2]

            val quarter =
                    when {
                        quarterText.startsWith("Q") -> quarterText
                        quarterText == "FIRST" -> "Q1"
                        quarterText == "SECOND" -> "Q2"
                        quarterText == "THIRD" -> "Q3"
                        else -> null
                    }

            return Pair(quarter, year)
        }

        return Pair(null, null)
    }

    override fun extractFinancialStatements(content: String): StructuredFinancialData? {
        val quarterInfo = extractQuarterInfo(content)
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
                reportType = "10-Q",
                fiscalYear = quarterInfo.second,
                fiscalPeriod = quarterInfo.first ?: "Quarterly",
                incomeStatement = incomeStatement,
                balanceSheet = balanceSheet,
                cashFlowStatement = cashFlow,
                dataQuality = if (statements.isNotEmpty()) DataQuality.HIGH else DataQuality.MEDIUM
        )
    }

    override fun extractRiskFactors(content: String): List<RiskFactor> {
        // Part II - Item 1A에서 리스크 요인 추출
        val riskSection = extractSections(content)["Part II - Item 1A"]

        if (riskSection.isNullOrBlank()) {
            return emptyList()
        }

        val riskFactors = mutableListOf<RiskFactor>()

        // 10-Q는 보통 "There have been no material changes" 또는 업데이트된 리스크만 포함
        if (riskSection.contains("no material changes", ignoreCase = true)) {
            riskFactors.add(
                    RiskFactor(
                            title = "No Material Changes",
                            summary =
                                    "No material changes to risk factors since last annual report",
                            category = RiskCategory.OTHER,
                            severity = RiskSeverity.LOW
                    )
            )
        } else {
            // 변경된 리스크 요인 파싱
            val paragraphs = riskSection.split(Regex("\n\n+"))
            paragraphs.forEach { paragraph ->
                if (paragraph.length > 100) {
                    riskFactors.add(
                            RiskFactor(
                                    title = "Updated Risk Factor",
                                    summary = paragraph.take(500),
                                    category = RiskCategory.OTHER,
                                    severity = RiskSeverity.MEDIUM
                            )
                    )
                }
            }
        }

        return riskFactors
    }
}
