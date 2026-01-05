package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * 8-K 현재 보고서 전문 파서
 *
 * 8-K는 중요 이벤트 발생 시 제출되는 보고서로, 다음을 포함합니다:
 * - 재무 결과 발표
 * - 인수합병 및 자산 거래
 * - 경영진 변동
 * - 파산 및 채무불이행
 * - 기타 중요 이벤트
 */
class Form8KParser : BaseSecReportParser<Form8KParseResult>(SecReportType.FORM_8K) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): Form8KParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return parseContent(cleanedContent, htmlContent, metadata)
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): Form8KParseResult {
        return parseContent(textContent, textContent, metadata)
    }

    private fun parseContent(
            cleanedContent: String,
            rawContent: String,
            metadata: SecReportMetadata
    ): Form8KParseResult {
        val sections = extractSections(cleanedContent)
        val eventItems = extractEventItems(cleanedContent)

        return Form8KParseResult(
                metadata = metadata,
                rawContent = rawContent,
                sections = sections,
                eventDate = extractEventDate(cleanedContent),
                eventItems = eventItems.keys.toList(),
                eventDescriptions = eventItems,
                financialResults = sections["Item 2.02"],
                acquisitions = sections["Item 2.01"],
                dispositions = sections["Item 2.01"],
                executiveChanges = sections["Item 5.02"],
                bankruptcy = sections["Item 1.03"],
                exhibits = extractExhibits(cleanedContent)
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        // 8-K Item 패턴들 (Item 1.01, Item 2.02 등)
        val itemPatterns =
                Form8KItem.values().map { item ->
                    Regex(
                            "(?i)item\\s+${Regex.escape(item.itemNumber)}[.:\\-\\s]+",
                            RegexOption.IGNORE_CASE
                    )
                }

        val headerMatches = findSectionHeader(content, itemPatterns)

        // 각 섹션 추출
        for (i in headerMatches.indices) {
            val (startIndex, headerText) = headerMatches[i]
            val endIndex =
                    if (i < headerMatches.size - 1) {
                        headerMatches[i + 1].first
                    } else {
                        null
                    }

            // Item 번호 추출 (1.01, 2.02 등)
            val itemMatch = Regex("(?i)item\\s+(\\d+\\.\\d+)").find(headerText)
            val itemNumber = itemMatch?.groupValues?.get(1) ?: continue

            val sectionContent = extractSection(content, startIndex, endIndex)
            sections["Item $itemNumber"] = sectionContent
        }

        return sections
    }

    private fun extractEventItems(content: String): Map<String, String> {
        val eventItems = mutableMapOf<String, String>()

        // 보고된 모든 Item 추출
        Form8KItem.values().forEach { item ->
            val itemPattern =
                    Regex(
                            "(?i)item\\s+${Regex.escape(item.itemNumber)}[.:\\-\\s]+([^\n]*)",
                            RegexOption.IGNORE_CASE
                    )

            val match = itemPattern.find(content)
            if (match != null) {
                val itemNumber = "Item ${item.itemNumber}"
                val description = "${item.title} (${item.category})"
                eventItems[itemNumber] = description
            }
        }

        return eventItems
    }

    private fun extractEventDate(content: String): String? {
        // "Date of Report (Date of earliest event reported): March 15, 2024"
        val datePattern =
                Regex(
                        "(?i)date\\s+of\\s+(?:earliest\\s+)?event\\s+(?:reported)?[:\\s]+(\\w+\\s+\\d+,?\\s+\\d{4})",
                        RegexOption.IGNORE_CASE
                )

        val match = datePattern.find(content)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun extractExhibits(content: String): List<String> {
        val exhibits = mutableListOf<String>()

        // Item 9.01에서 Exhibits 찾기
        val item901Section = extractSections(content)["Item 9.01"]

        if (!item901Section.isNullOrBlank()) {
            val exhibitPattern =
                    Regex(
                            "(?i)exhibit\\s+(\\d+\\.\\d+)[.:\\-\\s]*([^\n]+)",
                            RegexOption.IGNORE_CASE
                    )

            exhibitPattern.findAll(item901Section).forEach { match ->
                val exhibitNumber = match.groupValues[1]
                val exhibitDescription = match.groupValues[2].trim().take(150)
                exhibits.add("Exhibit $exhibitNumber: $exhibitDescription")
            }
        }

        return exhibits
    }

    override fun extractFinancialStatements(content: String): StructuredFinancialData? {
        // 8-K는 보통 완전한 재무제표를 포함하지 않음
        // Item 2.02 (Results of Operations)에 요약 재무 정보가 있을 수 있음
        val item202Section = extractSections(content)["Item 2.02"]

        if (item202Section.isNullOrBlank()) {
            return null
        }

        return StructuredFinancialData(
                companyName = null,
                reportType = "8-K",
                fiscalYear = null,
                fiscalPeriod = "Event",
                incomeStatement = null,
                balanceSheet = null,
                cashFlowStatement = null,
                dataQuality = DataQuality.LOW
        )
    }

    override fun extractRiskFactors(content: String): List<RiskFactor> {
        // 8-K에는 보통 Risk Factors가 없음
        // 하지만 Item 1.03 (Bankruptcy)와 같은 중대한 이벤트는 리스크로 간주
        val riskFactors = mutableListOf<RiskFactor>()

        // Bankruptcy 체크
        val bankruptcySection = extractSections(content)["Item 1.03"]
        if (!bankruptcySection.isNullOrBlank()) {
            riskFactors.add(
                    RiskFactor(
                            title = "Bankruptcy Event",
                            summary = "Bankruptcy or receivership proceedings disclosed",
                            category = RiskCategory.FINANCIAL,
                            severity = RiskSeverity.CRITICAL
                    )
            )
        }

        // Default or acceleration 체크
        val defaultSection = extractSections(content)["Item 2.04"]
        if (!defaultSection.isNullOrBlank()) {
            riskFactors.add(
                    RiskFactor(
                            title = "Debt Acceleration",
                            summary = "Triggering events that accelerate or increase obligations",
                            category = RiskCategory.FINANCIAL,
                            severity = RiskSeverity.HIGH
                    )
            )
        }

        // Material impairments 체크
        val impairmentSection = extractSections(content)["Item 2.06"]
        if (!impairmentSection.isNullOrBlank()) {
            riskFactors.add(
                    RiskFactor(
                            title = "Material Impairment",
                            summary = "Material impairments disclosed",
                            category = RiskCategory.FINANCIAL,
                            severity = RiskSeverity.MEDIUM
                    )
            )
        }

        return riskFactors
    }

    /** 8-K의 중요도를 평가합니다 특정 Item들은 더 중요한 이벤트를 나타냅니다 */
    fun calculateImportanceScore(parseResult: Form8KParseResult): Int {
        var score = 0

        // 고위험 이벤트
        if (parseResult.bankruptcy != null) score += 10
        if (parseResult.eventItems.contains("Item 1.03")) score += 10
        if (parseResult.eventItems.contains("Item 2.04")) score += 8

        // 재무 관련
        if (parseResult.financialResults != null) score += 7
        if (parseResult.eventItems.contains("Item 2.02")) score += 7

        // M&A 관련
        if (parseResult.acquisitions != null) score += 8
        if (parseResult.eventItems.contains("Item 2.01")) score += 8

        // 경영진 변동
        if (parseResult.executiveChanges != null) score += 6
        if (parseResult.eventItems.contains("Item 5.02")) score += 6

        // 지배구조 변화
        if (parseResult.eventItems.contains("Item 5.01")) score += 7 // Change in control

        return score
    }
}
