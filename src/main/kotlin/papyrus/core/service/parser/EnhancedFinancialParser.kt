package papyrus.core.service.parser

import papyrus.core.model.*
import papyrus.util.*

/** 향상된 재무 분석 파서 */
object EnhancedFinancialParser {

        /**
         * SEC PDF 문서 전용 파싱 - PdfParser의 SecDocumentText 활용
         *
         * PDF에서 추출된 구조화된 문서 정보를 활용하여 더 정확한 파싱 수행
         */
        fun parseFromSecDocument(secDoc: SecDocumentText): List<ExtendedFinancialMetric> {
                val metrics = mutableListOf<ExtendedFinancialMetric>()

                // 각 섹션별로 특화된 파싱 수행
                for (section in secDoc.sections) {
                        val sectionMetrics =
                                when (section.type) {
                                        SecSectionType.INCOME_STATEMENT ->
                                                parseIncomeStatementSection(section.content)
                                        SecSectionType.BALANCE_SHEET ->
                                                parseBalanceSheetSection(section.content)
                                        SecSectionType.CASH_FLOW ->
                                                parseCashFlowSection(section.content)
                                        else -> emptyList()
                                }
                        metrics.addAll(sectionMetrics)
                }

                // 전체 텍스트에서 추가 메트릭 추출 (섹션에서 못 찾은 경우)
                val foundCategories = metrics.map { it.category }.toSet()
                val additionalMetrics =
                        parseFinancialMetrics(secDoc.fullText).filter {
                                it.category !in foundCategories
                        }

                metrics.addAll(additionalMetrics)

                // 회사명과 회계연도 정보 추가
                return metrics.map { metric ->
                        metric.copy(
                                context =
                                        "${secDoc.companyName} - ${secDoc.fiscalYear ?: ""} ${metric.context ?: ""}".trim()
                        )
                }
        }

        /**
         * PDF 텍스트 기반 테이블 파싱 - 열 정렬 인식
         *
         * PDF에서 추출된 텍스트의 공백 패턴을 분석하여 테이블 구조 인식
         */
        fun parsePdfTextTable(text: String): List<ExtendedFinancialMetric> {
                val metrics = mutableListOf<ExtendedFinancialMetric>()
                val lines = text.split("\n")

                // 단위 감지
                val unit = detectUnit(text)

                // 기간 헤더 찾기 (보통 숫자가 여러 개 있는 열 헤더)
                val yearPattern = Regex("""20\d{2}""")
                val headerLine =
                        lines.take(20).find { line -> yearPattern.findAll(line).count() >= 2 }
                val years =
                        headerLine?.let { yearPattern.findAll(it).map { m -> m.value }.toList() }
                                ?: emptyList()

                // 각 라인에서 레이블과 숫자 추출
                for (line in lines) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.length < 10) continue

                        // 숫자 패턴 (SEC 문서 형식: 괄호는 음수, $ 기호 옵션, 천 단위 쉼표)
                        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")
                        val numbers = numberPattern.findAll(trimmedLine).toList()

                        if (numbers.isEmpty()) continue

                        // 첫 번째 숫자 앞이 레이블
                        val firstNumberStart = numbers.first().range.first
                        val label = trimmedLine.substring(0, firstNumberStart).trim()

                        // 레이블 유효성 검사
                        if (!isValidLabel(label)) continue

                        // 가장 최근 값 (첫 번째 숫자)
                        val currentValueStr = numbers.first().value
                        val currentValue = parseSecValue(currentValueStr, unit)

                        // 이전 기간 값 (두 번째 숫자, 있으면)
                        val priorValue =
                                if (numbers.size >= 2) {
                                        parseSecValue(numbers[1].value, unit)
                                } else null

                        if (currentValue == null) continue

                        // 카테고리 추론
                        val category = inferCategoryFromLabel(label) ?: continue

                        // YoY 변화율 계산 (BigDecimal for precision)
                        val yoyChange =
                                if (priorValue != null && priorValue != java.math.BigDecimal.ZERO) {
                                        currentValue
                                                .subtract(priorValue)
                                                .divide(
                                                        priorValue.abs(),
                                                        10,
                                                        java.math.RoundingMode.HALF_UP
                                                )
                                                .multiply(java.math.BigDecimal("100"))
                                                .setScale(2, java.math.RoundingMode.HALF_UP)
                                } else null

