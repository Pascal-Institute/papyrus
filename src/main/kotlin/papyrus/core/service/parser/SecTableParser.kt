package papyrus.core.service.parser

import papyrus.core.model.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * SEC 문서 테이블 전문 파서
 * 
 * SEC 10-K, 10-Q 등의 재무제표 테이블을 정확하게 파싱합니다.
 * HTML 테이블 구조를 인식하고, 숫자 데이터를 정확하게 추출합니다.
 * 
 * 지원 형식:
 * - Apple, Microsoft, NVE 등 다양한 회사의 10-K 형식
 * - Products/Services 분리 매출 형식
 * - 세그먼트별 매출 형식
 * - 간단한 단일 테이블 형식
 */
object SecTableParser {
    
    /**
     * 재무제표 테이블 파싱 결과
     */
    data class ParsedFinancialTable(
        val statementType: StatementType,
        val title: String,
        val periods: List<String>,       // 열 헤더 (예: "2024", "2023")
        val rows: List<TableRow>,
        val unit: MetricUnit,
        val currency: String = "USD",
        val rawHtml: String = ""
    )
    
    data class TableRow(
        val label: String,                  // 행 레이블 (예: "Total Revenue")
        val values: List<BigDecimal?>,      // 각 기간별 값
        val isSubtotal: Boolean = false,    // 소계 여부
        val isTotal: Boolean = false,       // 합계 여부
        val indentLevel: Int = 0,           // 들여쓰기 레벨
        val category: MetricCategory? = null
    )
    
    // 다양한 회사 형식을 위한 추가 섹션 패턴
    private val extendedSectionPatterns = mapOf(
        StatementType.INCOME_STATEMENT to listOf(
            "CONSOLIDATED STATEMENTS OF OPERATIONS",
            "CONSOLIDATED STATEMENTS OF INCOME",
            "CONSOLIDATED STATEMENTS OF EARNINGS",
            "STATEMENTS OF OPERATIONS",
            "INCOME STATEMENT",
            "STATEMENT OF OPERATIONS",
            "STATEMENT OF INCOME",
            "NET SALES BY CATEGORY",  // Apple 형식
            "REVENUE BY PRODUCT",     // 일부 회사 형식
            "RESULTS OF OPERATIONS"
        ),
        StatementType.BALANCE_SHEET to listOf(
            "CONSOLIDATED BALANCE SHEETS",
            "CONSOLIDATED BALANCE SHEET",
            "BALANCE SHEETS",
            "BALANCE SHEET",
            "STATEMENT OF FINANCIAL POSITION",
            "CONSOLIDATED STATEMENTS OF FINANCIAL POSITION",
            "FINANCIAL POSITION"
        ),
        StatementType.CASH_FLOW_STATEMENT to listOf(
            "CONSOLIDATED STATEMENTS OF CASH FLOWS",
            "STATEMENTS OF CASH FLOWS",
            "CASH FLOW STATEMENT",
            "STATEMENT OF CASH FLOWS",
            "CASH FLOW STATEMENTS"
        ),
        StatementType.COMPREHENSIVE_INCOME to listOf(
            "CONSOLIDATED STATEMENTS OF COMPREHENSIVE INCOME",
            "STATEMENTS OF COMPREHENSIVE INCOME",
            "COMPREHENSIVE INCOME",
            "STATEMENT OF COMPREHENSIVE INCOME"
        ),
        StatementType.EQUITY_STATEMENT to listOf(
            "CONSOLIDATED STATEMENTS OF EQUITY",
            "STATEMENTS OF STOCKHOLDERS' EQUITY",
            "STATEMENT OF CHANGES IN EQUITY",
            "CONSOLIDATED STATEMENTS OF SHAREHOLDERS' EQUITY",
            "STOCKHOLDERS' EQUITY"
        )
    )
    
    /**
     * HTML 문서에서 재무제표 테이블을 찾아 파싱
     */
    fun parseFinancialTables(htmlContent: String): List<ParsedFinancialTable> {
        val tables = mutableListOf<ParsedFinancialTable>()
        
        // 각 재무제표 유형별로 파싱 시도
        for ((statementType, patterns) in extendedSectionPatterns) {
            val parsed = findAndParseTableWithPatterns(htmlContent, statementType, patterns)
            if (parsed != null) {
                tables.add(parsed)
            }
        }
        
        // 테이블을 찾지 못한 경우 대체 전략: 텍스트 기반 파싱
        if (tables.isEmpty()) {
            val fallbackTables = parsePlainTextTables(htmlContent)
            tables.addAll(fallbackTables)
        }
        
        return tables
    }
    
