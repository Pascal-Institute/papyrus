package papyrus.core.service

import papyrus.core.model.*

/** 향상된 재무 분석 파서 */
object EnhancedFinancialParser {

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
                    PatternDef("Cash and Equivalents", MetricCategory.CASH_AND_EQUIVALENTS, 0.95),
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
                    PatternDef("Cash from Operations", MetricCategory.OPERATING_CASH_FLOW, 0.95),
                    PatternDef("Net Cash from Operating", MetricCategory.OPERATING_CASH_FLOW, 0.95),
                    PatternDef(
                            "Net Cash Provided by Operating",
                            MetricCategory.OPERATING_CASH_FLOW,
                            1.0
                    ),
                    PatternDef("Investing Cash Flow", MetricCategory.INVESTING_CASH_FLOW, 1.0),
                    PatternDef("Cash from Investing", MetricCategory.INVESTING_CASH_FLOW, 0.95),
                    PatternDef("Net Cash from Investing", MetricCategory.INVESTING_CASH_FLOW, 0.95),
                    PatternDef("Financing Cash Flow", MetricCategory.FINANCING_CASH_FLOW, 1.0),
                    PatternDef("Cash from Financing", MetricCategory.FINANCING_CASH_FLOW, 0.95),
                    PatternDef("Net Cash from Financing", MetricCategory.FINANCING_CASH_FLOW, 0.95),
                    PatternDef("Free Cash Flow", MetricCategory.FREE_CASH_FLOW, 1.0),
                    PatternDef("Capital Expenditures", MetricCategory.CAPITAL_EXPENDITURES, 1.0),
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
                    PatternDef("Selling, General and Administrative", MetricCategory.SGA_EXPENSE, 0.95)
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
                    PatternDef("Book Value Per Share", MetricCategory.BOOK_VALUE_PER_SHARE, 1.0),
                    PatternDef("Dividends Per Share", MetricCategory.DIVIDENDS_PER_SHARE, 1.0)
            )

    // ===== 주식수 관련 패턴 =====
    private val sharesPatterns =
            listOf(
                    PatternDef("Shares Outstanding", MetricCategory.SHARES_OUTSTANDING, 1.0),
                    PatternDef("Common Shares Outstanding", MetricCategory.SHARES_OUTSTANDING, 1.0),
                    PatternDef("Basic Shares Outstanding", MetricCategory.SHARES_OUTSTANDING, 0.95),
                    PatternDef("Diluted Shares Outstanding", MetricCategory.SHARES_DILUTED, 1.0),
                    PatternDef("Weighted Average Shares", MetricCategory.SHARES_OUTSTANDING, 0.9)
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
                    sharesPatterns

    /** 문서에서 모든 재무 지표 추출 */
    fun parseFinancialMetrics(content: String): List<ExtendedFinancialMetric> {
        val cleanText = cleanHtml(content)
        val metrics = mutableListOf<ExtendedFinancialMetric>()

        // 금액 단위 감지 (thousands, millions, billions)
        val unit = detectUnit(cleanText)

        // 기간 감지
        val period = detectPeriod(cleanText)
        val periodType = detectPeriodType(cleanText)

        for (pattern in allPatterns) {
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

        // 중복 제거 및 가장 신뢰도 높은 것 선택
        return deduplicateMetrics(metrics)
    }

    /** 재무제표 섹션 파싱 */
    fun parseFinancialStatements(content: String): List<FinancialStatement> {
        val statements = mutableListOf<FinancialStatement>()
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
                                periodType = detectPeriodType(incomeStatementSection),
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
                                periodType = PeriodType.QUARTERLY, // Balance sheet is point-in-time
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
            return metrics.find { it.category == category }?.rawValue
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
                getValue(MetricCategory.LONG_TERM_DEBT) // Keep variable but unused or suppress

        // Validation: Check if values look reasonable
        // If gross profit > revenue, something is wrong with parsing
        if (grossProfit != null && revenue != null && grossProfit > revenue * 1.5) {
            println("WARNING: Gross profit ($grossProfit) > revenue ($revenue) * 1.5 - may indicate parsing error")
        }
        
        // 1. 매출총이익률 (Gross Margin)
        if (grossProfit != null && revenue != null && revenue > 0) {
            val ratio = (grossProfit / revenue) * 100
            
            // Sanity check: if ratio is unreasonably high, skip it
            if (ratio <= 100) {  // Gross margin should typically be < 100%
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
                println("WARNING: Gross Margin calculation resulted in $ratio% - skipping (likely parsing error)")
            }
        }

        // 2. 영업이익률 (Operating Margin)
        if (operatingIncome != null && revenue != null && revenue > 0) {
            val ratio = (operatingIncome / revenue) * 100
            
            // Sanity check
            if (ratio <= 100) {  // Operating margin should typically be < 100%
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
                println("WARNING: Operating Margin calculation resulted in $ratio% - skipping (likely parsing error)")
            }
        }

        // 3. 순이익률 (Net Profit Margin)
        if (netIncome != null && revenue != null && revenue > 0) {
            val ratio = (netIncome / revenue) * 100
            
            // Sanity check (allow negative for losses, but cap at reasonable positive values)
            if (ratio <= 100) {  // Net margin should typically be < 100%
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
                println("WARNING: Net Profit Margin calculation resulted in $ratio% - skipping (likely parsing error)")
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
            if (ratio <= 150) {  // Sanity check
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
        if (currentAssets != null && currentLiabilities != null && totalAssets != null && totalAssets > 0) {
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

    private fun cleanHtml(content: String): String {
        var cleaned = content
        
        // Remove script and style tags with their content
        cleaned = cleaned.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
        cleaned = cleaned.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
        
        // Remove XBRL tags but preserve content
        cleaned = cleaned.replace(Regex("</?ix:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?us-gaap:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?dei:[^>]*>", RegexOption.IGNORE_CASE), " ")
        cleaned = cleaned.replace(Regex("</?xbrli:[^>]*>", RegexOption.IGNORE_CASE), " ")
        
        // Preserve table structure by converting to readable format
        cleaned = cleaned.replace(Regex("<tr[^>]*>", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("<td[^>]*>|<th[^>]*>", RegexOption.IGNORE_CASE), " | ")
        cleaned = cleaned.replace(Regex("</td>|</th>", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("</tr>", RegexOption.IGNORE_CASE), "\n")
        
        // Replace <br> with newlines
        cleaned = cleaned.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        cleaned = cleaned.replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
        
        // Remove all remaining HTML tags
        cleaned = cleaned.replace(Regex("<[^>]*>"), " ")
        
        // Decode HTML entities
        cleaned = cleaned.replace(Regex("&nbsp;|&#160;|&#xA0;"), " ")
        cleaned = cleaned.replace(Regex("&amp;"), "&")
        cleaned = cleaned.replace(Regex("&lt;"), "<")
        cleaned = cleaned.replace(Regex("&gt;"), ">")
        cleaned = cleaned.replace(Regex("&quot;"), "\"")
        cleaned = cleaned.replace(Regex("&apos;|&#39;"), "'")
        cleaned = cleaned.replace(Regex("&#8211;|&#8212;|&mdash;|&ndash;"), "-")
        cleaned = cleaned.replace(Regex("&#\\d+;"), "") // Remove other numeric entities
        cleaned = cleaned.replace(Regex("&[a-zA-Z]+;"), "") // Remove named entities
        
        // Normalize whitespace but preserve line breaks
        cleaned = cleaned.replace(Regex("[ \\t]+"), " ")
        cleaned = cleaned.replace(Regex("\n\\s*\n+"), "\n")
        
        return cleaned.trim()
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
            lowerText.contains("except per share") || lowerText.contains("per share data") ->
                    MetricUnit.NONE
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
            lowerText.contains("nine months") || lowerText.contains("six months") -> PeriodType.YTD
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
                
                // Skip if value is too small to be realistic (likely a ratio or percentage)
                val prelimCheck = valueStr.replace(",", "").replace(".", "").toDoubleOrNull()
                if (prelimCheck != null && prelimCheck < 0.01) continue
                
                val context =
                        text.substring(
                                maxOf(0, match.range.first - 100),
                                minOf(text.length, match.range.last + 100)
                        )
                
                // Determine if negative based on parentheses or context
                val isNegative = match.value.trim().startsWith("(") && match.value.trim().endsWith(")") ||
                                 context.lowercase().contains("loss") ||
                                 context.lowercase().contains("deficit")
                
                val rawValue = parseNumber(valueStr, unit, isNegative, context)

                if (rawValue != null && kotlin.math.abs(rawValue) >= 1000) {  // Filter out unrealistic small values

                    results.add(
                            ExtendedFinancialMetric(
                                    name = term,
                                    value = formatValue(rawValue),
                                    rawValue = rawValue,
                                    unit = unit,
                                    period = period,
                                    periodType = periodType,
                                    category = category,
                                    source = "Enhanced document extraction",
                                    confidence =
                                            baseConfidence * (1.0 - index * 0.08), // Gradual confidence decay
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
    ): Double? {
        return try {
            val cleaned = value.replace(",", "").replace("$", "").trim()
            var number = cleaned.toDoubleOrNull() ?: return null

            // 컨텍스트 텍스트에서 단위 힌트 확인 (실제 단위가 텍스트에 명시된 경우)
            val lowerContext = contextText.lowercase()
            val actualUnit =
                    when {
                        lowerContext.contains("billion") || lowerContext.contains("b)") ->
                                MetricUnit.BILLIONS
                        lowerContext.contains("million") || lowerContext.contains("m)") ->
                                MetricUnit.MILLIONS
                        lowerContext.contains("thousand") || lowerContext.contains("k)") ->
                                MetricUnit.THOUSANDS
                        else -> unit
                    }

            // 단위에 따라 조정
            number =
                    when (actualUnit) {
                        MetricUnit.BILLIONS -> number * 1_000_000_000
                        MetricUnit.MILLIONS -> number * 1_000_000
                        MetricUnit.THOUSANDS -> number * 1_000
                        else -> number
                    }

            if (isNegative) -number else number
        } catch (e: Exception) {
            null
        }
    }

    private fun formatValue(value: Double): String {
        val absValue = kotlin.math.abs(value)
        val prefix = if (value < 0) "-" else ""

        return when {
            absValue >= 1_000_000_000 ->
                    "${prefix}$${String.format("%.2f", absValue / 1_000_000_000)}B"
            absValue >= 1_000_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000_000)}M"
            absValue >= 1_000 -> "${prefix}$${String.format("%.2f", absValue / 1_000)}K"
            else -> "${prefix}$${String.format("%.2f", absValue)}"
        }
    }

    private fun extractSection(text: String, sectionNames: List<String>): String? {
        for (name in sectionNames) {
            val startPattern = Regex("(?i)$name")
            val startMatch = startPattern.find(text) ?: continue

            // 다음 주요 섹션까지 추출
            val endPatterns = listOf("CONSOLIDATED STATEMENTS", "NOTES TO", "Item \\d+", "PART II")

            var endIndex = text.length
            for (endPattern in endPatterns) {
                val endMatch = Regex("(?i)$endPattern").find(text, startMatch.range.last)
                if (endMatch != null && endMatch.range.first > startMatch.range.last + 100) {
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
                .mapValues { (_, list) -> list.maxByOrNull { it.confidence } ?: list.first() }
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
            lowerText.contains("competi") || lowerText.contains("rival") -> RiskCategory.COMPETITIVE
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
            lowerText.contains("minor") || lowerText.contains("limited impact") -> RiskSeverity.LOW
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
            value >= 10.0 -> HealthStatus.EXCELLENT  // 이자비용의 10배 이상 벌 수 있음
            value >= 5.0 -> HealthStatus.GOOD        // 5배 이상
            value >= 2.5 -> HealthStatus.NEUTRAL     // 2.5배 이상
            value >= 1.5 -> HealthStatus.CAUTION     // 1.5배 이상 (위험)
            else -> HealthStatus.WARNING             // 1.5배 미만 (매우 위험)
        }
    }
    
    private fun assessDebtRatioHealth(value: Double): HealthStatus {
        return when {
            value <= 30.0 -> HealthStatus.EXCELLENT   // 30% 이하
            value <= 50.0 -> HealthStatus.GOOD        // 50% 이하
            value <= 70.0 -> HealthStatus.NEUTRAL     // 70% 이하
            value <= 85.0 -> HealthStatus.CAUTION     // 85% 이하
            else -> HealthStatus.WARNING              // 85% 초과
        }
    }
    
    private fun assessRetainedEarningsHealth(value: Double): HealthStatus {
        return when {
            value >= 60.0 -> HealthStatus.EXCELLENT   // 60% 이상 (높은 유보)
            value >= 40.0 -> HealthStatus.GOOD        // 40% 이상
            value >= 20.0 -> HealthStatus.NEUTRAL     // 20% 이상
            value >= 0.0 -> HealthStatus.CAUTION      // 0% 이상 (낮은 유보)
            else -> HealthStatus.WARNING              // 음수 (누적 결손)
        }
    }
    
    private fun assessWorkingCapitalHealth(value: Double): HealthStatus {
        return when {
            value >= 20.0 -> HealthStatus.EXCELLENT   // 20% 이상
            value >= 10.0 -> HealthStatus.GOOD        // 10% 이상
            value >= 5.0 -> HealthStatus.NEUTRAL      // 5% 이상
            value >= 0.0 -> HealthStatus.CAUTION      // 0% 이상
            else -> HealthStatus.WARNING              // 음수 (유동성 위기)
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
