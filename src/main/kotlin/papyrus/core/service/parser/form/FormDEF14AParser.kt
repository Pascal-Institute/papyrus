package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * DEF 14A Proxy Statement 전문 파서
 *
 * DEF 14A는 주주총회 위임장으로, 다음을 포함합니다:
 * - 주주총회 일정 및 의결 사항
 * - 경영진 보상 정보
 * - 이사회 및 위원회 정보
 * - 기업 지배구조
 */
class FormDEF14AParser : BaseSecReportParser<FormDEF14AParseResult>(SecReportType.FORM_DEF14A) {

        override fun parseHtml(
                htmlContent: String,
                metadata: SecReportMetadata
        ): FormDEF14AParseResult {
                val cleanedContent = cleanHtml(htmlContent)
                return parseContent(cleanedContent, htmlContent, metadata)
        }

        override fun parseText(
                textContent: String,
                metadata: SecReportMetadata
        ): FormDEF14AParseResult {
                return parseContent(textContent, textContent, metadata)
        }

        private fun parseContent(
                cleanedContent: String,
                rawContent: String,
                metadata: SecReportMetadata
        ): FormDEF14AParseResult {
                val sections = extractSections(cleanedContent)

                return FormDEF14AParseResult(
                        metadata = metadata,
                        rawContent = rawContent,
                        sections = sections,
                        meetingDate = extractMeetingDate(cleanedContent),
                        votingMatters = extractVotingMatters(cleanedContent),
                        executiveCompensation = sections["Executive Compensation"],
                        compensationTables = sections["Compensation Tables"],
                        directorInfo = sections["Directors"],
                        corporateGovernance = sections["Corporate Governance"],
                        auditInfo = sections["Audit"]
                )
        }

        override fun extractSections(content: String): Map<String, String> {
                val sectionPatterns =
                        listOf(
                                "Meeting Information" to
                                        Regex(
                                                "(?i)notice\\s+of\\s+annual\\s+meeting",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Voting Matters" to
                                        Regex(
                                                "(?i)matters\\s+to\\s+be\\s+voted",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Directors" to
                                        Regex(
                                                "(?i)(proposal|election)\\s+of\\s+directors",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Executive Compensation" to
                                        Regex(
                                                "(?i)executive\\s+compensation",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Compensation Tables" to
                                        Regex(
                                                "(?i)summary\\s+compensation\\s+table",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Corporate Governance" to
                                        Regex(
                                                "(?i)corporate\\s+governance",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Audit" to Regex("(?i)audit\\s+committee", RegexOption.IGNORE_CASE),
                                "Stock Ownership" to
                                        Regex("(?i)security\\s+ownership", RegexOption.IGNORE_CASE)
                        )

                return extractNamedSections(content, sectionPatterns)
        }

        private fun extractMeetingDate(content: String): String? {
                val datePattern =
                        Regex(
                                "(?i)annual\\s+meeting.*?(\\w+\\s+\\d+,?\\s+\\d{4})",
                                RegexOption.IGNORE_CASE
                        )

                val match = datePattern.find(content)
                return match?.groupValues?.get(1)
        }

        private fun extractVotingMatters(content: String): List<String> {
                val matters = mutableListOf<String>()

                // "Proposal 1:", "Proposal 2:" 등 찾기
                val proposalPattern =
                        Regex("(?i)proposal\\s+(\\d+)[.:\\-\\s]+([^\n]+)", RegexOption.IGNORE_CASE)

                proposalPattern.findAll(content).forEach { match ->
                        val proposalNumber = match.groupValues[1]
                        val proposalText = match.groupValues[2].trim().take(200)
                        matters.add("Proposal $proposalNumber: $proposalText")
                }

                return matters
        }

        override fun extractFinancialStatements(content: String): StructuredFinancialData? {
                // DEF 14A에는 재무제표가 포함되지 않음
                return null
        }

        override fun extractRiskFactors(content: String): List<RiskFactor> {
                // DEF 14A에는 리스크 요인이 포함되지 않음
                return emptyList()
        }
}
