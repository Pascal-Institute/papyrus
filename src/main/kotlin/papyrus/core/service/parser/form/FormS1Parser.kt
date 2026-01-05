package papyrus.core.service.parser

import papyrus.core.model.*

/**
 * S-1 IPO 등록서 전문 파서
 *
 * S-1은 기업공개(IPO) 등록서로, 다음을 포함합니다:
 * - 투자설명서 (Prospectus)
 * - 사업 개요 및 전략
 * - 리스크 요인
 * - 자금 사용 계획
 * - 재무제표 (3-5년)
 * - 경영진 및 주주 정보
 */
class FormS1Parser : BaseSecReportParser<FormS1ParseResult>(SecReportType.FORM_S1) {

        override fun parseHtml(
                htmlContent: String,
                metadata: SecReportMetadata
        ): FormS1ParseResult {
                val cleanedContent = cleanHtml(htmlContent)
                return parseContent(cleanedContent, htmlContent, metadata)
        }

        override fun parseText(
                textContent: String,
                metadata: SecReportMetadata
        ): FormS1ParseResult {
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
                val sections = mutableMapOf<String, String>()

                // S-1 주요 섹션 패턴들
                val sectionPatterns =
                        listOf(
                                "Prospectus Summary" to
                                        Regex("(?i)prospectus\\s+summary", RegexOption.IGNORE_CASE),
                                "Risk Factors" to
                                        Regex("(?i)risk\\s+factors", RegexOption.IGNORE_CASE),
                                "Use of Proceeds" to
                                        Regex("(?i)use\\s+of\\s+proceeds", RegexOption.IGNORE_CASE),
                                "Dividend Policy" to
                                        Regex("(?i)dividend\\s+policy", RegexOption.IGNORE_CASE),
                                "Dilution" to Regex("(?i)dilution", RegexOption.IGNORE_CASE),
                                "Capitalization" to
                                        Regex("(?i)capitalization", RegexOption.IGNORE_CASE),
                                "Business" to
                                        Regex("(?i)(our\\s+)?business", RegexOption.IGNORE_CASE),
                                "Management" to
                                        Regex(
                                                "(?i)management'?s?\\s+discussion",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Executive Compensation" to
                                        Regex(
                                                "(?i)executive\\s+compensation",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Directors and Officers" to
                                        Regex(
                                                "(?i)directors\\s+(and|,)\\s+executive\\s+officers",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Principal Stockholders" to
                                        Regex(
                                                "(?i)principal\\s+(stockholders|shareholders)",
                                                RegexOption.IGNORE_CASE
                                        ),
                                "Underwriting" to
                                        Regex("(?i)underwriting", RegexOption.IGNORE_CASE),
                                "Legal Matters" to
                                        Regex("(?i)legal\\s+matters", RegexOption.IGNORE_CASE)
                        )

                val headerMatches =
                        sectionPatterns
                                .mapNotNull { (sectionName, pattern) ->
                                        pattern.find(content)?.let {
                                                Triple(it.range.first, sectionName, it.value)
                                        }
                                }
                                .sortedBy { it.first }

                // 각 섹션 추출
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
                // "$15.00 to $17.00 per share" 형식 찾기
                val pricePattern =
                        Regex(
                                "(?i)\\$([0-9,]+\\.\\d{2})\\s+to\\s+\\$([0-9,]+\\.\\d{2})\\s+per\\s+share",
                                RegexOption.IGNORE_CASE
                        )

                val match = pricePattern.find(content)
                return match?.value
        }

        private fun extractSharesOffered(content: String): String? {
                // "10,000,000 shares" 형식 찾기
                val sharesPattern =
                        Regex(
                                "(?i)([0-9,]+)\\s+shares\\s+of\\s+common\\s+stock",
                                RegexOption.IGNORE_CASE
                        )

                val match = sharesPattern.find(content)
                return match?.groupValues?.get(1)
        }

        override fun extractFinancialStatements(content: String): StructuredFinancialData? {
                return StructuredFinancialData(
                        companyName = null,
                        reportType = "S-1",
                        fiscalYear = "Multi-year",
                        fiscalPeriod = "Historical",
                        incomeStatement = null,
                        balanceSheet = null,
                        cashFlowStatement = null,
                        dataQuality = DataQuality.HIGH
                )
        }
}
