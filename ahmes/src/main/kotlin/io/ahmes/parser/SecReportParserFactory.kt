package io.ahmes.parser

import io.ahmes.model.*
import io.ahmes.form.*

/**
 * SEC Report Parser Factory
 *
 * Creates appropriate parser based on SEC report type (10-K, 10-Q, 8-K, etc.)
 */
object SecReportParserFactory {

    /** Get parser for the specified report type */
    fun getParser(reportType: SecReportType): SecReportParser<out SecReportParseResult> {
        return when (reportType) {
            SecReportType.FORM_10K -> Form10KParser()
            SecReportType.FORM_10Q -> Form10QParser()
            SecReportType.FORM_8K -> Form8KParser()
            SecReportType.FORM_S1 -> FormS1Parser()
            SecReportType.FORM_DEF14A -> FormDEF14AParser()
            SecReportType.FORM_20F -> Form20FParser()
            else -> GenericSecReportParser(reportType)
        }
    }

    /** Get parser by form type string */
    fun getParserByFormType(formType: String): SecReportParser<out SecReportParseResult> {
        val reportType = SecReportType.fromFormType(formType)
        return getParser(reportType)
    }

    /** Get all supported form types */
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

    /** Check if specialized parser exists for report type */
    fun hasSpecializedParser(reportType: SecReportType): Boolean {
        return reportType in getSupportedFormTypes()
    }
}

/** Generic SEC Report Parser for types without specialized parsers */
class GenericSecReportParser(reportType: SecReportType) :
    BaseSecReportParser<GenericSecReportParseResult>(reportType) {

    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): GenericSecReportParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        return GenericSecReportParseResult(
            metadata = metadata,
            rawContent = htmlContent,
            sections = extractSections(cleanedContent)
        )
    }

    override fun parseText(textContent: String, metadata: SecReportMetadata): GenericSecReportParseResult {
        return GenericSecReportParseResult(
            metadata = metadata,
            rawContent = textContent,
            sections = extractSections(textContent)
        )
    }

    override fun extractSections(content: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()

        val sectionPatterns = listOf(
            Regex("(?i)(item|section|part)\\s+([\\dA-Z]+)[.:\\-\\s]+([^\n]+)", RegexOption.IGNORE_CASE),
            Regex("(?i)(introduction|summary|risk|business|financial)[.:\\-\\s]*", RegexOption.IGNORE_CASE)
        )

        val headerMatches = findSectionHeader(content, sectionPatterns)

        for (i in headerMatches.indices) {
            val (startIndex, headerText) = headerMatches[i]
            val endIndex = if (i < headerMatches.size - 1) {
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

/** SEC Report Parsing utilities */
object SecReportParsingUtils {

    /** Parse report with automatic content type detection */
    fun parseReport(content: String, formType: String, metadata: SecReportMetadata): SecReportParseResult {
        val parser = SecReportParserFactory.getParserByFormType(formType)

        return if (isHtmlContent(content)) {
            parser.parseHtml(content, metadata)
        } else {
            parser.parseText(content, metadata)
        }
    }

    /** Check if content is HTML */
    private fun isHtmlContent(content: String): Boolean {
        return content.trim().startsWith("<", ignoreCase = true) ||
            content.contains("<html", ignoreCase = true) ||
            content.contains("<!DOCTYPE", ignoreCase = true)
    }

    /** Get report importance score */
    fun getReportImportanceScore(reportType: SecReportType): Int {
        return reportType.importance
    }

    /** Check if report contains financial statements */
    fun hasFinancialStatements(reportType: SecReportType): Boolean {
        return reportType in listOf(
            SecReportType.FORM_10K,
            SecReportType.FORM_10Q,
            SecReportType.FORM_20F,
            SecReportType.FORM_S1
        )
    }

    /** Check if report contains audited financial statements */
    fun hasAuditedFinancials(reportType: SecReportType): Boolean {
        return reportType in listOf(
            SecReportType.FORM_10K,
            SecReportType.FORM_20F,
            SecReportType.FORM_S1
        )
    }

    /** Get description for report type */
    fun getReportDescription(reportType: SecReportType): String {
        return when (reportType) {
            SecReportType.FORM_10K -> "Annual report with comprehensive financial information and audited statements"
            SecReportType.FORM_10Q -> "Quarterly report with unaudited financial statements"
            SecReportType.FORM_8K -> "Current report for material events and significant changes"
            SecReportType.FORM_S1 -> "IPO registration statement with detailed business and financial history"
            SecReportType.FORM_DEF14A -> "Proxy statement with executive compensation and governance information"
            SecReportType.FORM_20F -> "Annual report for foreign companies (similar to 10-K)"
            SecReportType.FORM_6K -> "Current report for foreign companies (similar to 8-K)"
            else -> "SEC filing: ${reportType.displayName}"
        }
    }
}