    /**
     * 특정 유형의 재무제표 테이블 찾기 및 파싱 (확장된 패턴 사용)
     */
    private fun findAndParseTableWithPatterns(
        content: String, 
        type: StatementType,
        patterns: List<String>
    ): ParsedFinancialTable? {
        // 섹션 찾기
        val section = findSectionWithPatterns(content, patterns) ?: return null
        
        // 단위 감지
        val unit = detectTableUnit(section)
        
        // 기간 헤더 추출
        val periods = extractPeriodHeaders(section)
        
        // 테이블 행 파싱
        val rows = parseTableRows(section, type, periods.size)
        
        if (rows.isEmpty()) return null
        
        return ParsedFinancialTable(
            statementType = type,
            title = patterns.firstOrNull() ?: type.name,
            periods = periods,
            rows = rows,
            unit = unit,
            rawHtml = section.take(3000)
        )
    }
    
    /**
     * 확장된 패턴으로 섹션 찾기
     */
    private fun findSectionWithPatterns(content: String, patterns: List<String>): String? {
        val lowerContent = content.lowercase()
        
        for (pattern in patterns) {
            val lowerPattern = pattern.lowercase()
            val startIdx = lowerContent.indexOf(lowerPattern)
            
            if (startIdx != -1) {
                // 다음 주요 섹션까지 또는 최대 50000자까지 추출
                val endPatterns = listOf(
                    "consolidated statements of",
                    "notes to consolidated",
                    "notes to the consolidated",
                    "item 1a", "item 1b", "item 1c",
                    "item 2", "item 3", "item 4",
                    "part ii", "part iii",
                    "signatures",
                    "report of independent"
                )
                
                var endIdx = content.length
                for (endPattern in endPatterns) {
                    val possibleEnd = lowerContent.indexOf(endPattern, startIdx + pattern.length + 100)
                    if (possibleEnd != -1 && possibleEnd < endIdx) {
                        endIdx = possibleEnd
                    }
                }
                
                val section = content.substring(startIdx, minOf(endIdx, startIdx + 50000))
                if (section.length > 500) return section
            }
        }
        return null
    }
    
    /**
     * 일반 텍스트 기반 테이블 파싱 (HTML이 아닌 경우)
     */
    private fun parsePlainTextTables(text: String): List<ParsedFinancialTable> {
        val tables = mutableListOf<ParsedFinancialTable>()
        
        // 손익계산서 섹션 찾기
        val incomeSection = findIncomeStatementInText(text)
        if (incomeSection != null) {
            val rows = parseTextTableRows(incomeSection, StatementType.INCOME_STATEMENT)
            if (rows.isNotEmpty()) {
                tables.add(ParsedFinancialTable(
                    statementType = StatementType.INCOME_STATEMENT,
                    title = "Income Statement",
                    periods = extractYearsFromText(incomeSection),
                    rows = rows,
                    unit = detectTableUnit(incomeSection)
                ))
            }
        }
        
        // 재무상태표 섹션 찾기
        val balanceSection = findBalanceSheetInText(text)
        if (balanceSection != null) {
            val rows = parseTextTableRows(balanceSection, StatementType.BALANCE_SHEET)
            if (rows.isNotEmpty()) {
                tables.add(ParsedFinancialTable(
                    statementType = StatementType.BALANCE_SHEET,
                    title = "Balance Sheet",
                    periods = extractYearsFromText(balanceSection),
                    rows = rows,
                    unit = detectTableUnit(balanceSection)
                ))
            }
        }
        
        // 현금흐름표 섹션 찾기
        val cashFlowSection = findCashFlowInText(text)
        if (cashFlowSection != null) {
            val rows = parseTextTableRows(cashFlowSection, StatementType.CASH_FLOW_STATEMENT)
            if (rows.isNotEmpty()) {
                tables.add(ParsedFinancialTable(
                    statementType = StatementType.CASH_FLOW_STATEMENT,
                    title = "Cash Flow Statement",
                    periods = extractYearsFromText(cashFlowSection),
                    rows = rows,
                    unit = detectTableUnit(cashFlowSection)
                ))
            }
        }
        
        return tables
    }
    
