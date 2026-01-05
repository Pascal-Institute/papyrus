package papyrus.core.service.parser

import papyrus.core.model.*

/** SEC 보고서 파서의 공통 인터페이스 */
interface SecReportParser<T : SecReportParseResult> {
    /** 보고서 타입 */
    val reportType: SecReportType

    /** HTML 형식의 SEC 보고서를 파싱합니다 */
    fun parseHtml(htmlContent: String, metadata: SecReportMetadata): T

    /** 텍스트 형식의 SEC 보고서를 파싱합니다 */
    fun parseText(textContent: String, metadata: SecReportMetadata): T

    /** 보고서에서 섹션을 추출합니다 */
    fun extractSections(content: String): Map<String, String>

    /** 보고서에서 재무제표를 추출합니다 */
    fun extractFinancialStatements(content: String): StructuredFinancialData?

    /** 보고서에서 리스크 요인을 추출합니다 */
    fun extractRiskFactors(content: String): List<RiskFactor>
}

/** SEC 보고서 파서의 기본 구현을 제공하는 추상 클래스 */
abstract class BaseSecReportParser<T : SecReportParseResult>(
        override val reportType: SecReportType
) : SecReportParser<T> {

    /** HTML 정리 (공통) */
    protected fun cleanHtml(html: String): String {
        return SecTextNormalization.cleanHtmlToText(html)
    }

    /**
     * Extract sections by detecting a single header match per logical section name.
     * This is useful for forms like S-1 / DEF 14A / 20-F where sections are not strictly item-numbered.
     */
    protected fun extractNamedSections(
            content: String,
            sectionPatterns: List<Pair<String, Regex>>
    ): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        val headerMatches =
                sectionPatterns
                        .mapNotNull { (sectionName, pattern) ->
                            pattern.find(content)?.let { match ->
                                Triple(match.range.first, sectionName, match.value)
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

    /** 섹션 헤더 패턴 매칭 */
    protected fun findSectionHeader(
            content: String,
            patterns: List<Regex>
    ): List<Pair<Int, String>> {
        val matches = mutableListOf<Pair<Int, String>>()

        for (pattern in patterns) {
            pattern.findAll(content).forEach { match ->
                matches.add(match.range.first to match.value)
            }
        }

        return matches.sortedBy { it.first }
    }

    /** 문자열에서 섹션 추출 (시작 위치와 다음 섹션 사이) */
    protected fun extractSection(content: String, startIndex: Int, endIndex: Int?): String {
        val end = endIndex ?: content.length
        return content.substring(startIndex, end.coerceAtMost(content.length)).trim()
    }

    /** 기본 리스크 요인 추출 (하위 클래스에서 오버라이드 가능) */
    override fun extractRiskFactors(content: String): List<RiskFactor> {
        val riskFactors = mutableListOf<RiskFactor>()

        // "Risk Factor" 섹션 찾기
        val riskSectionPattern =
                Regex("(?i)(item\\s+1a\\.|risk\\s+factors)", RegexOption.IGNORE_CASE)
        val match = riskSectionPattern.find(content) ?: return emptyList()

        // 다음 섹션까지 추출
        val nextSectionPattern =
                Regex("(?i)(item\\s+\\d+[a-z]?\\.|part\\s+[ivi]+)", RegexOption.IGNORE_CASE)
        val nextMatch = nextSectionPattern.find(content, match.range.last + 1)

        val riskSection = extractSection(content, match.range.first, nextMatch?.range?.first)

        // 간단한 리스크 요인 파싱 (문단 기반)
        val paragraphs = riskSection.split(Regex("\n\n+"))

        paragraphs.forEach { paragraph ->
            if (paragraph.length > 100) { // 유의미한 길이
                riskFactors.add(
                        RiskFactor(
                                title = "Risk Factor",
                                summary = paragraph.take(500), // 첫 500자
                                category = RiskCategory.OTHER,
                                severity = RiskSeverity.MEDIUM
                        )
                )
            }
        }

        return riskFactors
    }

    /** 기본 재무제표 추출 (하위 클래스에서 오버라이드 권장) */
    override fun extractFinancialStatements(content: String): StructuredFinancialData? {
        // 기본 구현은 null 반환, 각 파서에서 구체적으로 구현
        return null
    }
}