                        metrics.add(
                                ExtendedFinancialMetric(
                                        name = label,
                                        value = formatValue(currentValue),
                                        rawValue =
                                                currentValue
                                                        .setScale(2, java.math.RoundingMode.HALF_UP)
                                                        .toString(),
                                        unit = unit,
                                        period = years.firstOrNull(),
                                        category = category,
                                        source = "PDF Text Table Parser",
                                        confidence =
                                                if (label.lowercase().startsWith("total")) 0.95
                                                else 0.85,
                                        yearOverYearChange = yoyChange?.toString()
                                )
                        )
                }

                return deduplicateMetrics(metrics)
        }

        private fun isValidLabel(label: String): Boolean {
                if (label.length < 3) return false
                if (label.all {
                                it.isDigit() ||
                                        it.isWhitespace() ||
                                        it == ',' ||
                                        it == '.' ||
                                        it == '$'
                        }
                )
                        return false
                if (label.contains("Page ") || label.matches(Regex("""F-\d+.*"""))) return false
                if (label.matches(Regex("""^\d+.*"""))) return false // 숫자로 시작하면 스킵
                if (label.contains("---")) return false // 구분선
                return true
        }

        private fun parseSecValue(valueStr: String, unit: MetricUnit): java.math.BigDecimal? {
                val unitStr =
                        when (unit) {
                                MetricUnit.BILLIONS -> "billions"
                                MetricUnit.MILLIONS -> "millions"
                                MetricUnit.THOUSANDS -> "thousands"
                                else -> "dollars"
                        }

                val monetaryAmount =
                        papyrus.util.FinancialPrecision.parseSecValue(valueStr, unitStr, "USD")
                return monetaryAmount?.number?.numberValue(java.math.BigDecimal::class.java)
        }

        private fun inferCategoryFromLabel(label: String): MetricCategory? {
                val lowerLabel = label.lowercase().trim()

                // 더 유연한 매칭을 위한 패턴
                return when {
                        // 수익 (다양한 표현)
                        lowerLabel.matches(Regex(".*total.*(?:revenue|sales).*")) ->
                                MetricCategory.REVENUE
                        lowerLabel.matches(Regex(".*net.*(?:revenue|sales).*")) ->
                                MetricCategory.REVENUE
                        lowerLabel == "revenue" || lowerLabel == "revenues" ->
                                MetricCategory.REVENUE
                        lowerLabel == "net sales" || lowerLabel == "total net sales" ->
                                MetricCategory.REVENUE
                        lowerLabel.contains("products") && lowerLabel.contains("net sales") ->
                                MetricCategory.PRODUCT_REVENUE
                        lowerLabel.contains("services") &&
                                (lowerLabel.contains("revenue") || lowerLabel.contains("sales")) ->
                                MetricCategory.SERVICE_REVENUE

                        // 매출원가
                        lowerLabel.matches(Regex(".*cost.*(?:revenue|sales|goods).*")) ->
                                MetricCategory.COST_OF_REVENUE
                        lowerLabel == "cogs" -> MetricCategory.COST_OF_REVENUE

                        // 이익
                        lowerLabel == "gross profit" || lowerLabel == "gross margin" ->
                                MetricCategory.GROSS_PROFIT
                        lowerLabel.contains("operating income") -> MetricCategory.OPERATING_INCOME
                        lowerLabel.contains("income from operations") ->
                                MetricCategory.OPERATING_INCOME
                        lowerLabel == "operating profit" -> MetricCategory.OPERATING_INCOME
                        lowerLabel.matches(Regex(".*net income.*")) -> MetricCategory.NET_INCOME
                        lowerLabel == "net earnings" || lowerLabel == "net profit" ->
                                MetricCategory.NET_INCOME
                        lowerLabel.contains("net loss") -> MetricCategory.NET_INCOME
                        lowerLabel.contains("ebitda") -> MetricCategory.EBITDA

                        // 자산
                        lowerLabel == "total assets" -> MetricCategory.TOTAL_ASSETS
                        lowerLabel.matches(Regex(".*total.*current.*assets.*")) ->
                                MetricCategory.CURRENT_ASSETS
                        lowerLabel.matches(Regex(".*current.*assets.*total.*")) ->
                                MetricCategory.CURRENT_ASSETS
                        lowerLabel.contains("cash and cash equivalents") ->
                                MetricCategory.CASH_AND_EQUIVALENTS
                        lowerLabel == "cash" -> MetricCategory.CASH_AND_EQUIVALENTS
                        lowerLabel.contains("accounts receivable") ->
                                MetricCategory.ACCOUNTS_RECEIVABLE
                        lowerLabel.contains("inventories") || lowerLabel == "inventory" ->
                                MetricCategory.INVENTORY
                        lowerLabel.contains("marketable securities") ->
                                MetricCategory.MARKETABLE_SECURITIES
                        lowerLabel.contains("property") && lowerLabel.contains("equipment") ->
                                MetricCategory.FIXED_ASSETS

                        // 부채
                        lowerLabel == "total liabilities" -> MetricCategory.TOTAL_LIABILITIES
                        lowerLabel.matches(Regex(".*total.*current.*liabilities.*")) ->
                                MetricCategory.CURRENT_LIABILITIES
                        lowerLabel.contains("accounts payable") -> MetricCategory.ACCOUNTS_PAYABLE
                        lowerLabel.matches(Regex(".*long.*term.*debt.*")) ->
                                MetricCategory.LONG_TERM_DEBT
                        lowerLabel.contains("term debt") -> MetricCategory.LONG_TERM_DEBT
                        lowerLabel.contains("deferred revenue") -> MetricCategory.DEFERRED_REVENUE

                        // 자본
                        lowerLabel.matches(
                                Regex(".*total.*(?:equity|stockholders|shareholders).*")
                        ) -> MetricCategory.TOTAL_EQUITY
                        lowerLabel.matches(Regex(".*(?:stockholders|shareholders).*equity.*")) ->
                                MetricCategory.TOTAL_EQUITY
                        lowerLabel.contains("retained earnings") -> MetricCategory.RETAINED_EARNINGS
                        lowerLabel.contains("accumulated deficit") ->
                                MetricCategory.RETAINED_EARNINGS

                        // 현금흐름
                        lowerLabel.matches(
                                Regex(".*(?:net )?cash.*(?:provided|generated|used).*operating.*")
                        ) -> MetricCategory.OPERATING_CASH_FLOW
                        lowerLabel.matches(Regex(".*operating.*(?:cash flow|activities).*")) ->
                                MetricCategory.OPERATING_CASH_FLOW
                        lowerLabel.matches(
                                Regex(".*(?:net )?cash.*(?:provided|used).*investing.*")
                        ) -> MetricCategory.INVESTING_CASH_FLOW
                        lowerLabel.matches(
                                Regex(".*(?:net )?cash.*(?:provided|used).*financing.*")
                        ) -> MetricCategory.FINANCING_CASH_FLOW
                        lowerLabel.contains("capital expenditures") || lowerLabel == "capex" ->
                                MetricCategory.CAPITAL_EXPENDITURES
                        lowerLabel.contains("free cash flow") -> MetricCategory.FREE_CASH_FLOW

                        // 비용
                        lowerLabel.contains("research and development") ||
                                lowerLabel.contains("r&d") -> MetricCategory.RD_EXPENSE
                        lowerLabel.matches(Regex(".*selling.*(?:general|admin).*")) ->
                                MetricCategory.SGA_EXPENSE
                        lowerLabel.contains("sg&a") -> MetricCategory.SGA_EXPENSE
                        lowerLabel.contains("interest expense") -> MetricCategory.INTEREST_EXPENSE
                        lowerLabel.contains("depreciation") -> MetricCategory.DEPRECIATION
                        lowerLabel.contains("income tax") || lowerLabel.contains("provision for") ->
                                MetricCategory.INCOME_TAX

                        // EPS
                        lowerLabel.matches(Regex(".*basic.*(?:earnings|eps).*(?:share)?.*")) ->
                                MetricCategory.EPS_BASIC
                        lowerLabel.matches(Regex(".*diluted.*(?:earnings|eps).*(?:share)?.*")) ->
                                MetricCategory.EPS_DILUTED
                        lowerLabel == "earnings per share" || lowerLabel == "eps" ->
                                MetricCategory.EPS_BASIC

                        // 주식수
                        lowerLabel.contains("shares outstanding") ->
                                MetricCategory.SHARES_OUTSTANDING
                        lowerLabel.contains("weighted average shares") ->
                                MetricCategory.SHARES_OUTSTANDING
                        else -> null
                }
        }

        /** 손익계산서 섹션 전용 파싱 */
        private fun parseIncomeStatementSection(content: String): List<ExtendedFinancialMetric> {
                val metrics = parsePdfTextTable(content)
                val incomeCategories =
                        setOf(
                                MetricCategory.REVENUE,
                                MetricCategory.PRODUCT_REVENUE,
                                MetricCategory.SERVICE_REVENUE,
                                MetricCategory.COST_OF_REVENUE,
                                MetricCategory.GROSS_PROFIT,
                                MetricCategory.OPERATING_INCOME,
                                MetricCategory.NET_INCOME,
                                MetricCategory.EBITDA,
                                MetricCategory.RD_EXPENSE,
                                MetricCategory.SGA_EXPENSE,
                                MetricCategory.INTEREST_EXPENSE,
                                MetricCategory.INCOME_TAX,
                                MetricCategory.EPS_BASIC,
                                MetricCategory.EPS_DILUTED
                        )
                return metrics.filter { it.category in incomeCategories }
        }

        /** 재무상태표 섹션 전용 파싱 */
        private fun parseBalanceSheetSection(content: String): List<ExtendedFinancialMetric> {
                val metrics = parsePdfTextTable(content)
                val balanceCategories =
                        setOf(
                                MetricCategory.TOTAL_ASSETS,
                                MetricCategory.CURRENT_ASSETS,
                                MetricCategory.CASH_AND_EQUIVALENTS,
                                MetricCategory.ACCOUNTS_RECEIVABLE,
                                MetricCategory.INVENTORY,
                                MetricCategory.MARKETABLE_SECURITIES,
                                MetricCategory.FIXED_ASSETS,
                                MetricCategory.TOTAL_LIABILITIES,
                                MetricCategory.CURRENT_LIABILITIES,
                                MetricCategory.ACCOUNTS_PAYABLE,
                                MetricCategory.LONG_TERM_DEBT,
                                MetricCategory.DEFERRED_REVENUE,
                                MetricCategory.TOTAL_EQUITY,
                                MetricCategory.RETAINED_EARNINGS
                        )
                return metrics.filter { it.category in balanceCategories }
        }

        /** 현금흐름표 섹션 전용 파싱 */
        private fun parseCashFlowSection(content: String): List<ExtendedFinancialMetric> {
                val metrics = parsePdfTextTable(content)
                val cashFlowCategories =
                        setOf(
                                MetricCategory.OPERATING_CASH_FLOW,
                                MetricCategory.INVESTING_CASH_FLOW,
                                MetricCategory.FINANCING_CASH_FLOW,
                                MetricCategory.FREE_CASH_FLOW,
                                MetricCategory.CAPITAL_EXPENDITURES
                        )
                return metrics.filter { it.category in cashFlowCategories }
        }

        /**
         * 세그먼트 정보 추출 (Segment Revenue Analysis)
         *
         * SEC 보고서에서 지역별/제품별 매출 세그먼트 정보를 추출합니다. AGENTS.md 원칙 5: 실제 SEC 보고서 샘플에서 세그먼트 구조 파악
         */
        fun parseSegmentInformation(content: String): List<SegmentRevenue> {
                val segments = mutableListOf<SegmentRevenue>()
                val lines = content.split("\n")

                // 세그먼트 섹션 탐지 패턴
                val segmentSectionPatterns =
                        listOf(
                                Regex(
                                        """(geographic|segment|regional)\s+information""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex(
                                        """revenue\s+by\s+(segment|region|geography|product)""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex("""segment\s+(revenue|results)""", RegexOption.IGNORE_CASE)
                        )

                // 지역 패턴
                val geographicPatterns =
                        listOf(
                                "Americas",
                                "United States",
                                "North America",
                                "EMEA",
                                "Europe",
                                "Asia Pacific",
                                "APAC",
                                "China",
                                "Japan",
                                "Other Countries",
                                "International",
                                "Domestic"
                        )

                // 제품/서비스 카테고리 패턴
                val productPatterns =
                        listOf(
                                "iPhone",
                                "Mac",
                                "iPad",
                                "Services",
                                "Wearables",
                                "Product",
                                "Software",
                                "Hardware",
                                "Subscription"
                        )

                var inSegmentSection = false
                val unit = detectUnit(content)

                for (i in lines.indices) {
                        val line = lines[i]

                        // 세그먼트 섹션 시작 감지
                        if (!inSegmentSection &&
                                        segmentSectionPatterns.any { it.containsMatchIn(line) }
                        ) {
                                inSegmentSection = true
                                continue
                        }

                        // 섹션 종료 감지 (다음 주요 섹션)
                        if (inSegmentSection &&
                                        (line.matches(Regex("""^[A-Z][\w\s]+(:|$)""")) &&
                                                !line.contains("revenue", ignoreCase = true))
                        ) {
                                inSegmentSection = false
                                continue
                        }

                        if (inSegmentSection) {
                                // 지역별 매출 파싱
                                for (region in geographicPatterns) {
                                        if (line.contains(region, ignoreCase = true)) {
                                                val numberPattern =
                                                        Regex("""\$?\s*([\d,]+(?:\.\d+)?)""")
                                                val matches = numberPattern.findAll(line).toList()

                                                if (matches.isNotEmpty()) {
                                                        val valueStr =
                                                                matches.first().groupValues[1]
                                                        val value = parseSecValue(valueStr, unit)

                                                        if (value != null &&
                                                                        value >
                                                                                java.math
                                                                                        .BigDecimal(
                                                                                                "1000"
                                                                                        )
                                                        ) { // 최소 임계값
                                                                segments.add(
                                                                        SegmentRevenue(
                                                                                segmentName =
                                                                                        region,
                                                                                segmentType =
                                                                                        SegmentType
                                                                                                .GEOGRAPHIC,
                                                                                revenue =
                                                                                        value.toDouble(),
                                                                                percentOfTotal =
                                                                                        null,
                                                                                source =
                                                                                        "Line ${i + 1}"
                                                                        )
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // 제품별 매출 파싱
                                for (product in productPatterns) {
                                        if (line.contains(product, ignoreCase = true) &&
                                                        !line.contains("total", ignoreCase = true)
                                        ) {
                                                val numberPattern =
                                                        Regex("""\$?\s*([\d,]+(?:\.\d+)?)""")
                                                val matches = numberPattern.findAll(line).toList()

                                                if (matches.isNotEmpty()) {
                                                        val valueStr =
                                                                matches.first().groupValues[1]
                                                        val value = parseSecValue(valueStr, unit)

                                                        if (value != null &&
                                                                        value >
                                                                                java.math
                                                                                        .BigDecimal(
                                                                                                "1000"
                                                                                        )
                                                        ) {
                                                                segments.add(
                                                                        SegmentRevenue(
                                                                                segmentName =
                                                                                        product,
                                                                                segmentType =
                                                                                        SegmentType
                                                                                                .PRODUCT,
                                                                                revenue =
                                                                                        value.toDouble(),
                                                                                percentOfTotal =
                                                                                        null,
                                                                                source =
                                                                                        "Line ${i + 1}"
                                                                        )
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }

                // 총 매출 대비 비율 계산
                val totalRevenue = segments.sumOf { it.revenue }
                if (totalRevenue > 0) {
                        return segments.map { segment ->
                                segment.copy(
                                        percentOfTotal = (segment.revenue / totalRevenue * 100)
                                )
                        }
                }

                return segments
        }

        /**
         * MD&A (Management Discussion & Analysis) 섹션 파싱
         *
         * 경영진의 재무 상황 논의 및 분석 내용을 추출합니다.
         */
        fun parseMDASection(content: String): ManagementDiscussion? {
                val lines = content.split("\n")

                // MD&A 섹션 시작 패턴
                val mdaStartPatterns =
                        listOf(
                                Regex(
                                        """management.?s\s+discussion\s+and\s+analysis""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex("""md\&a""", RegexOption.IGNORE_CASE),
                                Regex(
                                        """item\s+[27].*financial\s+condition""",
                                        RegexOption.IGNORE_CASE
                                )
                        )

                var mdaStartIndex = -1
                var mdaEndIndex = -1

                // MD&A 섹션 찾기
                for (i in lines.indices) {
                        if (mdaStartIndex == -1 &&
                                        mdaStartPatterns.any { it.containsMatchIn(lines[i]) }
                        ) {
                                mdaStartIndex = i
                        }

                        if (mdaStartIndex != -1 && mdaEndIndex == -1) {
                                // 다음 주요 섹션까지 (예: "Quantitative and Qualitative", "Financial
                                // Statements")
                                if (lines[i].matches(
                                                Regex(
                                                        """^(quantitative|financial statements|item\s+[38])""",
                                                        RegexOption.IGNORE_CASE
                                                )
                                        )
                                ) {
                                        mdaEndIndex = i
                                        break
                                }
                        }
                }

                if (mdaStartIndex == -1) return null
                if (mdaEndIndex == -1)
                        mdaEndIndex = minOf(mdaStartIndex + 500, lines.size) // 최대 500줄

                val mdaText = lines.subList(mdaStartIndex, mdaEndIndex).joinToString("\n")

                // 핵심 비즈니스 동인 추출
                val keyDrivers = extractKeyBusinessDrivers(mdaText)

                // 시장 상황 추출
                val marketConditions = extractMarketConditions(mdaText)

                // 향후 전망 추출
                val futureOutlook = extractFutureOutlook(mdaText)

                return ManagementDiscussion(
                        keyBusinessDrivers = keyDrivers,
                        marketConditions = marketConditions,
                        futureOutlook = futureOutlook,
                        criticalAccountingPolicies = emptyList() // TODO: 추후 구현
                )
        }

        private fun extractKeyBusinessDrivers(text: String): List<String> {
                val drivers = mutableListOf<String>()
                val driverPatterns =
                        listOf(
                                Regex(
                                        """(revenue|sales)\s+(increased|decreased|grew|declined)\s+by\s+([\d.]+%)""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex("""driven\s+by\s+([^.]{10,100})""", RegexOption.IGNORE_CASE),
                                Regex(
                                        """primarily\s+(due to|attributable to|driven by)\s+([^.]{10,100})""",
                                        RegexOption.IGNORE_CASE
                                )
                        )

                for (pattern in driverPatterns) {
                        val matches = pattern.findAll(text).take(5) // 최대 5개
                        for (match in matches) {
                                drivers.add(match.value.trim())
                        }
                }

                return drivers.distinct()
        }

        private fun extractMarketConditions(text: String): String {
                val conditionPatterns =
                        listOf(
                                Regex(
                                        """market\s+conditions\s*:?\s*([^.]{20,200})\.?""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex(
                                        """economic\s+(environment|conditions)\s*:?\s*([^.]{20,200})\.?""",
                                        RegexOption.IGNORE_CASE
                                )
                        )

                for (pattern in conditionPatterns) {
                        val match = pattern.find(text)
                        if (match != null) {
                                return match.groupValues.getOrNull(1) ?: match.value
                        }
                }

                return "Market conditions not explicitly stated"
        }

        private fun extractFutureOutlook(text: String): String {
                val outlookPatterns =
                        listOf(
                                Regex(
                                        """(outlook|expect|anticipate|forecast)\s+([^.]{20,200})\.?""",
                                        RegexOption.IGNORE_CASE
                                ),
                                Regex(
                                        """looking\s+forward\s*,?\s*([^.]{20,200})\.?""",
                                        RegexOption.IGNORE_CASE
                                )
                        )

                for (pattern in outlookPatterns) {
                        val match = pattern.find(text)
                        if (match != null) {
                                return match.groupValues.getOrNull(1) ?: match.value
                        }
                }

                return "Future outlook not explicitly stated"
        }

        /**
         * 데이터 출처 위치 추출 (Source Location Tracking)
         *
         * AGENTS.md 원칙 4: 추적 가능성 확보 재무 데이터가 문서의 어디서 왔는지 추적합니다.
         */
        fun extractSourceLocation(content: String, searchTerm: String): String? {
                val lines = content.split("\n")

                for (i in lines.indices) {
                        if (lines[i].contains(searchTerm, ignoreCase = true)) {
                                // 페이지 번호 찾기 (이전 라인들에서)
                                var pageNumber: Int? = null
                                for (j in maxOf(0, i - 20) until i) {
                                        val pageMatch =
                                                Regex("""page\s+(\d+)""", RegexOption.IGNORE_CASE)
                                                        .find(lines[j])
                                        if (pageMatch != null) {
                                                pageNumber = pageMatch.groupValues[1].toIntOrNull()
                                        }
                                }

                                // 테이블 번호 찾기
                                var tableNumber: Int? = null
                                for (j in maxOf(0, i - 10) until i) {
                                        val tableMatch =
                                                Regex("""table\s+(\d+)""", RegexOption.IGNORE_CASE)
                                                        .find(lines[j])
                                        if (tableMatch != null) {
                                                tableNumber =
                                                        tableMatch.groupValues[1].toIntOrNull()
                                        }
                                }

                                // 출처 문자열 생성
                                val parts = mutableListOf<String>()
                                if (pageNumber != null) parts.add("Page $pageNumber")
                                if (tableNumber != null) parts.add("Table $tableNumber")
                                parts.add("Line ${i + 1}")

                                return parts.joinToString(", ")
                        }
                }

                return null
        }

        // ===== 수익 관련 패턴 =====
        private val revenuePatterns =
                listOf(
                        PatternDef("Total Revenue", MetricCategory.REVENUE, 1.0),
                        PatternDef("Total Revenues", MetricCategory.REVENUE, 1.0),
                        PatternDef("Net Revenue", MetricCategory.REVENUE, 0.95),
                        PatternDef("Net Revenues", MetricCategory.REVENUE, 0.95),
                        PatternDef("Revenue", MetricCategory.REVENUE, 0.8),
                        PatternDef("Revenues", MetricCategory.REVENUE, 0.8),
                        PatternDef("Net Sales", MetricCategory.REVENUE, 0.9),
                        PatternDef("Total Net Sales", MetricCategory.REVENUE, 0.95),
                        PatternDef("Sales", MetricCategory.REVENUE, 0.7),
                        PatternDef("Total Sales", MetricCategory.REVENUE, 0.9)
                )

        // ===== 비용 관련 패턴 =====
        private val costPatterns =
                listOf(
                        PatternDef("Cost of Revenue", MetricCategory.COST_OF_REVENUE, 1.0),
                        PatternDef("Cost of Revenues", MetricCategory.COST_OF_REVENUE, 1.0),
                        PatternDef("Cost of Sales", MetricCategory.COST_OF_REVENUE, 0.95),
                        PatternDef("Cost of Goods Sold", MetricCategory.COST_OF_REVENUE, 0.95),
                        PatternDef("COGS", MetricCategory.COST_OF_REVENUE, 0.9)
                )

        // ===== 이익 관련 패턴 =====
        private val profitPatterns =
                listOf(
                        PatternDef("Gross Profit", MetricCategory.GROSS_PROFIT, 1.0),
                        PatternDef("Gross Margin", MetricCategory.GROSS_PROFIT, 0.9),
                        PatternDef("Operating Income", MetricCategory.OPERATING_INCOME, 1.0),
                        PatternDef("Operating Profit", MetricCategory.OPERATING_INCOME, 0.95),
                        PatternDef("Income from Operations", MetricCategory.OPERATING_INCOME, 0.95),
                        PatternDef("Net Income", MetricCategory.NET_INCOME, 1.0),
                        PatternDef("Net Earnings", MetricCategory.NET_INCOME, 0.95),
                        PatternDef("Net Profit", MetricCategory.NET_INCOME, 0.95),
                        PatternDef("Net Loss", MetricCategory.NET_INCOME, 0.9),
                        PatternDef("Net Income (Loss)", MetricCategory.NET_INCOME, 1.0),
                        PatternDef("EBITDA", MetricCategory.EBITDA, 1.0),
                        PatternDef("Adjusted EBITDA", MetricCategory.EBITDA, 0.95)
                )

        // ===== 자산 관련 패턴 =====
        private val assetPatterns =
                listOf(
                        PatternDef("Total Assets", MetricCategory.TOTAL_ASSETS, 1.0),
                        PatternDef("Total Current Assets", MetricCategory.CURRENT_ASSETS, 1.0),
                        PatternDef("Current Assets", MetricCategory.CURRENT_ASSETS, 0.95),
                        PatternDef(
                                "Cash and Cash Equivalents",
                                MetricCategory.CASH_AND_EQUIVALENTS,
                                1.0
                        ),
                        PatternDef(
                                "Cash and Equivalents",
                                MetricCategory.CASH_AND_EQUIVALENTS,
                                0.95
                        ),
                        PatternDef("Cash", MetricCategory.CASH_AND_EQUIVALENTS, 0.7),
                        PatternDef("Accounts Receivable", MetricCategory.ACCOUNTS_RECEIVABLE, 1.0),
                        PatternDef("Trade Receivables", MetricCategory.ACCOUNTS_RECEIVABLE, 0.95),
                        PatternDef("Inventory", MetricCategory.INVENTORY, 0.9),
                        PatternDef("Inventories", MetricCategory.INVENTORY, 1.0),
                        PatternDef("Total Inventory", MetricCategory.INVENTORY, 1.0)
                )

        // ===== 부채 관련 패턴 =====
        private val liabilityPatterns =
                listOf(
                        PatternDef("Total Liabilities", MetricCategory.TOTAL_LIABILITIES, 1.0),
                        PatternDef(
                                "Total Current Liabilities",
                                MetricCategory.CURRENT_LIABILITIES,
                                1.0
                        ),
                        PatternDef("Current Liabilities", MetricCategory.CURRENT_LIABILITIES, 0.95),
                        PatternDef("Long-term Debt", MetricCategory.LONG_TERM_DEBT, 1.0),
                        PatternDef("Long Term Debt", MetricCategory.LONG_TERM_DEBT, 1.0),
                        PatternDef("Total Long-term Debt", MetricCategory.LONG_TERM_DEBT, 1.0),
                        PatternDef("Total Debt", MetricCategory.LONG_TERM_DEBT, 0.9)
                )

        // ===== 자본 관련 패턴 =====
        private val equityPatterns =
                listOf(
                        PatternDef("Total Equity", MetricCategory.TOTAL_EQUITY, 1.0),
                        PatternDef("Total Stockholders' Equity", MetricCategory.TOTAL_EQUITY, 1.0),
                        PatternDef("Total Shareholders' Equity", MetricCategory.TOTAL_EQUITY, 1.0),
                        PatternDef("Stockholders' Equity", MetricCategory.TOTAL_EQUITY, 0.95),
                        PatternDef("Shareholders' Equity", MetricCategory.TOTAL_EQUITY, 0.95),
                        PatternDef("Retained Earnings", MetricCategory.RETAINED_EARNINGS, 1.0),
                        PatternDef("Accumulated Deficit", MetricCategory.RETAINED_EARNINGS, 0.9)
                )

        // ===== 현금흐름 관련 패턴 =====
        private val cashFlowPatterns =
                listOf(
                        PatternDef("Operating Cash Flow", MetricCategory.OPERATING_CASH_FLOW, 1.0),
                        PatternDef(
                                "Cash from Operations",
                                MetricCategory.OPERATING_CASH_FLOW,
                                0.95
                        ),
                        PatternDef(
                                "Net Cash from Operating",
                                MetricCategory.OPERATING_CASH_FLOW,
                                0.95
                        ),
                        PatternDef(
                                "Net Cash Provided by Operating",
                                MetricCategory.OPERATING_CASH_FLOW,
                                1.0
                        ),
                        PatternDef("Investing Cash Flow", MetricCategory.INVESTING_CASH_FLOW, 1.0),
                        PatternDef("Cash from Investing", MetricCategory.INVESTING_CASH_FLOW, 0.95),
                        PatternDef(
                                "Net Cash from Investing",
                                MetricCategory.INVESTING_CASH_FLOW,
                                0.95
                        ),
                        PatternDef("Financing Cash Flow", MetricCategory.FINANCING_CASH_FLOW, 1.0),
                        PatternDef("Cash from Financing", MetricCategory.FINANCING_CASH_FLOW, 0.95),
                        PatternDef(
                                "Net Cash from Financing",
                                MetricCategory.FINANCING_CASH_FLOW,
                                0.95
                        ),
                        PatternDef("Free Cash Flow", MetricCategory.FREE_CASH_FLOW, 1.0),
                        PatternDef(
                                "Capital Expenditures",
                                MetricCategory.CAPITAL_EXPENDITURES,
                                1.0
                        ),
                        PatternDef("CapEx", MetricCategory.CAPITAL_EXPENDITURES, 0.9)
                )

        // ===== 비용 관련 패턴 (추가) =====
        private val expensePatterns =
                listOf(
                        PatternDef("Interest Expense", MetricCategory.INTEREST_EXPENSE, 1.0),
                        PatternDef("Interest Costs", MetricCategory.INTEREST_EXPENSE, 0.95),
                        PatternDef("Interest Paid", MetricCategory.INTEREST_EXPENSE, 0.9),
                        PatternDef("R&D Expense", MetricCategory.RD_EXPENSE, 1.0),
                        PatternDef("Research and Development", MetricCategory.RD_EXPENSE, 1.0),
                        PatternDef("SG&A Expense", MetricCategory.SGA_EXPENSE, 1.0),
                        PatternDef(
                                "Selling, General and Administrative",
                                MetricCategory.SGA_EXPENSE,
                                0.95
                        )
                )

        // ===== 주당 지표 패턴 =====
        private val perSharePatterns =
                listOf(
                        PatternDef("Basic Earnings Per Share", MetricCategory.EPS_BASIC, 1.0),
                        PatternDef("Basic EPS", MetricCategory.EPS_BASIC, 0.95),
                        PatternDef("Diluted Earnings Per Share", MetricCategory.EPS_DILUTED, 1.0),
                        PatternDef("Diluted EPS", MetricCategory.EPS_DILUTED, 0.95),
                        PatternDef("Earnings Per Share", MetricCategory.EPS_BASIC, 0.8),
                        PatternDef("EPS", MetricCategory.EPS_BASIC, 0.7),
                        PatternDef(
                                "Book Value Per Share",
                                MetricCategory.BOOK_VALUE_PER_SHARE,
                                1.0
                        ),
                        PatternDef("Dividends Per Share", MetricCategory.DIVIDENDS_PER_SHARE, 1.0)
                )

        // ===== 주식수 관련 패턴 =====
        private val sharesPatterns =
                listOf(
                        PatternDef("Shares Outstanding", MetricCategory.SHARES_OUTSTANDING, 1.0),
                        PatternDef(
                                "Common Shares Outstanding",
                                MetricCategory.SHARES_OUTSTANDING,
                                1.0
                        ),
                        PatternDef(
                                "Basic Shares Outstanding",
                                MetricCategory.SHARES_OUTSTANDING,
                                0.95
                        ),
                        PatternDef(
                                "Diluted Shares Outstanding",
                                MetricCategory.SHARES_DILUTED,
                                1.0
                        ),
                        PatternDef(
                                "Weighted Average Shares",
                                MetricCategory.SHARES_OUTSTANDING,
                                0.9
                        )
                )

        // ===== 세부 자산 항목 패턴 =====
        private val detailedAssetPatterns =
                listOf(
                        PatternDef(
                                "Marketable Securities",
                                MetricCategory.MARKETABLE_SECURITIES,
                                1.0
                        ),
                        PatternDef(
                                "Short-term Investments",
                                MetricCategory.MARKETABLE_SECURITIES,
                                0.95
                        ),
                        PatternDef(
                                "Short-term Marketable Securities",
                                MetricCategory.MARKETABLE_SECURITIES,
                                0.95
                        ),
                        PatternDef(
                                "Long-term Marketable Securities",
                                MetricCategory.LONG_TERM_INVESTMENTS,
                                1.0
                        ),
                        PatternDef(
                                "Long-term Investments",
                                MetricCategory.LONG_TERM_INVESTMENTS,
                                0.95
                        ),
                        PatternDef("Prepaid Expenses", MetricCategory.PREPAID_EXPENSES, 1.0),
                        PatternDef(
                                "Prepaid Expenses and Other",
                                MetricCategory.PREPAID_EXPENSES,
                                0.9
                        ),
                        PatternDef(
                                "Other Current Assets",
                                MetricCategory.OTHER_CURRENT_ASSETS,
                                0.9
                        ),
                        PatternDef("Fixed Assets", MetricCategory.FIXED_ASSETS, 1.0),
                        PatternDef("Net Fixed Assets", MetricCategory.FIXED_ASSETS, 0.95),
                        PatternDef(
                                "Property, Plant and Equipment",
                                MetricCategory.FIXED_ASSETS,
                                1.0
                        ),
                        PatternDef("PP&E", MetricCategory.FIXED_ASSETS, 0.9),
                        PatternDef("Machinery and Equipment", MetricCategory.FIXED_ASSETS, 0.85),
                        PatternDef("Deferred Tax Assets", MetricCategory.DEFERRED_TAX_ASSETS, 1.0)
                )

        // ===== 재고 세부 항목 패턴 =====
        private val inventoryDetailPatterns =
                listOf(
                        PatternDef("Raw Materials", MetricCategory.RAW_MATERIALS, 1.0),
                        PatternDef("Work in Process", MetricCategory.WORK_IN_PROCESS, 1.0),
                        PatternDef("Work-in-Process", MetricCategory.WORK_IN_PROCESS, 1.0),
                        PatternDef("Finished Goods", MetricCategory.FINISHED_GOODS, 1.0),
                        PatternDef("Total Inventories", MetricCategory.INVENTORY, 1.0),
                        PatternDef("Inventories, Net", MetricCategory.INVENTORY, 0.95)
                )

        // ===== 세부 부채 항목 패턴 =====
        private val detailedLiabilityPatterns =
                listOf(
                        PatternDef("Accounts Payable", MetricCategory.ACCOUNTS_PAYABLE, 1.0),
                        PatternDef("Trade Payables", MetricCategory.ACCOUNTS_PAYABLE, 0.95),
                        PatternDef("Accrued Expenses", MetricCategory.ACCRUED_EXPENSES, 1.0),
                        PatternDef("Accrued Payroll", MetricCategory.ACCRUED_EXPENSES, 0.9),
                        PatternDef("Accrued Liabilities", MetricCategory.ACCRUED_EXPENSES, 0.9),
                        PatternDef("Operating Lease", MetricCategory.OPERATING_LEASE, 1.0),
                        PatternDef(
                                "Operating Lease Liability",
                                MetricCategory.OPERATING_LEASE,
                                1.0
                        ),
                        PatternDef(
                                "Long-term Operating Lease",
                                MetricCategory.LONG_TERM_LEASE,
                                1.0
                        ),
                        PatternDef("Deferred Revenue", MetricCategory.DEFERRED_REVENUE, 1.0),
                        PatternDef("Unearned Revenue", MetricCategory.DEFERRED_REVENUE, 0.9)
                )

        // ===== 세부 수익/비용 항목 패턴 =====
        private val detailedIncomePatterns =
                listOf(
                        PatternDef("Product Sales", MetricCategory.PRODUCT_REVENUE, 1.0),
                        PatternDef("Service Revenue", MetricCategory.SERVICE_REVENUE, 1.0),
                        PatternDef(
                                "Contract Research and Development",
                                MetricCategory.RD_REVENUE,
                                1.0
                        ),
                        PatternDef("Total Expenses", MetricCategory.TOTAL_EXPENSES, 1.0),
                        PatternDef("Total Operating Expenses", MetricCategory.TOTAL_EXPENSES, 0.95),
                        PatternDef("Income Before Taxes", MetricCategory.INCOME_BEFORE_TAX, 1.0),
                        PatternDef("Pretax Income", MetricCategory.INCOME_BEFORE_TAX, 0.95),
                        PatternDef("Provision for Income Taxes", MetricCategory.INCOME_TAX, 1.0),
                        PatternDef("Income Tax Expense", MetricCategory.INCOME_TAX, 0.95),
                        PatternDef("Interest Income", MetricCategory.INTEREST_INCOME, 1.0),
                        PatternDef(
                                "Interest and Other Income",
                                MetricCategory.INTEREST_INCOME,
                                0.9
                        ),
                        PatternDef("Other Income", MetricCategory.OTHER_INCOME, 0.85),
                        PatternDef("Depreciation", MetricCategory.DEPRECIATION, 1.0),
                        PatternDef(
                                "Depreciation and Amortization",
                                MetricCategory.DEPRECIATION,
                                1.0
                        ),
                        PatternDef("Amortization", MetricCategory.AMORTIZATION, 1.0)
                )

        // ===== 현금흐름 세부 항목 패턴 =====
        private val detailedCashFlowPatterns =
                listOf(
                        PatternDef(
                                "Purchases of Fixed Assets",
                                MetricCategory.CAPITAL_EXPENDITURES,
                                1.0
                        ),
                        PatternDef(
                                "Purchases of Marketable Securities",
                                MetricCategory.INVESTMENT_PURCHASES,
                                1.0
                        ),
                        PatternDef(
                                "Proceeds from Maturities",
                                MetricCategory.INVESTMENT_PROCEEDS,
                                1.0
                        ),
                        PatternDef(
                                "Proceeds from Sale of",
                                MetricCategory.INVESTMENT_PROCEEDS,
                                0.85
                        ),
                        PatternDef("Payment of Dividends", MetricCategory.DIVIDENDS_PAID, 1.0),
                        PatternDef("Cash Dividends Paid", MetricCategory.DIVIDENDS_PAID, 1.0),
                        PatternDef(
                                "Stock-based Compensation",
                                MetricCategory.STOCK_COMPENSATION,
                                1.0
                        ),
                        PatternDef(
                                "Share-based Compensation",
                                MetricCategory.STOCK_COMPENSATION,
                                1.0
                        ),
                        PatternDef(
                                "Changes in Operating Assets",
                                MetricCategory.WORKING_CAPITAL_CHANGES,
                                0.8
                        )
                )

        // 모든 패턴 합치기
        private val allPatterns =
                revenuePatterns +
                        costPatterns +
                        profitPatterns +
                        assetPatterns +
                        liabilityPatterns +
                        equityPatterns +
                        cashFlowPatterns +
                        expensePatterns +
                        perSharePatterns +
                        sharesPatterns +
                        detailedAssetPatterns +
                        inventoryDetailPatterns +
                        detailedLiabilityPatterns +
                        detailedIncomePatterns +
                        detailedCashFlowPatterns

        /**
         * 문서에서 모든 재무 지표 추출 (향상된 버전)
         *
         * 1. 먼저 SecTableParser로 테이블 기반 파싱 시도
         * 2. 테이블 파싱이 부족하면 텍스트 패턴 파싱으로 보완
         * 3. 두 결과를 병합하여 최적의 결과 반환
         */
        fun parseFinancialMetrics(content: String): List<ExtendedFinancialMetric> {
                val metrics = mutableListOf<ExtendedFinancialMetric>()

                // 1단계: 테이블 기반 파싱 (가장 정확함)
                try {
                        val tables = SecTableParser.parseFinancialTables(content)
                        val tableMetrics = SecTableParser.convertToMetrics(tables)
                        metrics.addAll(tableMetrics)
                        println(
                                "✓ Table parsing: Found ${tableMetrics.size} metrics from ${tables.size} tables"
                        )
                } catch (e: Exception) {
                        println("⚠ Table parsing failed: ${e.message}")
                }

                // 2단계: 텍스트 패턴 기반 파싱 (보완용)
                val cleanText = cleanHtml(content)

                // 금액 단위 감지 (thousands, millions, billions)
                val unit = detectUnit(cleanText)

                // 기간 감지
                val period = detectPeriod(cleanText)
                val periodType = detectPeriodType(cleanText)

                // 이미 테이블에서 찾은 카테고리는 제외
                val foundCategories = metrics.map { it.category }.toSet()

                for (pattern in allPatterns) {
                        // 이미 테이블에서 찾은 카테고리는 스킵
                        if (pattern.category in foundCategories) continue

                        val found =
                                searchMetricValues(
                                        cleanText,
                                        pattern.term,
                                        pattern.category,
                                        unit,
                                        period,
                                        periodType,
                                        pattern.confidence
                                )
                        metrics.addAll(found)
                }

                // 3단계: 중복 제거 및 가장 신뢰도 높은 것 선택
                val deduplicated = deduplicateMetrics(metrics)
                println("✓ Total metrics after deduplication: ${deduplicated.size}")
                return deduplicated
        }

        /** 재무제표 섹션 파싱 (향상된 버전) */
        fun parseFinancialStatements(content: String): List<FinancialStatement> {
                val statements = mutableListOf<FinancialStatement>()

                // 먼저 테이블 파서로 시도
                try {
                        val tables = SecTableParser.parseFinancialTables(content)
                        for (table in tables) {
                                val tableMetrics =
                                        table.rows.filter { it.category != null }.map { row ->
                                                ExtendedFinancialMetric(
                                                        name = row.label,
                                                        value = row.values.firstOrNull()?.toString()
                                                                        ?: "",
                                                        rawValue =
                                                                row.values
                                                                        .firstOrNull()
                                                                        ?.toString(),
                                                        category = row.category!!,
                                                        confidence = if (row.isTotal) 0.95 else 0.85
                                                )
                                        }

                                if (tableMetrics.isNotEmpty()) {
                                        statements.add(
                                                FinancialStatement(
                                                        type = table.statementType,
                                                        periodEnding = table.periods.firstOrNull(),
                                                        periodType = PeriodType.QUARTERLY,
                                                        metrics = tableMetrics,
                                                        rawSection = table.rawHtml.take(2000)
                                                )
                                        )
                                }
                        }

                        if (statements.isNotEmpty()) {
                                println(
                                        "✓ Found ${statements.size} financial statements via table parsing"
                                )
                                return statements
                        }
                } catch (e: Exception) {
                        println("⚠ Table statement parsing failed: ${e.message}")
                }

                // 폴백: 기존 텍스트 기반 파싱
                val cleanText = cleanHtml(content)

                // 손익계산서 찾기
                val incomeStatementSection =
                        extractSection(
                                cleanText,
                                listOf(
                                        "CONSOLIDATED STATEMENTS OF OPERATIONS",
                                        "CONSOLIDATED STATEMENTS OF INCOME",
                                        "STATEMENTS OF OPERATIONS",
                                        "INCOME STATEMENT"
                                )
                        )
                if (incomeStatementSection != null) {
                        val metrics = parseFinancialMetrics(incomeStatementSection)
                        val incomeMetrics =
                                metrics.filter {
                                        it.category in
                                                listOf(
                                                        MetricCategory.REVENUE,
                                                        MetricCategory.COST_OF_REVENUE,
                                                        MetricCategory.GROSS_PROFIT,
                                                        MetricCategory.OPERATING_INCOME,
                                                        MetricCategory.NET_INCOME,
                                                        MetricCategory.EBITDA
                                                )
                                }
                        if (incomeMetrics.isNotEmpty()) {
                                statements.add(
                                        FinancialStatement(
                                                type = StatementType.INCOME_STATEMENT,
                                                periodEnding = detectPeriod(incomeStatementSection),
                                                periodType =
                                                        detectPeriodType(incomeStatementSection),
                                                metrics = incomeMetrics,
                                                rawSection = incomeStatementSection.take(2000)
                                        )
                                )
                        }
                }

                // 재무상태표 찾기
                val balanceSheetSection =
                        extractSection(
                                cleanText,
                                listOf(
                                        "CONSOLIDATED BALANCE SHEETS",
                                        "BALANCE SHEET",
                                        "CONSOLIDATED BALANCE SHEET",
                                        "STATEMENT OF FINANCIAL POSITION"
                                )
                        )
                if (balanceSheetSection != null) {
                        val metrics = parseFinancialMetrics(balanceSheetSection)
                        val balanceMetrics =
                                metrics.filter {
                                        it.category in
                                                listOf(
                                                        MetricCategory.TOTAL_ASSETS,
                                                        MetricCategory.CURRENT_ASSETS,
                                                        MetricCategory.CASH_AND_EQUIVALENTS,
                                                        MetricCategory.INVENTORY,
                                                        MetricCategory.TOTAL_LIABILITIES,
                                                        MetricCategory.CURRENT_LIABILITIES,
                                                        MetricCategory.LONG_TERM_DEBT,
                                                        MetricCategory.TOTAL_EQUITY
                                                )
                                }
                        if (balanceMetrics.isNotEmpty()) {
                                statements.add(
                                        FinancialStatement(
                                                type = StatementType.BALANCE_SHEET,
                                                periodEnding = detectPeriod(balanceSheetSection),
                                                periodType =
                                                        PeriodType.QUARTERLY, // Balance sheet is
                                                // point-in-time
                                                metrics = balanceMetrics,
                                                rawSection = balanceSheetSection.take(2000)
                                        )
                                )
                        }
                }

                // 현금흐름표 찾기
                val cashFlowSection =
                        extractSection(
                                cleanText,
                                listOf(
                                        "CONSOLIDATED STATEMENTS OF CASH FLOWS",
                                        "STATEMENTS OF CASH FLOWS",
                                        "CASH FLOW STATEMENT"
                                )
                        )
                if (cashFlowSection != null) {
                        val metrics = parseFinancialMetrics(cashFlowSection)
                        val cashMetrics =
                                metrics.filter {
                                        it.category in
                                                listOf(
                                                        MetricCategory.OPERATING_CASH_FLOW,
                                                        MetricCategory.INVESTING_CASH_FLOW,
                                                        MetricCategory.FINANCING_CASH_FLOW,
                                                        MetricCategory.FREE_CASH_FLOW,
                                                        MetricCategory.CAPITAL_EXPENDITURES
                                                )
                                }
                        if (cashMetrics.isNotEmpty()) {
                                statements.add(
                                        FinancialStatement(
                                                type = StatementType.CASH_FLOW_STATEMENT,
                                                periodEnding = detectPeriod(cashFlowSection),
                                                periodType = detectPeriodType(cashFlowSection),
                                                metrics = cashMetrics,
                                                rawSection = cashFlowSection.take(2000)
                                        )
                                )
                        }
                }

                return statements
        }

        /** 위험 요소 파싱 */
        fun parseRiskFactors(content: String): List<RiskFactor> {
                val risks = mutableListOf<RiskFactor>()
                val cleanText = cleanHtml(content)

                // Risk Factors 섹션 찾기
                val riskSection = extractSection(cleanText, listOf("RISK FACTORS", "Item 1A"))
                if (riskSection == null) return risks

                // 위험 요소 항목 추출 (일반적으로 굵은 글씨나 특정 패턴으로 시작)
                val riskPatterns =
                        listOf(
                                Regex(
                                        "(?i)(?:^|\\n)\\s*([A-Z][^.\\n]{10,100})\\s*[-–—.]\\s*([^\\n]{50,500})"
                                ),
                                Regex("(?i)(?:^|\\n)\\s*•\\s*([^\\n]{20,200})")
                        )

                for (pattern in riskPatterns) {
                        val matches = pattern.findAll(riskSection)
                        for (match in matches.take(15)) {
                                val title = match.groupValues.getOrElse(1) { match.value }.trim()
                                val summary = match.groupValues.getOrElse(2) { "" }.trim()

                                val category = categorizeRisk(title + " " + summary)

                                risks.add(
                                        RiskFactor(
                                                title = title.take(100),
                                                summary = summary.take(300),
                                                category = category,
                                                severity = assessRiskSeverity(title + " " + summary)
                                        )
                                )
                        }
                }

                return risks.distinctBy { it.title }.take(10)
        }

        /** 재무 비율 계산 */
        fun calculateRatios(metrics: List<ExtendedFinancialMetric>): List<FinancialRatio> {
                val ratios = mutableListOf<FinancialRatio>()

                // 메트릭에서 값 추출하는 헬퍼
                fun getValue(category: MetricCategory): Double? {
                        return metrics
                                .find { it.category == category }
                                ?.getRawValueBigDecimal()
                                ?.toDouble()
                }
                val revenue = getValue(MetricCategory.REVENUE)
                val grossProfit = getValue(MetricCategory.GROSS_PROFIT)
                val operatingIncome = getValue(MetricCategory.OPERATING_INCOME)
                val netIncome = getValue(MetricCategory.NET_INCOME)
                val totalAssets = getValue(MetricCategory.TOTAL_ASSETS)
                val totalLiabilities = getValue(MetricCategory.TOTAL_LIABILITIES)
                val totalEquity = getValue(MetricCategory.TOTAL_EQUITY)
                val currentAssets = getValue(MetricCategory.CURRENT_ASSETS)
                val currentLiabilities = getValue(MetricCategory.CURRENT_LIABILITIES)
                val cash = getValue(MetricCategory.CASH_AND_EQUIVALENTS)
                val inventory = getValue(MetricCategory.INVENTORY)
                @Suppress("UNUSED_VARIABLE")
                val notUsedDebt =
                        getValue(
                                MetricCategory.LONG_TERM_DEBT
                        ) // Keep variable but unused or suppress

                // Validation: Check if values look reasonable
                // If gross profit > revenue, something is wrong with parsing
                if (grossProfit != null && revenue != null && grossProfit > revenue * 1.5) {
                        println(
                                "WARNING: Gross profit ($grossProfit) > revenue ($revenue) * 1.5 - may indicate parsing error"
                        )
                }

                // 1. 매출총이익률 (Gross Margin)
                if (grossProfit != null && revenue != null && revenue > 0) {
                        val ratio = (grossProfit / revenue) * 100

                        // Sanity check: if ratio is unreasonably high, skip it
                        if (ratio <= 100) { // Gross margin should typically be < 100%
                                ratios.add(
                                        createRatio(
                                                "매출총이익률",
                                                "Gross Margin",
                                                ratio,
                                                "%",
                                                "매출에서 매출원가를 제외한 이익의 비율",
                                                RatioCategory.PROFITABILITY,
                                                assessProfitabilityHealth(ratio, 30.0, 50.0)
                                        )
                                )
                        } else {
                                println(
                                        "WARNING: Gross Margin calculation resulted in $ratio% - skipping (likely parsing error)"
                                )
                        }
                }

                // 2. 영업이익률 (Operating Margin)
                if (operatingIncome != null && revenue != null && revenue > 0) {
                        val ratio = (operatingIncome / revenue) * 100

                        // Sanity check
                        if (ratio <= 100) { // Operating margin should typically be < 100%
                                ratios.add(
                                        createRatio(
                                                "영업이익률",
                                                "Operating Margin",
                                                ratio,
                                                "%",
                                                "영업활동으로 발생한 이익의 매출 대비 비율",
                                                RatioCategory.PROFITABILITY,
                                                assessProfitabilityHealth(ratio, 10.0, 20.0)
                                        )
                                )
                        } else {
                                println(
                                        "WARNING: Operating Margin calculation resulted in $ratio% - skipping (likely parsing error)"
                                )
                        }
                }

                // 3. 순이익률 (Net Profit Margin)
                if (netIncome != null && revenue != null && revenue > 0) {
                        val ratio = (netIncome / revenue) * 100

                        // Sanity check (allow negative for losses, but cap at reasonable positive
                        // values)
                        if (ratio <= 100) { // Net margin should typically be < 100%
                                ratios.add(
                                        createRatio(
                                                "순이익률",
                                                "Net Profit Margin",
                                                ratio,
                                                "%",
                                                "모든 비용을 제외한 순수익의 매출 대비 비율",
                                                RatioCategory.PROFITABILITY,
                                                assessProfitabilityHealth(ratio, 5.0, 15.0)
                                        )
                                )
                        } else {
                                println(
                                        "WARNING: Net Profit Margin calculation resulted in $ratio% - skipping (likely parsing error)"
                                )
                        }
                }

                // 4. ROA (Return on Assets)
                if (netIncome != null && totalAssets != null && totalAssets > 0) {
                        val ratio = (netIncome / totalAssets) * 100
                        ratios.add(
                                createRatio(
                                        "총자산이익률",
                                        "ROA",
                                        ratio,
                                        "%",
                                        "자산을 얼마나 효율적으로 활용하는지 측정",
                                        RatioCategory.PROFITABILITY,
                                        assessProfitabilityHealth(ratio, 2.0, 8.0)
                                )
                        )
                }

                // 5. ROE (Return on Equity)
                if (netIncome != null && totalEquity != null && totalEquity > 0) {
                        val ratio = (netIncome / totalEquity) * 100
                        ratios.add(
                                createRatio(
                                        "자기자본이익률",
                                        "ROE",
                                        ratio,
                                        "%",
                                        "주주 자본으로 얼마나 수익을 창출하는지 측정",
                                        RatioCategory.PROFITABILITY,
                                        assessProfitabilityHealth(ratio, 10.0, 20.0)
                                )
                        )
                }

                // 6. 유동비율 (Current Ratio)
                if (currentAssets != null && currentLiabilities != null && currentLiabilities > 0) {
                        val ratio = currentAssets / currentLiabilities
                        ratios.add(
                                createRatio(
                                        "유동비율",
                                        "Current Ratio",
                                        ratio,
                                        "배",
                                        "단기 부채 상환 능력 측정 (1 이상이면 양호)",
                                        RatioCategory.LIQUIDITY,
                                        assessLiquidityHealth(ratio, 1.0, 2.0)
                                )
                        )
                }

                // 7. 당좌비율 (Quick Ratio)
                if (currentAssets != null &&
                                inventory != null &&
                                currentLiabilities != null &&
                                currentLiabilities > 0
                ) {
                        val ratio = (currentAssets - inventory) / currentLiabilities
                        ratios.add(
                                createRatio(
                                        "당좌비율",
                                        "Quick Ratio",
                                        ratio,
                                        "배",
                                        "재고를 제외한 즉시 현금화 가능 자산의 비율",
                                        RatioCategory.LIQUIDITY,
                                        assessLiquidityHealth(ratio, 0.8, 1.5)
                                )
                        )
                }

                // 8. 부채비율 (Debt to Equity)
                if (totalLiabilities != null && totalEquity != null && totalEquity > 0) {
                        val ratio = (totalLiabilities / totalEquity) * 100
                        ratios.add(
                                createRatio(
                                        "부채비율",
                                        "Debt to Equity",
                                        ratio,
                                        "%",
                                        "자기자본 대비 총부채 비율 (낮을수록 안정적)",
                                        RatioCategory.SOLVENCY,
                                        assessDebtHealth(ratio, 100.0, 200.0)
                                )
                        )
                }

                // 9. 자기자본비율 (Equity Ratio)
                if (totalEquity != null && totalAssets != null && totalAssets > 0) {
                        val ratio = (totalEquity / totalAssets) * 100
                        ratios.add(
                                createRatio(
                                        "자기자본비율",
                                        "Equity Ratio",
                                        ratio,
                                        "%",
                                        "총자산 중 자기자본이 차지하는 비율",
                                        RatioCategory.SOLVENCY,
                                        assessProfitabilityHealth(ratio, 30.0, 50.0)
                                )
                        )
                }

                // 10. 현금비율 (Cash Ratio)
                if (cash != null && currentLiabilities != null && currentLiabilities > 0) {
                        val ratio = cash / currentLiabilities
                        ratios.add(
                                createRatio(
                                        "현금비율",
                                        "Cash Ratio",
                                        ratio,
                                        "배",
                                        "현금 및 현금성 자산으로 단기부채를 갚을 수 있는 비율",
                                        RatioCategory.LIQUIDITY,
                                        assessLiquidityHealth(ratio, 0.2, 0.5)
                                )
                        )
                }

                // 11. 자산회전율 (Asset Turnover)
                if (revenue != null && totalAssets != null && totalAssets > 0) {
                        val ratio = revenue / totalAssets
                        ratios.add(
                                createRatio(
                                        "총자산회전율",
                                        "Asset Turnover",
                                        ratio,
                                        "회",
                                        "자산을 활용한 매출 창출 효율성 (높을수록 좋음)",
                                        RatioCategory.EFFICIENCY,
                                        assessEfficiencyHealth(ratio, 0.5, 1.5)
                                )
                        )
                }

                // 12. 매출채권회전율 (Receivables Turnover)
                val receivables = getValue(MetricCategory.ACCOUNTS_RECEIVABLE)
                if (revenue != null && receivables != null && receivables > 0) {
                        val ratio = revenue / receivables
                        ratios.add(
                                createRatio(
                                        "매출채권회전율",
                                        "Receivables Turnover",
                                        ratio,
                                        "회",
                                        "매출채권을 현금으로 회수하는 속도 (높을수록 빠름)",
                                        RatioCategory.EFFICIENCY,
                                        assessEfficiencyHealth(ratio, 4.0, 8.0)
                                )
                        )
                }

                // 13. 재고자산회전율 (Inventory Turnover)
                val costOfRevenue = getValue(MetricCategory.COST_OF_REVENUE)
                if (costOfRevenue != null && inventory != null && inventory > 0) {
                        val ratio = costOfRevenue / inventory
                        ratios.add(
                                createRatio(
                                        "재고자산회전율",
                                        "Inventory Turnover",
                                        ratio,
                                        "회",
                                        "재고를 판매하는 속도 (높을수록 재고 관리가 효율적)",
                                        RatioCategory.EFFICIENCY,
                                        assessEfficiencyHealth(ratio, 3.0, 7.0)
                                )
                        )
                }

                // 14. 이자보상배율 (Interest Coverage)
                val ebit = operatingIncome
                val interestExpense = getValue(MetricCategory.INTEREST_EXPENSE)
                if (ebit != null && interestExpense != null && interestExpense > 0) {
                        val ratio = ebit / interestExpense
                        ratios.add(
                                createRatio(
                                        "이자보상배율",
                                        "Interest Coverage",
                                        ratio,
                                        "배",
                                        "영업이익으로 이자비용을 감당할 수 있는 능력 (높을수록 안전)",
                                        RatioCategory.SOLVENCY,
                                        assessInterestCoverageHealth(ratio)
                                )
                        )
                }

                // 15. 부채비율 (Debt Ratio)
                if (totalLiabilities != null && totalAssets != null && totalAssets > 0) {
                        val ratio = (totalLiabilities / totalAssets) * 100
                        ratios.add(
                                createRatio(
                                        "부채비율",
                                        "Debt Ratio",
                                        ratio,
                                        "%",
                                        "총자산 중 부채가 차지하는 비중 (50% 이하가 안정적)",
                                        RatioCategory.SOLVENCY,
                                        assessDebtRatioHealth(ratio)
                                )
                        )
                }

                // 16. 유보율 (Retained Earnings Ratio)
                val retainedEarnings = getValue(MetricCategory.RETAINED_EARNINGS)
                if (retainedEarnings != null && totalEquity != null && totalEquity > 0) {
                        val ratio = (retainedEarnings / totalEquity) * 100
                        ratios.add(
                                createRatio(
                                        "유보율",
                                        "Retained Earnings Ratio",
                                        ratio,
                                        "%",
                                        "자기자본 중 이익잉여금 비율 (기업의 내부 유보 수준)",
                                        RatioCategory.SOLVENCY,
                                        assessRetainedEarningsHealth(ratio)
                                )
                        )
                }

                // 17. EBITDA 마진 (EBITDA Margin)
                val ebitda = getValue(MetricCategory.EBITDA)
                if (ebitda != null && revenue != null && revenue > 0) {
                        val ratio = (ebitda / revenue) * 100
                        if (ratio <= 150) { // Sanity check
                                ratios.add(
                                        createRatio(
                                                "EBITDA 마진",
                                                "EBITDA Margin",
                                                ratio,
                                                "%",
                                                "감가상각 전 영업현금흐름 효율성 (20% 이상 우수)",
                                                RatioCategory.PROFITABILITY,
                                                assessProfitabilityHealth(ratio, 10.0, 20.0)
                                        )
                                )
                        }
                }

                // 18. 운전자본비율 (Working Capital Ratio)
                if (currentAssets != null &&
                                currentLiabilities != null &&
                                totalAssets != null &&
                                totalAssets > 0
                ) {
                        val workingCapital = currentAssets - currentLiabilities
                        val ratio = (workingCapital / totalAssets) * 100
                        ratios.add(
                                createRatio(
                                        "운전자본비율",
                                        "Working Capital Ratio",
                                        ratio,
                                        "%",
                                        "총자산 대비 운전자본 비율 (유동성 여력)",
                                        RatioCategory.LIQUIDITY,
                                        assessWorkingCapitalHealth(ratio)
                                )
                        )
                }

                return ratios
        }

        // ===== Helper Functions =====

        public fun cleanHtml(content: String): String {
                var cleaned = content

                // Remove script, style, and XBRL metadata blocks with their content
                cleaned =
                        cleaned.replace(
                                Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL),
                                " "
                        )
                cleaned =
                        cleaned.replace(
                                Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL),
                                " "
                        )
                cleaned =
                        cleaned.replace(
                                Regex("<head[^>]*>.*?</head>", RegexOption.DOT_MATCHES_ALL),
                                " "
                        )
                cleaned =
                        cleaned.replace(
                                Regex(
                                        "<ix:header[^>]*>.*?</ix:header>",
                                        RegexOption.DOT_MATCHES_ALL
                                ),
                                " "
                        )
                cleaned =
                        cleaned.replace(
                                Regex(
                                        "<xbrli:context[^>]*>.*?</xbrli:context>",
                                        RegexOption.DOT_MATCHES_ALL
                                ),
                                " "
                        )
                cleaned =
                        cleaned.replace(
                                Regex(
                                        "<xbrli:unit[^>]*>.*?</xbrli:unit>",
                                        RegexOption.DOT_MATCHES_ALL
                                ),
                                " "
                        )

                // Remove XBRL tags but preserve content (these wrap the actual visible numbers)
                cleaned = cleaned.replace(Regex("</?ix:[^>]*>", RegexOption.IGNORE_CASE), " ")
                cleaned = cleaned.replace(Regex("</?us-gaap:[^>]*>", RegexOption.IGNORE_CASE), " ")
                cleaned = cleaned.replace(Regex("</?dei:[^>]*>", RegexOption.IGNORE_CASE), " ")
                cleaned = cleaned.replace(Regex("</?xbrli:[^>]*>", RegexOption.IGNORE_CASE), " ")

                // Enhanced table structure preservation - keep column alignment
                cleaned = cleaned.replace(Regex("<tr[^>]*>", RegexOption.IGNORE_CASE), "\n| ")
                cleaned =
                        cleaned.replace(
                                Regex("<td[^>]*>|<th[^>]*>", RegexOption.IGNORE_CASE),
                                " | "
                        )
                cleaned = cleaned.replace(Regex("</td>|</th>", RegexOption.IGNORE_CASE), " ")
                cleaned = cleaned.replace(Regex("</tr>", RegexOption.IGNORE_CASE), " |\n")

                // Replace <br> with newlines
                cleaned = cleaned.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                cleaned = cleaned.replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
                cleaned = cleaned.replace(Regex("<div[^>]*>", RegexOption.IGNORE_CASE), "\n")

                // Remove all remaining HTML tags
                cleaned = cleaned.replace(Regex("<[^>]*>"), " ")

                // Decode HTML entities
                cleaned = SecTextNormalization.decodeBasicEntities(cleaned)
                cleaned = cleaned.replace(Regex("&#160;|&#xA0;"), " ")
                cleaned = cleaned.replace(Regex("&apos;|&#39;"), "'")
                cleaned = cleaned.replace(Regex("&#8211;|&#8212;|&mdash;|&ndash;"), "-")
                cleaned =
                        cleaned.replace(
                                Regex("&#\\d+;"),
                                " "
                        ) // Replace numeric entities with space
                cleaned =
                        cleaned.replace(
                                Regex("&[a-zA-Z]+;"),
                                " "
                        ) // Replace named entities with space

                // Normalize whitespace (but keep newlines)
                return SecTextNormalization.normalizeWhitespacePreserveNewlines(cleaned)
        }

        private fun detectUnit(text: String): MetricUnit {
                val lowerText = text.lowercase()
                return when {
                        lowerText.contains("in billions") ||
                                lowerText.contains("(in billions)") ||
                                lowerText.contains("billions of dollars") ||
                                lowerText.contains(", in billions,") -> MetricUnit.BILLIONS
                        lowerText.contains("in millions") ||
                                lowerText.contains("(in millions)") ||
                                lowerText.contains("$ in millions") ||
                                lowerText.contains("millions of dollars") ||
                                lowerText.contains(", in millions,") -> MetricUnit.MILLIONS
                        lowerText.contains("in thousands") ||
                                lowerText.contains("(in thousands)") ||
                                lowerText.contains("thousands of dollars") ||
                                lowerText.contains(", in thousands,") -> MetricUnit.THOUSANDS
                        lowerText.contains("except per share") ||
                                lowerText.contains("per share data") -> MetricUnit.NONE
                        else -> MetricUnit.MILLIONS // Default for most SEC filings
                }
        }

        private fun detectPeriod(text: String): String? {
                val patterns =
                        listOf(
                                Regex(
                                        "(?i)(?:For the |Quarter Ended |Year Ended |Period Ended )([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
                                ),
                                Regex(
                                        "(?i)(?:Three Months Ended |Nine Months Ended |Twelve Months Ended )([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"
                                ),
                                Regex("(?i)(Q[1-4]\\s+\\d{4})"),
                                Regex("(?i)(FY\\s*\\d{4})")
                        )

                for (pattern in patterns) {
                        val match = pattern.find(text)
                        if (match != null) return match.groupValues[1].trim()
                }
                return null
        }

        private fun detectPeriodType(text: String): PeriodType? {
                val lowerText = text.lowercase()
                return when {
                        lowerText.contains("three months") ||
                                lowerText.contains("quarterly") ||
                                lowerText.contains("q1 ") ||
                                lowerText.contains("q2 ") ||
                                lowerText.contains("q3 ") ||
                                lowerText.contains("q4 ") -> PeriodType.QUARTERLY
                        lowerText.contains("twelve months") ||
                                lowerText.contains("annual") ||
                                lowerText.contains("fiscal year") ||
                                lowerText.contains("year ended") -> PeriodType.ANNUAL
                        lowerText.contains("nine months") || lowerText.contains("six months") ->
                                PeriodType.YTD
                        else -> null
                }
        }

        private fun searchMetricValues(
                text: String,
                term: String,
                category: MetricCategory,
                unit: MetricUnit,
                period: String?,
                periodType: PeriodType?,
                baseConfidence: Double
        ): List<ExtendedFinancialMetric> {
                val results = mutableListOf<ExtendedFinancialMetric>()

                // Enhanced number patterns with better context matching
                val patterns =
                        listOf(
                                // Pattern 1: Label followed by amount with optional parentheses
                                // e.g., "Total Revenue $ 123,456" or "Total Revenue (123,456)"
                                Regex(
                                        "(?i)${Regex.escape(term)}[:\\s\\|]*\\(?\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\)?(?:\\s*(?:million|billion|thousand|m|b|k))?",
                                        RegexOption.IGNORE_CASE
                                ),
                                // Pattern 2: Table format with pipe separator
                                // e.g., "Total Revenue | 123,456"
                                Regex(
                                        "(?i)${Regex.escape(term)}\\s*\\|\\s*\\$?\\s*\\(?([\\d,]+(?:\\.\\d+)?)\\)?",
                                        RegexOption.IGNORE_CASE
                                ),
                                // Pattern 3: Parentheses for negative numbers
                                // e.g., "Net Loss (123,456)"
                                Regex(
                                        "(?i)${Regex.escape(term)}[:\\s]*\\(\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\)",
                                        RegexOption.IGNORE_CASE
                                ),
                                // Pattern 4: Amount before label (less common)
                                // e.g., "$ 123,456 Total Revenue"
                                Regex(
                                        "(?i)\\$?\\s*\\(?([\\d,]+(?:\\.\\d+)?)\\)?\\s*[-–—]?\\s*${Regex.escape(term)}",
                                        RegexOption.IGNORE_CASE
                                )
                        )

                for (pattern in patterns) {
                        val matches = pattern.findAll(text)
                        for ((index, match) in matches.take(5).withIndex()) {
                                val valueStr = match.groupValues.getOrNull(1) ?: continue

                                // Skip if value is too small to be realistic (likely a ratio or
                                // percentage)
                                val prelimCheck =
                                        valueStr.replace(",", "").replace(".", "").toDoubleOrNull()
                                if (prelimCheck != null && prelimCheck < 0.01) continue

                                val context =
                                        text.substring(
                                                maxOf(0, match.range.first - 100),
                                                minOf(text.length, match.range.last + 100)
                                        )

                                // Determine if negative based on parentheses or context
                                val isNegative =
                                        match.value.trim().startsWith("(") &&
                                                match.value.trim().endsWith(")") ||
                                                context.lowercase().contains("loss") ||
                                                context.lowercase().contains("deficit")

                                val rawValue = parseNumber(valueStr, unit, isNegative, context)

                                if (rawValue != null &&
                                                rawValue.abs() >= java.math.BigDecimal("1000")
                                ) { // Filter out unrealistic small values

                                        results.add(
                                                ExtendedFinancialMetric(
                                                        name = term,
                                                        value = formatValue(rawValue),
                                                        rawValue = rawValue.toString(),
                                                        unit = unit,
                                                        period = period,
                                                        periodType = periodType,
                                                        category = category,
                                                        source = "Enhanced document extraction",
                                                        confidence =
                                                                baseConfidence *
                                                                        (1.0 -
                                                                                index *
                                                                                        0.08), // Gradual confidence decay
                                                        context = context.trim()
                                                )
                                        )
                                }
                        }
                }

                return results.distinctBy { it.rawValue } // Remove duplicate values
        }

        private fun parseNumber(
                value: String,
                unit: MetricUnit,
                isNegative: Boolean = false,
                contextText: String = ""
        ): java.math.BigDecimal? {
                val unitStr =
                        when (unit) {
                                MetricUnit.BILLIONS -> "billions"
                                MetricUnit.MILLIONS -> "millions"
                                MetricUnit.THOUSANDS -> "thousands"
                                else -> "dollars"
                        }

                val result = papyrus.util.FinancialPrecision.parseSecValue(value, unitStr, "USD")
                return result?.number?.numberValue(java.math.BigDecimal::class.java)?.let {
                        if (isNegative && it > java.math.BigDecimal.ZERO) it.negate() else it
                }
        }

        private fun formatValue(value: java.math.BigDecimal): String {
                val absValue = value.abs()
                val prefix = if (value < java.math.BigDecimal.ZERO) "-" else ""

                return when {
                        absValue >= java.math.BigDecimal("1000000000") ->
                                "${prefix}$${absValue.divide(java.math.BigDecimal("1000000000"), 2, java.math.RoundingMode.HALF_UP)}B"
                        absValue >= java.math.BigDecimal("1000000") ->
                                "${prefix}$${absValue.divide(java.math.BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP)}M"
                        absValue >= java.math.BigDecimal("1000") ->
                                "${prefix}$${absValue.divide(java.math.BigDecimal("1000"), 2, java.math.RoundingMode.HALF_UP)}K"
                        else -> "${prefix}$${absValue.setScale(2, java.math.RoundingMode.HALF_UP)}"
                }
        }

        private fun extractSection(text: String, sectionNames: List<String>): String? {
                for (name in sectionNames) {
                        val startPattern = Regex("(?i)$name")
                        val startMatch = startPattern.find(text) ?: continue

                        // 다음 주요 섹션까지 추출
                        val endPatterns =
                                listOf(
                                        "CONSOLIDATED STATEMENTS",
                                        "NOTES TO",
                                        "Item \\d+",
                                        "PART II"
                                )

                        var endIndex = text.length
                        for (endPattern in endPatterns) {
                                val endMatch =
                                        Regex("(?i)$endPattern").find(text, startMatch.range.last)
                                if (endMatch != null &&
                                                endMatch.range.first > startMatch.range.last + 100
                                ) {
                                        endIndex = minOf(endIndex, endMatch.range.first)
                                }
                        }

                        val section =
                                text.substring(
                                        startMatch.range.first,
                                        minOf(endIndex, startMatch.range.first + 15000)
                                )
                        if (section.length > 200) return section
                }
                return null
        }

        private fun deduplicateMetrics(
                metrics: List<ExtendedFinancialMetric>
        ): List<ExtendedFinancialMetric> {
                return metrics
                        .groupBy { it.category }
                        .mapValues { (_, list) ->
                                list.maxByOrNull { it.confidence } ?: list.first()
                        }
                        .values
                        .toList()
                        .sortedBy { it.category.ordinal }
        }

        private fun categorizeRisk(text: String): RiskCategory {
                val lowerText = text.lowercase()
                return when {
                        lowerText.contains("market") ||
                                lowerText.contains("economic") ||
                                lowerText.contains("demand") -> RiskCategory.MARKET
                        lowerText.contains("operation") ||
                                lowerText.contains("supply chain") ||
                                lowerText.contains("manufacturing") -> RiskCategory.OPERATIONAL
                        lowerText.contains("debt") ||
                                lowerText.contains("credit") ||
                                lowerText.contains("liquidity") ||
                                lowerText.contains("financial") -> RiskCategory.FINANCIAL
                        lowerText.contains("regulat") ||
                                lowerText.contains("compliance") ||
                                lowerText.contains("government") ||
                                lowerText.contains("law") -> RiskCategory.REGULATORY
                        lowerText.contains("competi") || lowerText.contains("rival") ->
                                RiskCategory.COMPETITIVE
                        lowerText.contains("technolog") ||
                                lowerText.contains("cyber") ||
                                lowerText.contains("security") ||
                                lowerText.contains("data") -> RiskCategory.TECHNOLOGY
                        lowerText.contains("legal") ||
                                lowerText.contains("litigation") ||
                                lowerText.contains("lawsuit") -> RiskCategory.LEGAL
                        lowerText.contains("environment") ||
                                lowerText.contains("climate") ||
                                lowerText.contains("sustain") -> RiskCategory.ENVIRONMENTAL
                        lowerText.contains("geopolit") ||
                                lowerText.contains("international") ||
                                lowerText.contains("tariff") ||
                                lowerText.contains("trade war") -> RiskCategory.GEOPOLITICAL
                        else -> RiskCategory.OTHER
                }
        }

        private fun assessRiskSeverity(text: String): RiskSeverity {
                val lowerText = text.lowercase()
                return when {
                        lowerText.contains("material adverse") ||
                                lowerText.contains("significant risk") ||
                                lowerText.contains("substantial harm") ||
                                lowerText.contains("critical") -> RiskSeverity.HIGH
                        lowerText.contains("may adversely") ||
                                lowerText.contains("could harm") ||
                                lowerText.contains("potential risk") -> RiskSeverity.MEDIUM
                        lowerText.contains("minor") || lowerText.contains("limited impact") ->
                                RiskSeverity.LOW
                        else -> RiskSeverity.MEDIUM
                }
        }

        private fun createRatio(
                koreanName: String,
                englishName: String,
                value: Double,
                suffix: String,
                description: String,
                category: RatioCategory,
                health: HealthStatus
        ): FinancialRatio {
                val formatted =
                        when (suffix) {
                                "%" -> String.format("%.1f%%", value)
                                "배" -> String.format("%.2f배", value)
                                else -> String.format("%.2f", value)
                        }

                return FinancialRatio(
                        name = "$koreanName ($englishName)",
                        value = value,
                        formattedValue = formatted,
                        description = description,
                        interpretation = getInterpretation(koreanName, health),
                        healthStatus = health,
                        category = category
                )
        }

        private fun assessProfitabilityHealth(
                value: Double,
                cautionThreshold: Double,
                goodThreshold: Double
        ): HealthStatus {
                return when {
                        value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
                        value >= goodThreshold -> HealthStatus.GOOD
                        value >= cautionThreshold -> HealthStatus.NEUTRAL
                        value >= 0 -> HealthStatus.CAUTION
                        else -> HealthStatus.WARNING
                }
        }

        private fun assessLiquidityHealth(
                value: Double,
                cautionThreshold: Double,
                goodThreshold: Double
        ): HealthStatus {
                return when {
                        value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
                        value >= goodThreshold -> HealthStatus.GOOD
                        value >= cautionThreshold -> HealthStatus.NEUTRAL
                        value >= 1.0 -> HealthStatus.CAUTION
                        else -> HealthStatus.WARNING
                }
        }

        private fun assessDebtHealth(
                value: Double,
                goodThreshold: Double,
                cautionThreshold: Double
        ): HealthStatus {
                return when {
                        value <= goodThreshold -> HealthStatus.EXCELLENT
                        value <= (goodThreshold + cautionThreshold) / 2 -> HealthStatus.GOOD
                        value <= cautionThreshold -> HealthStatus.NEUTRAL
                        value <= cautionThreshold * 1.5 -> HealthStatus.CAUTION
                        else -> HealthStatus.WARNING
                }
        }

        private fun assessEfficiencyHealth(
                value: Double,
                cautionThreshold: Double,
                goodThreshold: Double
        ): HealthStatus {
                return when {
                        value >= goodThreshold * 1.5 -> HealthStatus.EXCELLENT
                        value >= goodThreshold -> HealthStatus.GOOD
                        value >= cautionThreshold -> HealthStatus.NEUTRAL
                        value >= cautionThreshold * 0.5 -> HealthStatus.CAUTION
                        else -> HealthStatus.WARNING
                }
        }

        private fun assessInterestCoverageHealth(value: Double): HealthStatus {
                return when {
                        value >= 10.0 -> HealthStatus.EXCELLENT // 이자비용의 10배 이상 벌 수 있음
                        value >= 5.0 -> HealthStatus.GOOD // 5배 이상
                        value >= 2.5 -> HealthStatus.NEUTRAL // 2.5배 이상
                        value >= 1.5 -> HealthStatus.CAUTION // 1.5배 이상 (위험)
                        else -> HealthStatus.WARNING // 1.5배 미만 (매우 위험)
                }
        }

        private fun assessDebtRatioHealth(value: Double): HealthStatus {
                return when {
                        value <= 30.0 -> HealthStatus.EXCELLENT // 30% 이하
                        value <= 50.0 -> HealthStatus.GOOD // 50% 이하
                        value <= 70.0 -> HealthStatus.NEUTRAL // 70% 이하
                        value <= 85.0 -> HealthStatus.CAUTION // 85% 이하
                        else -> HealthStatus.WARNING // 85% 초과
                }
        }

        private fun assessRetainedEarningsHealth(value: Double): HealthStatus {
                return when {
                        value >= 60.0 -> HealthStatus.EXCELLENT // 60% 이상 (높은 유보)
                        value >= 40.0 -> HealthStatus.GOOD // 40% 이상
                        value >= 20.0 -> HealthStatus.NEUTRAL // 20% 이상
                        value >= 0.0 -> HealthStatus.CAUTION // 0% 이상 (낮은 유보)
                        else -> HealthStatus.WARNING // 음수 (누적 결손)
                }
        }

        private fun assessWorkingCapitalHealth(value: Double): HealthStatus {
                return when {
                        value >= 20.0 -> HealthStatus.EXCELLENT // 20% 이상
                        value >= 10.0 -> HealthStatus.GOOD // 10% 이상
                        value >= 5.0 -> HealthStatus.NEUTRAL // 5% 이상
                        value >= 0.0 -> HealthStatus.CAUTION // 0% 이상
                        else -> HealthStatus.WARNING // 음수 (유동성 위기)
                }
        }

        private fun getInterpretation(name: String, health: HealthStatus): String {
                return when (health) {
                        HealthStatus.EXCELLENT -> "${name}이(가) 매우 우수합니다."
                        HealthStatus.GOOD -> "${name}이(가) 양호합니다."
                        HealthStatus.NEUTRAL -> "${name}이(가) 평균 수준입니다."
                        HealthStatus.CAUTION -> "${name}에 주의가 필요합니다."
                        HealthStatus.WARNING -> "${name}이(가) 위험 수준입니다."
                }
        }

        private data class PatternDef(
                val term: String,
                val category: MetricCategory,
                val confidence: Double
        )
}
