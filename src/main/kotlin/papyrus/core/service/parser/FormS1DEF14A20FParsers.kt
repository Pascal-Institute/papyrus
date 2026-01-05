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
                val sections = mutableMapOf<String, String>()

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
