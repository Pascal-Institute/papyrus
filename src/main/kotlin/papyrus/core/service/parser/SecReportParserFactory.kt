package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * SEC 보고서 타입별 파서를 생성하는 팩토리
 *
 * 보고서 형식(10-K, 10-Q, 8-K 등)에 따라 적절한 전문 파서를 반환합니다.
 */
object SecReportParserFactory {

    /** 보고서 타입에 맞는 파서를 반환합니다 */
    fun getParser(reportType: SecReportType): SecReportParser<out SecReportParseResult> {
        return when (reportType) {
            SecReportType.FORM_10K -> Form10KParser()
            SecReportType.FORM_10Q -> Form10QParser()
            SecReportType.FORM_8K -> Form8KParser()
            SecReportType.FORM_S1 -> FormS1Parser()
            SecReportType.FORM_DEF14A -> FormDEF14AParser()
            SecReportType.FORM_20F -> Form20FParser()

            // 나머지는 GenericParser 사용
            else -> GenericSecReportParser(reportType)
        }
    }

    /** 보고서 형식 문자열로부터 파서를 반환합니다 */
    fun getParserByFormType(formType: String): SecReportParser<out SecReportParseResult> {
        val reportType = SecReportType.fromFormType(formType)
        return getParser(reportType)
    }

    /** 지원되는 모든 파서 타입 반환 */
    fun getSupportedFormTypes(): List<SecReportType> {
        return listOf(
                SecReportType.FORM_10K,
                SecReportType.FORM_10Q,
                SecReportType.FORM_8K,
                SecReportType.FORM_S1,
                SecReportType.FORM_DEF14A,
                SecReportType.FORM_20F
        )
    }

    /** 특정 보고서 타입에 전문 파서가 있는지 확인 */
    fun hasSpecializedParser(reportType: SecReportType): Boolean {
        return reportType in getSupportedFormTypes()
    }
}

/** 범용 SEC 보고서 파서 (전문 파서가 없는 형식용) */
class GenericSecReportParser(reportType: SecReportType) :
        BaseSecReportParser<GenericSecReportParseResult>(reportType) {

    override fun parseHtml(
            htmlContent: String,
            metadata: SecReportMetadata
    ): GenericSecReportParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return GenericSecReportParseResult(
                metadata = metadata,
                rawContent = htmlContent,
                sections = extractSections(cleanedContent)
        )
    }

    override fun parseText(
            textContent: String,
            metadata: SecReportMetadata
    ): GenericSecReportParseResult {
        return GenericSecReportParseResult(
                metadata = metadata,
                rawContent = textContent,
                sections = extractSections(textContent)
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        // 기본적인 섹션 패턴들
        val sectionPatterns =
                listOf(
                        Regex(
                                "(?i)(item|section|part)\\s+([\\dA-Z]+)[.:\\-\\s]+([^\n]+)",
                                RegexOption.IGNORE_CASE
                        ),
                        Regex(
                                "(?i)(introduction|summary|risk|business|financial)[.:\\-\\s]*",
                                RegexOption.IGNORE_CASE
                        )
                )

        val headerMatches = findSectionHeader(content, sectionPatterns)

        for (i in headerMatches.indices) {
            val (startIndex, headerText) = headerMatches[i]
            val endIndex =
                    if (i < headerMatches.size - 1) {
                        headerMatches[i + 1].first
                    } else {
                        null
                    }

            val sectionContent = extractSection(content, startIndex, endIndex)
            sections["Section $i: ${headerText.take(50)}"] = sectionContent
        }

        return sections
    }
}

/** SEC 보고서 파싱을 위한 유틸리티 함수들 */
object SecReportParsingUtils {

    /** HTML 또는 텍스트 콘텐츠를 자동으로 감지하고 파싱합니다 */
    fun parseReport(
            content: String,
            formType: String,
            metadata: SecReportMetadata
    ): SecReportParseResult {
        val parser = SecReportParserFactory.getParserByFormType(formType)

        return if (isHtmlContent(content)) {
            parser.parseHtml(content, metadata)
        } else {
            parser.parseText(content, metadata)
        }
    }

    /** 콘텐츠가 HTML인지 확인 */
    private fun isHtmlContent(content: String): Boolean {
        return content.trim().startsWith("<", ignoreCase = true) ||
                content.contains("<html", ignoreCase = true) ||
                content.contains("<!DOCTYPE", ignoreCase = true)
    }

    /** 보고서 타입별 중요도 점수 반환 */
    fun getReportImportanceScore(reportType: SecReportType): Int {
        return reportType.importance
    }

    /** 보고서가 재무제표를 포함하는지 여부 */
    fun hasFinancialStatements(reportType: SecReportType): Boolean {
        return reportType in
                listOf(
                        SecReportType.FORM_10K,
                        SecReportType.FORM_10Q,
                        SecReportType.FORM_20F,
                        SecReportType.FORM_S1
                )
    }

    /** 보고서가 감사된 재무제표를 포함하는지 여부 */
    fun hasAuditedFinancials(reportType: SecReportType): Boolean {
        return reportType in
                listOf(SecReportType.FORM_10K, SecReportType.FORM_20F, SecReportType.FORM_S1)
    }

    /** 보고서 타입에 대한 설명 반환 */
    fun getReportDescription(reportType: SecReportType): String {
        return when (reportType) {
            SecReportType.FORM_10K ->
                    "Annual report with comprehensive financial information and audited statements"
            SecReportType.FORM_10Q -> "Quarterly report with unaudited financial statements"
            SecReportType.FORM_8K -> "Current report for material events and significant changes"
            SecReportType.FORM_S1 ->
                    "IPO registration statement with detailed business and financial history"
            SecReportType.FORM_DEF14A ->
                    "Proxy statement with executive compensation and governance information"
            SecReportType.FORM_20F -> "Annual report for foreign companies (similar to 10-K)"
            SecReportType.FORM_6K -> "Current report for foreign companies (similar to 8-K)"
            else -> "SEC filing: ${reportType.displayName}"
        }
    }
}