    private fun findIncomeStatementInText(text: String): String? {
        val patterns = listOf(
            "Statements of Operations", "Statement of Income", "Income Statement",
            "Consolidated Statements of Operations", "Results of Operations"
        )
        return findTextSection(text, patterns)
    }
    
    private fun findBalanceSheetInText(text: String): String? {
        val patterns = listOf(
            "Balance Sheets", "Balance Sheet", "Financial Position",
            "Consolidated Balance Sheets", "Statement of Financial Position"
        )
        return findTextSection(text, patterns)
    }
    
    private fun findCashFlowInText(text: String): String? {
        val patterns = listOf(
            "Cash Flows", "Statement of Cash Flows", 
            "Consolidated Statements of Cash Flows"
        )
        return findTextSection(text, patterns)
    }
    
    private fun findTextSection(text: String, patterns: List<String>): String? {
        val lowerText = text.lowercase()
        
        for (pattern in patterns) {
            val startIdx = lowerText.indexOf(pattern.lowercase())
            if (startIdx != -1) {
                // 다음 섹션 또는 20000자까지
                val sectionMarkers = listOf(
                    "notes to", "item ", "part ", "signatures",
                    "consolidated statements of", "statement of"
                )
                
                var endIdx = minOf(text.length, startIdx + 20000)
                for (marker in sectionMarkers) {
                    val possibleEnd = lowerText.indexOf(marker, startIdx + pattern.length + 50)
                    if (possibleEnd != -1 && possibleEnd < endIdx) {
                        endIdx = possibleEnd
                    }
                }
                
                val section = text.substring(startIdx, endIdx)
                if (section.length > 300) return section
            }
        }
        return null
    }
    
    private fun extractYearsFromText(text: String): List<String> {
        val yearPattern = Regex("""20\d{2}""")
        return yearPattern.findAll(text.take(2000))
            .map { it.value }
            .toSet()
            .sorted()
            .takeLast(4)
            .reversed()
    }
    
    private fun parseTextTableRows(section: String, type: StatementType): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        val lines = section.split("\n")
        
        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.length < 5) continue
            
            val numbers = numberPattern.findAll(trimmedLine).toList()
            if (numbers.isEmpty()) continue
            
            val firstNumberStart = numbers.first().range.first
            val label = trimmedLine.substring(0, firstNumberStart).trim()
            
            // 유효성 검사
            if (label.length < 3) continue
            if (label.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() || it == '$' }) continue
            if (label.contains("Page ") || label.matches(Regex("""F-\d+.*"""))) continue
            
            val values = numbers.map { parseTableValue(it.value) }
            if (values.all { it == null }) continue
            
            val category = mapLabelToCategory(label, type)
            val isTotal = label.lowercase().let { 
                it.startsWith("total") || it.contains("total ") 
            }
            
            rows.add(TableRow(
                label = label,
                values = values,
                isTotal = isTotal,
                category = category
            ))
        }
        
        return rows
    }
    
    /**
     * 문서에서 특정 섹션 찾기
     */
    private fun findSection(content: String, patterns: List<String>): String? {
        val lowerContent = content.lowercase()
        
        for (pattern in patterns) {
            val lowerPattern = pattern.lowercase()
            val startIdx = lowerContent.indexOf(lowerPattern)
            
            if (startIdx != -1) {
                // 다음 주요 섹션까지 또는 최대 50000자까지 추출
                val endPatterns = listOf(
                    "consolidated statements of",
                    "notes to consolidated",
                    "notes to the consolidated",
                    "item 1a",
                    "item 2",
                    "item 3",
                    "part ii",
                    "signatures"
                )
                
                var endIdx = content.length
                for (endPattern in endPatterns) {
                    val possibleEnd = lowerContent.indexOf(endPattern, startIdx + pattern.length + 100)
                    if (possibleEnd != -1 && possibleEnd < endIdx) {
                        endIdx = possibleEnd
                    }
                }
                
                val section = content.substring(startIdx, minOf(endIdx, startIdx + 50000))
                if (section.length > 500) return section
            }
        }
        return null
    }
    
    /**
     * 테이블 단위 감지 (millions, thousands, billions)
     */
    private fun detectTableUnit(section: String): MetricUnit {
        val lowerSection = section.lowercase()
        
        return when {
            lowerSection.contains("in billions") || 
            lowerSection.contains("(billions)") ||
            lowerSection.contains("$ billions") -> MetricUnit.BILLIONS
            
            lowerSection.contains("in millions") || 
            lowerSection.contains("(in millions)") ||
            lowerSection.contains("$ in millions") ||
            lowerSection.contains("amounts in millions") ||
            lowerSection.contains("(millions)") -> MetricUnit.MILLIONS
            
            lowerSection.contains("in thousands") || 
            lowerSection.contains("(in thousands)") ||
            lowerSection.contains("(thousands)") -> MetricUnit.THOUSANDS
            
            // SEC 기본값은 millions
            else -> MetricUnit.MILLIONS
        }
    }
    
    /**
     * 기간 헤더 추출 (예: "December 31, 2024", "2024", "2023")
     */
    private fun extractPeriodHeaders(section: String): List<String> {
        val periods = mutableListOf<String>()
        
        // 패턴 1: 날짜 형식 (December 31, 2024)
        val datePattern = Regex(
            """(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},?\s+\d{4}""",
            RegexOption.IGNORE_CASE
        )
        
        // 패턴 2: 연도만 (2024, 2023)
        val yearPattern = Regex("""\b(20\d{2})\b""")
        
        // 패턴 3: 분기 형식 (Q3 2024)
        val quarterPattern = Regex("""Q[1-4]\s+20\d{2}""", RegexOption.IGNORE_CASE)
        
        // 먼저 날짜 형식 시도
        val dateMatches = datePattern.findAll(section.take(2000)).toList()
        if (dateMatches.size >= 2) {
            return dateMatches.take(4).map { it.value }.distinct()
        }
        
        // 분기 형식 시도
        val quarterMatches = quarterPattern.findAll(section.take(2000)).toList()
        if (quarterMatches.size >= 2) {
            return quarterMatches.take(4).map { it.value }.distinct()
        }
        
        // 연도만 추출
        val yearMatches = yearPattern.findAll(section.take(2000)).toList()
        val years = yearMatches.map { it.value }.distinct().sorted().takeLast(4)
        
        return if (years.isNotEmpty()) years else listOf("Current Period", "Prior Period")
    }
    
    /**
     * 테이블 행 파싱
     */
    private fun parseTableRows(
        section: String, 
        type: StatementType,
        expectedColumns: Int
    ): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        
        // HTML 테이블 행 추출
        val tableRowPattern = Regex("""<tr[^>]*>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
        val tableRows = tableRowPattern.findAll(section)
        
        for (trMatch in tableRows) {
            val rowHtml = trMatch.groupValues[1]
            
            // 셀 추출 (td 또는 th)
            val cellPattern = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""", RegexOption.DOT_MATCHES_ALL)
            val cells = cellPattern.findAll(rowHtml).map { cleanCellContent(it.groupValues[1]) }.toList()
            
            if (cells.isEmpty()) continue
            
            val label = cells.firstOrNull()?.trim() ?: continue
            
            // 레이블이 비어있거나 너무 짧으면 스킵
            if (label.length < 3) continue
            
            // 숫자만 있는 행은 스킵 (헤더일 가능성)
            if (label.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() }) continue
            
            // 나머지 셀에서 숫자 추출
            val values = cells.drop(1).map { parseTableValue(it) }
            
            // 값이 하나도 없으면 스킵
            if (values.all { it == null }) continue
            
            // 카테고리 매핑
            val category = mapLabelToCategory(label, type)
            
            // 합계/소계 여부 확인
            val isTotal = label.lowercase().let { 
                it.startsWith("total") || it.contains("total ") 
            }
            val isSubtotal = label.lowercase().contains("subtotal")
            
            // 들여쓰기 레벨 추정
            val indentLevel = estimateIndentLevel(label, rowHtml)
            
            rows.add(TableRow(
                label = label,
                values = values,
                isSubtotal = isSubtotal,
                isTotal = isTotal,
                indentLevel = indentLevel,
                category = category
            ))
        }
        
        // HTML 테이블이 없으면 텍스트 기반 파싱 시도
        if (rows.isEmpty()) {
            return parseTextBasedTable(section, type, expectedColumns)
        }
        
        return rows
    }
    
    /**
     * 텍스트 기반 테이블 파싱 (HTML 테이블이 없는 경우)
     */
    private fun parseTextBasedTable(
        section: String, 
        type: StatementType,
        expectedColumns: Int
    ): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        val cleanSection = cleanHtmlForText(section)
        val lines = cleanSection.split("\n")
        
        // 숫자 패턴 (음수 포함)
        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.length < 5) continue
            
            // 숫자 찾기
            val numbers = numberPattern.findAll(trimmedLine).toList()
            if (numbers.isEmpty()) continue
            
            // 첫 번째 숫자 앞의 텍스트가 레이블
            val firstNumberStart = numbers.first().range.first
            val label = trimmedLine.substring(0, firstNumberStart).trim()
            
            if (label.length < 3) continue
            if (label.all { it.isDigit() || it == ',' || it == '.' || it.isWhitespace() }) continue
            
            // 숫자들 파싱
            val values = numbers.map { parseTableValue(it.value) }
            
            val category = mapLabelToCategory(label, type)
            val isTotal = label.lowercase().startsWith("total")
            
            rows.add(TableRow(
                label = label,
                values = values,
                isTotal = isTotal,
                category = category
            ))
        }
        
        return rows
    }
    
    /**
     * HTML 셀 내용 정리
     */
    private fun cleanCellContent(html: String): String {
        var cleaned = html
        
        // XBRL 태그 제거하되 내용 보존
        cleaned = cleaned.replace(Regex("""<ix:[^>]*>""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""</ix:[^>]*>""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""<us-gaap:[^>]*>""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""</us-gaap:[^>]*>""", RegexOption.IGNORE_CASE), "")
        
        // 기타 HTML 태그 제거
        cleaned = cleaned.replace(Regex("""<[^>]+>"""), " ")
        
        // HTML 엔티티 디코딩
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace("&#160;", " ")
        cleaned = cleaned.replace("&amp;", "&")
        cleaned = cleaned.replace("&lt;", "<")
        cleaned = cleaned.replace("&gt;", ">")
        cleaned = cleaned.replace("&quot;", "\"")
        cleaned = cleaned.replace("&#8211;", "-")
        cleaned = cleaned.replace("&#8212;", "-")
        cleaned = cleaned.replace("—", "-")
        cleaned = cleaned.replace("–", "-")
        
        // 공백 정리
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        return cleaned
    }
    
    /**
     * 텍스트용 HTML 정리
     */
    private fun cleanHtmlForText(html: String): String {
        var cleaned = html
        
        // 스크립트, 스타일 제거
        cleaned = cleaned.replace(Regex("""<script[^>]*>.*?</script>""", RegexOption.DOT_MATCHES_ALL), "")
        cleaned = cleaned.replace(Regex("""<style[^>]*>.*?</style>""", RegexOption.DOT_MATCHES_ALL), "")
        
        // 줄바꿈 처리
        cleaned = cleaned.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</tr>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("""</div>""", RegexOption.IGNORE_CASE), "\n")
        
        // 테이블 셀 구분
        cleaned = cleaned.replace(Regex("""</td>""", RegexOption.IGNORE_CASE), " | ")
        cleaned = cleaned.replace(Regex("""</th>""", RegexOption.IGNORE_CASE), " | ")
        
        // 모든 태그 제거
        cleaned = cleaned.replace(Regex("""<[^>]+>"""), " ")
        
        // HTML 엔티티 처리
        cleaned = cleaned.replace("&nbsp;", " ")
        cleaned = cleaned.replace("&#160;", " ")
        cleaned = cleaned.replace("&amp;", "&")
        
        return cleaned
    }
    
    /**
     * 테이블 셀 값 파싱 (정밀 계산용 BigDecimal 사용)
     */
    private fun parseTableValue(cellText: String): BigDecimal? {
        val cleaned = cellText.trim()
        
        // 비어있거나 대시만 있으면 null
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "—" || cleaned == "–") {
            return null
        }
        
        // 괄호는 음수를 의미
        val isNegative = cleaned.startsWith("(") && cleaned.endsWith(")")
        
        // 숫자만 추출
        val numberStr = cleaned
            .replace("$", "")
            .replace(",", "")
            .replace("(", "")
            .replace(")", "")
            .replace(" ", "")
            .trim()
        
        return try {
            val value = BigDecimal(numberStr)
            if (isNegative) value.negate() else value
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 레이블을 MetricCategory로 매핑
     */
    private fun mapLabelToCategory(label: String, statementType: StatementType): MetricCategory? {
        val lowerLabel = label.lowercase().trim()
        
        return when {
            // 수익 관련
            lowerLabel == "total revenue" || lowerLabel == "total revenues" -> MetricCategory.REVENUE
            lowerLabel == "net revenue" || lowerLabel == "net revenues" -> MetricCategory.REVENUE
            lowerLabel == "revenue" || lowerLabel == "revenues" -> MetricCategory.REVENUE
            lowerLabel == "net sales" || lowerLabel == "total net sales" -> MetricCategory.REVENUE
            lowerLabel == "sales" -> MetricCategory.REVENUE
            
            // 매출원가
            lowerLabel.contains("cost of revenue") -> MetricCategory.COST_OF_REVENUE
            lowerLabel.contains("cost of sales") -> MetricCategory.COST_OF_REVENUE
            lowerLabel.contains("cost of goods sold") || lowerLabel == "cogs" -> MetricCategory.COST_OF_REVENUE
            
            // 이익
            lowerLabel == "gross profit" || lowerLabel == "gross margin" -> MetricCategory.GROSS_PROFIT
            lowerLabel.contains("operating income") -> MetricCategory.OPERATING_INCOME
            lowerLabel.contains("income from operations") -> MetricCategory.OPERATING_INCOME
            lowerLabel == "net income" || lowerLabel.contains("net income (loss)") -> MetricCategory.NET_INCOME
            lowerLabel == "net earnings" -> MetricCategory.NET_INCOME
            lowerLabel == "net loss" -> MetricCategory.NET_INCOME
            lowerLabel == "ebitda" || lowerLabel == "adjusted ebitda" -> MetricCategory.EBITDA
            
            // 자산
            lowerLabel == "total assets" -> MetricCategory.TOTAL_ASSETS
            lowerLabel == "total current assets" -> MetricCategory.CURRENT_ASSETS
            lowerLabel.contains("cash and cash equivalents") -> MetricCategory.CASH_AND_EQUIVALENTS
            lowerLabel == "accounts receivable" || lowerLabel.contains("trade receivables") -> MetricCategory.ACCOUNTS_RECEIVABLE
            lowerLabel == "inventories" || lowerLabel == "inventory" -> MetricCategory.INVENTORY
            lowerLabel == "total inventories" -> MetricCategory.INVENTORY
            
            // 부채
            lowerLabel == "total liabilities" -> MetricCategory.TOTAL_LIABILITIES
            lowerLabel == "total current liabilities" -> MetricCategory.CURRENT_LIABILITIES
            lowerLabel.contains("long-term debt") || lowerLabel.contains("long term debt") -> MetricCategory.LONG_TERM_DEBT
            lowerLabel == "accounts payable" -> MetricCategory.ACCOUNTS_PAYABLE
            
            // 자본
            lowerLabel == "total equity" -> MetricCategory.TOTAL_EQUITY
            lowerLabel.contains("stockholders' equity") || lowerLabel.contains("shareholders' equity") -> MetricCategory.TOTAL_EQUITY
            lowerLabel.contains("total stockholders' equity") -> MetricCategory.TOTAL_EQUITY
            lowerLabel == "retained earnings" || lowerLabel == "accumulated deficit" -> MetricCategory.RETAINED_EARNINGS
            
            // 현금흐름
            lowerLabel.contains("net cash provided by operating") -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.contains("net cash from operating") -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.contains("cash from operations") -> MetricCategory.OPERATING_CASH_FLOW
            lowerLabel.contains("net cash used in investing") -> MetricCategory.INVESTING_CASH_FLOW
            lowerLabel.contains("net cash from investing") -> MetricCategory.INVESTING_CASH_FLOW
            lowerLabel.contains("net cash used in financing") -> MetricCategory.FINANCING_CASH_FLOW
            lowerLabel.contains("net cash from financing") -> MetricCategory.FINANCING_CASH_FLOW
            lowerLabel.contains("capital expenditures") || lowerLabel == "capex" -> MetricCategory.CAPITAL_EXPENDITURES
            lowerLabel == "free cash flow" -> MetricCategory.FREE_CASH_FLOW
            
            // 주당 지표
            lowerLabel.contains("basic earnings per share") || lowerLabel == "basic eps" -> MetricCategory.EPS_BASIC
            lowerLabel.contains("diluted earnings per share") || lowerLabel == "diluted eps" -> MetricCategory.EPS_DILUTED
            lowerLabel == "earnings per share" || lowerLabel == "eps" -> MetricCategory.EPS_BASIC
            
            // 비용
            lowerLabel.contains("research and development") || lowerLabel == "r&d expense" -> MetricCategory.RD_EXPENSE
            lowerLabel.contains("selling, general") || lowerLabel == "sg&a" -> MetricCategory.SGA_EXPENSE
            lowerLabel.contains("interest expense") -> MetricCategory.INTEREST_EXPENSE
            lowerLabel.contains("income tax") || lowerLabel.contains("provision for") -> MetricCategory.INCOME_TAX
            lowerLabel.contains("depreciation") -> MetricCategory.DEPRECIATION
            
            else -> null
        }
    }
    
    /**
     * 들여쓰기 레벨 추정
     */
    private fun estimateIndentLevel(label: String, rowHtml: String): Int {
        // 공백으로 시작하는 경우
        val leadingSpaces = label.takeWhile { it.isWhitespace() }.length
        if (leadingSpaces > 0) return (leadingSpaces / 3).coerceAtMost(3)
        
        // CSS padding 확인
        val paddingMatch = Regex("""padding-left:\s*(\d+)""").find(rowHtml)
        if (paddingMatch != null) {
            val padding = paddingMatch.groupValues[1].toIntOrNull() ?: 0
            return (padding / 20).coerceAtMost(3)
        }
        
        return 0
    }
    
    /**
     * 파싱된 테이블에서 ExtendedFinancialMetric 리스트 생성
     */
    fun convertToMetrics(tables: List<ParsedFinancialTable>): List<ExtendedFinancialMetric> {
        val metrics = mutableListOf<ExtendedFinancialMetric>()
        
        for (table in tables) {
            val multiplier = when (table.unit) {
                MetricUnit.BILLIONS -> BigDecimal("1000000000")
                MetricUnit.MILLIONS -> BigDecimal("1000000")
                MetricUnit.THOUSANDS -> BigDecimal("1000")
                else -> BigDecimal.ONE
            }
            
            for (row in table.rows) {
                val category = row.category ?: continue
                
                // 가장 최근 값 사용 (첫 번째 열)
                val currentValue = row.values.firstOrNull { it != null } ?: continue
                val priorValue = row.values.drop(1).firstOrNull { it != null }
                
                // 단위 적용
                val scaledValue = currentValue.multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
                
                // YoY 변화율 계산
                val yoyChange = if (priorValue != null && priorValue != BigDecimal.ZERO) {
                    currentValue.subtract(priorValue)
                        .divide(priorValue.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal("100"))
                        .toDouble()
                } else null
                
                metrics.add(ExtendedFinancialMetric(
                    name = row.label,
                    value = formatValue(scaledValue),
                    rawValue = scaledValue,
                    unit = MetricUnit.DOLLARS,  // 변환 후 단위
                    period = table.periods.firstOrNull(),
                    periodType = detectPeriodType(table.periods.firstOrNull()),
                    category = category,
                    source = "SEC Table Parser - ${table.statementType.name}",
                    confidence = if (row.isTotal) 0.95 else 0.85,
                    yearOverYearChange = yoyChange,
                    context = "From ${table.title}"
                ))
            }
        }
        
        // 중복 제거: 같은 카테고리에서 가장 신뢰도 높은 것 선택
        return metrics
            .groupBy { it.category }
            .mapValues { (_, list) -> list.maxByOrNull { it.confidence } ?: list.first() }
            .values
            .toList()
    }
    
    /**
     * 값 포맷팅
     */
    private fun formatValue(value: Double): String {
        val absValue = kotlin.math.abs(value)
        val prefix = if (value < 0) "-" else ""
        
        return when {
            absValue >= 1_000_000_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000_000_000)}B"
            absValue >= 1_000_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000_000)}M"
            absValue >= 1_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000)}K"
            else -> "${prefix}$${String.format("%.2f", absValue)}"
        }
    }
    
    /**
     * 기간 유형 감지
     */
    private fun detectPeriodType(period: String?): PeriodType? {
        if (period == null) return null
        val lower = period.lowercase()
        
        return when {
            lower.contains("q1") || lower.contains("q2") || 
            lower.contains("q3") || lower.contains("q4") -> PeriodType.QUARTERLY
            lower.matches(Regex(""".*20\d{2}.*""")) -> PeriodType.ANNUAL
            else -> null
        }
    }
}
