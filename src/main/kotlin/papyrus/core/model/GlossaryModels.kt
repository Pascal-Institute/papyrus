package papyrus.core.model

import kotlinx.serialization.Serializable

/**
 * Comprehensive financial term glossary for investor education Following AGENTS.md principles:
 * intuitive, meaningful, with financial precision
 */
@Serializable
data class FinancialGlossaryTerm(
        val term: String,
        val category: GlossaryCategory,
        val simpleDefinition: String,
        val technicalDefinition: String,
        val realWorldAnalogy: String,
        val whyItMatters: String,
        val investmentRelevance: String,
        val example: String,
        val relatedTerms: List<String> = emptyList(),
        val warningSignals: List<String> = emptyList(),
        val idealRange: String? = null
)

enum class GlossaryCategory {
    PROFITABILITY,
    LIQUIDITY,
    SOLVENCY,
    EFFICIENCY,
    VALUATION,
    INCOME_STATEMENT,
    BALANCE_SHEET,
    CASH_FLOW,
    MARKET_METRICS,
    REPORTING_CONCEPTS,
    RISK_INDICATORS
}

/** Pre-built comprehensive glossary following financial precision principles */
object FinancialGlossary {

    private val glossaryTerms =
            listOf(
                    // === PROFITABILITY METRICS ===
                    FinancialGlossaryTerm(
                            term = "Gross Margin",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "매출에서 제품/서비스를 만드는 직접 비용을 뺀 나머지 비율",
                            technicalDefinition = "(Revenue - Cost of Goods Sold) / Revenue × 100",
                            realWorldAnalogy =
                                    "당신이 100원에 물건을 팔아서 30원의 원가를 쓴다면, 매출총이익률은 70%입니다. 높을수록 가격 결정력이 강합니다.",
                            whyItMatters =
                                    "회사가 제품에서 얼마나 이익을 남기는지 보여줍니다. 경쟁사보다 높으면 브랜드 파워나 원가 관리가 우수합니다.",
                            investmentRelevance =
                                    "지속적으로 높은 매출총이익률은 경쟁 우위의 신호입니다. 급격한 하락은 가격 경쟁 심화나 원가 상승을 의미합니다.",
                            example = "Apple의 매출총이익률 43%는 프리미엄 가격 전략과 규모의 경제를 반영합니다.",
                            relatedTerms = listOf("Operating Margin", "Net Profit Margin", "COGS"),
                            warningSignals = listOf("지속적인 하락 추세", "경쟁사 대비 현저히 낮음", "분기별 변동성 심화"),
                            idealRange = "30-50% (산업별 차이 큼)"
                    ),
                    FinancialGlossaryTerm(
                            term = "Operating Margin",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "영업활동으로 벌어들인 이익의 비율",
                            technicalDefinition = "Operating Income / Revenue × 100",
                            realWorldAnalogy =
                                    "가게 운영비(월세, 인건비)까지 다 빼고 남은 이익. 실제 사업이 얼마나 잘 되는지 보여줍니다.",
                            whyItMatters = "회사의 핵심 사업 효율성을 직접적으로 보여줍니다. 이자, 세금 전의 순수한 영업 성과입니다.",
                            investmentRelevance =
                                    "높고 안정적인 영업이익률은 지속 가능한 경쟁 우위를 나타냅니다. 10% 이상이면 우수합니다.",
                            example = "Microsoft의 영업이익률 42%는 클라우드 비즈니스의 높은 수익성을 보여줍니다.",
                            relatedTerms = listOf("Gross Margin", "EBITDA", "Operating Income"),
                            warningSignals = listOf("5% 이하로 하락", "연속 3분기 감소", "산업 평균 대비 절반 수준"),
                            idealRange = "10-20% 이상"
                    ),
                    FinancialGlossaryTerm(
                            term = "Net Profit Margin",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "모든 비용을 빼고 최종적으로 남는 순이익의 비율",
                            technicalDefinition = "Net Income / Revenue × 100",
                            realWorldAnalogy = "월급에서 세금, 대출 이자 다 내고 저축할 수 있는 돈의 비율. 최종 생존력을 보여줍니다.",
                            whyItMatters = "회사가 실제로 주주에게 돌려줄 수 있는 이익이 얼마인지 보여줍니다.",
                            investmentRelevance = "순이익률이 높고 증가하면 주가 상승과 배당 증가 가능성이 높습니다.",
                            example = "Meta의 순이익률 29%는 광고 비즈니스의 높은 수익성을 반영합니다.",
                            relatedTerms = listOf("EPS", "ROE", "Operating Margin"),
                            warningSignals = listOf("음수 전환", "3년 연속 감소", "경쟁사의 절반 이하"),
                            idealRange = "5-15% 이상"
                    ),
                    FinancialGlossaryTerm(
                            term = "ROE (Return on Equity)",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "주주가 투자한 돈으로 얼마나 수익을 냈는지",
                            technicalDefinition = "Net Income / Total Equity × 100",
                            realWorldAnalogy =
                                    "1억원 투자해서 1년에 2천만원 벌었다면 ROE 20%. 높을수록 돈을 효율적으로 쓴다는 뜻입니다.",
                            whyItMatters = "주주 입장에서 가장 중요한 지표. Warren Buffett도 ROE를 최우선으로 봅니다.",
                            investmentRelevance =
                                    "ROE 15% 이상이 지속되면 우수한 기업입니다. 20% 이상은 exceptional입니다.",
                            example = "NVIDIA의 ROE 123%는 AI 붐으로 인한 폭발적 수익성을 보여줍니다.",
                            relatedTerms = listOf("ROA", "Net Profit Margin", "Equity Multiplier"),
                            warningSignals = listOf("10% 이하로 하락", "부채로 부풀린 높은 ROE", "지속적 감소 추세"),
                            idealRange = "15-20% 이상"
                    ),
                    FinancialGlossaryTerm(
                            term = "ROA (Return on Assets)",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "회사가 가진 자산으로 얼마나 수익을 냈는지",
                            technicalDefinition = "Net Income / Total Assets × 100",
                            realWorldAnalogy = "100억 상당의 건물, 기계, 재고로 5억을 벌었다면 ROA 5%. 자산 활용 효율입니다.",
                            whyItMatters = "자산 대비 수익성. 부채 영향을 받지 않아 순수한 운영 효율을 볼 수 있습니다.",
                            investmentRelevance = "자산 집약적 산업(제조업)에서 중요합니다. 5% 이상이면 우수합니다.",
                            example = "Amazon의 ROA 6.5%는 물류 인프라를 효율적으로 활용함을 보여줍니다.",
                            relatedTerms = listOf("ROE", "Asset Turnover", "Total Assets"),
                            warningSignals = listOf("3% 이하", "경쟁사 대비 현저히 낮음", "자산은 늘지만 수익은 정체"),
                            idealRange = "5-10% 이상"
                    ),

                    // === LIQUIDITY METRICS ===
                    FinancialGlossaryTerm(
                            term = "Current Ratio",
                            category = GlossaryCategory.LIQUIDITY,
                            simpleDefinition = "1년 내 갚아야 할 돈 대비 1년 내 현금화 가능한 자산의 비율",
                            technicalDefinition = "Current Assets / Current Liabilities",
                            realWorldAnalogy =
                                    "다음달 카드값 100만원인데 통장에 200만원 있으면 유동비율 2.0. 여유있는 상태입니다.",
                            whyItMatters = "단기 부채를 갚을 능력이 있는지 보여줍니다. 회사가 파산하지 않을지 판단하는 1차 지표입니다.",
                            investmentRelevance = "1.5 미만이면 단기 유동성 위험이 있습니다. 갑작스런 위기 시 버티기 어렵습니다.",
                            example = "Tesla의 유동비율 1.73은 단기 부채 상환 능력이 충분함을 보여줍니다.",
                            relatedTerms = listOf("Quick Ratio", "Cash Ratio", "Working Capital"),
                            warningSignals = listOf("1.0 이하", "분기별 급격한 하락", "현금은 적고 재고만 많음"),
                            idealRange = "1.5-3.0"
                    ),
                    FinancialGlossaryTerm(
                            term = "Quick Ratio",
                            category = GlossaryCategory.LIQUIDITY,
                            simpleDefinition = "재고를 제외한 유동자산으로 단기부채를 갚을 수 있는지",
                            technicalDefinition =
                                    "(Current Assets - Inventory) / Current Liabilities",
                            realWorldAnalogy =
                                    "급하게 현금이 필요할 때 재고는 못 팔 수 있으니, 현금+외상값만으로 갚을 수 있는지 봅니다.",
                            whyItMatters = "Current Ratio보다 보수적인 지표. 재고가 많은 회사는 이 비율이 더 중요합니다.",
                            investmentRelevance =
                                    "1.0 이상이면 안전합니다. 유통업은 낮아도 괜찮지만 제조업은 1.0 이상 필수입니다.",
                            example = "Ford의 당좌비율 1.15는 재고 제외해도 단기 부채 상환 가능함을 보여줍니다.",
                            relatedTerms =
                                    listOf("Current Ratio", "Cash Ratio", "Inventory Turnover"),
                            warningSignals = listOf("0.5 이하", "재고는 늘지만 현금은 감소", "경쟁사 대비 낮음"),
                            idealRange = "1.0-2.0"
                    ),
                    FinancialGlossaryTerm(
                            term = "Cash Ratio",
                            category = GlossaryCategory.LIQUIDITY,
                            simpleDefinition = "순수 현금+현금성 자산으로 단기부채를 갚을 수 있는지",
                            technicalDefinition = "Cash and Cash Equivalents / Current Liabilities",
                            realWorldAnalogy = "지금 당장 통장에 있는 돈만으로 빚을 갚을 수 있는지. 가장 보수적인 기준입니다.",
                            whyItMatters = "극단적 위기 상황에서의 생존 능력. 금융 위기 시 가장 중요한 지표입니다.",
                            investmentRelevance = "0.5 이상이면 매우 안전합니다. 낮아도 현금 흐름이 좋으면 괜찮습니다.",
                            example = "Apple의 현금비율 0.32는 낮아 보이지만 엄청난 현금 창출력으로 문제없습니다.",
                            relatedTerms =
                                    listOf(
                                            "Cash Flow from Operations",
                                            "Quick Ratio",
                                            "Days Cash on Hand"
                                    ),
                            warningSignals = listOf("0.1 이하", "현금이 빠르게 감소", "영업 현금 흐름 마이너스"),
                            idealRange = "0.5-1.0 (현금 흐름 고려)"
                    ),

                    // === SOLVENCY METRICS ===
                    FinancialGlossaryTerm(
                            term = "Debt to Equity Ratio",
                            category = GlossaryCategory.SOLVENCY,
                            simpleDefinition = "자기 돈 대비 빚의 비율",
                            technicalDefinition = "Total Liabilities / Total Equity × 100",
                            realWorldAnalogy = "내 돈 1억으로 사업하는데 빚이 2억이면 부채비율 200%. 위험도를 나타냅니다.",
                            whyItMatters = "회사가 얼마나 빚에 의존하는지 보여줍니다. 높으면 이자 부담과 파산 위험이 커집니다.",
                            investmentRelevance = "100% 이하가 안전합니다. 200% 초과 시 금리 상승기에 매우 위험합니다.",
                            example = "Netflix의 부채비율 172%는 콘텐츠 투자를 위한 적극적 차입을 보여줍니다.",
                            relatedTerms =
                                    listOf("Interest Coverage", "Debt to Assets", "Long-term Debt"),
                            warningSignals = listOf("300% 초과", "급격한 증가", "이자보상배율 낮음"),
                            idealRange = "50-100%"
                    ),
                    FinancialGlossaryTerm(
                            term = "Interest Coverage Ratio",
                            category = GlossaryCategory.SOLVENCY,
                            simpleDefinition = "영업이익으로 이자를 몇 배나 갚을 수 있는지",
                            technicalDefinition = "Operating Income / Interest Expense",
                            realWorldAnalogy = "월급 300만원인데 대출 이자가 30만원이면 이자보상배율 10배. 여유있는 상태입니다.",
                            whyItMatters = "이자를 갚을 능력이 있는지 직접적으로 보여줍니다. 3배 이하면 위험합니다.",
                            investmentRelevance = "5배 이상이면 안전합니다. 1배 이하면 부도 위험이 높습니다.",
                            example = "AT&T의 이자보상배율 2.8배는 높은 부채 수준으로 인한 이자 부담을 보여줍니다.",
                            relatedTerms =
                                    listOf(
                                            "EBIT",
                                            "Debt Service Coverage",
                                            "Fixed Charge Coverage"
                                    ),
                            warningSignals = listOf("2배 이하", "지속적 감소", "마이너스 전환"),
                            idealRange = "5배 이상"
                    ),

                    // === EFFICIENCY METRICS ===
                    FinancialGlossaryTerm(
                            term = "Asset Turnover",
                            category = GlossaryCategory.EFFICIENCY,
                            simpleDefinition = "자산 1원당 얼마의 매출을 만드는지",
                            technicalDefinition = "Revenue / Total Assets",
                            realWorldAnalogy =
                                    "100억 기계로 500억 매출을 만들면 자산회전율 5배. 자산을 효율적으로 쓴다는 뜻입니다.",
                            whyItMatters = "같은 자산으로 더 많은 매출을 내는 회사가 효율적입니다.",
                            investmentRelevance = "소매업은 2배 이상, 제조업은 1배 이상이 일반적입니다.",
                            example = "Walmart의 자산회전율 2.4배는 효율적인 재고 관리를 보여줍니다.",
                            relatedTerms =
                                    listOf("Inventory Turnover", "Receivables Turnover", "ROA"),
                            warningSignals = listOf("경쟁사 대비 낮음", "지속적 감소", "자산은 늘지만 매출은 정체"),
                            idealRange = "산업별 차이 큼"
                    ),
                    FinancialGlossaryTerm(
                            term = "Inventory Turnover",
                            category = GlossaryCategory.EFFICIENCY,
                            simpleDefinition = "1년에 재고를 몇 번이나 팔아치우는지",
                            technicalDefinition = "Cost of Goods Sold / Average Inventory",
                            realWorldAnalogy = "빵집이 하루 재고를 3번 다 팔면 회전율 3배. 신선한 빵을 효율적으로 파는 것입니다.",
                            whyItMatters = "재고가 쌓이면 현금이 묶이고 재고 손실 위험이 커집니다.",
                            investmentRelevance = "높을수록 재고 관리가 우수합니다. 낮으면 매출 둔화나 과잉 재고 신호입니다.",
                            example = "Costco의 재고회전율 12배는 빠른 재고 순환으로 현금 흐름이 우수함을 보여줍니다.",
                            relatedTerms =
                                    listOf("Days Inventory Outstanding", "Cash Conversion Cycle"),
                            warningSignals = listOf("급격한 하락", "재고 가치 증가율 > 매출 증가율", "경쟁사의 절반"),
                            idealRange = "4-12배 (산업별 차이)"
                    ),

                    // === VALUATION METRICS ===
                    FinancialGlossaryTerm(
                            term = "EPS (Earnings Per Share)",
                            category = GlossaryCategory.VALUATION,
                            simpleDefinition = "주식 1주당 벌어들인 순이익",
                            technicalDefinition = "Net Income / Shares Outstanding",
                            realWorldAnalogy = "회사가 100억 벌고 주식이 1억주면 주당 100원. 주가의 기반이 되는 핵심 지표입니다.",
                            whyItMatters = "주가를 정당화하는 근본적 가치. EPS 성장이 장기 주가 상승을 만듭니다.",
                            investmentRelevance = "연 15% 이상 EPS 성장은 우수합니다. 주가는 장기적으로 EPS를 따라갑니다.",
                            example = "NVIDIA의 EPS $10.34 (전년비 +582%)는 AI 반도체 호황을 반영합니다.",
                            relatedTerms = listOf("P/E Ratio", "Diluted EPS", "EPS Growth Rate"),
                            warningSignals = listOf("연속 감소", "주식 희석 심화", "회계 조작 의심"),
                            idealRange = "지속적 증가 추세"
                    ),
                    FinancialGlossaryTerm(
                            term = "P/E Ratio",
                            category = GlossaryCategory.VALUATION,
                            simpleDefinition = "주가를 주당순이익으로 나눈 값, 몇 년치 이익인지",
                            technicalDefinition = "Stock Price / Earnings Per Share",
                            realWorldAnalogy = "PER 20배는 현재 주가가 20년치 이익과 같다는 뜻. 높으면 비싸거나 성장 기대입니다.",
                            whyItMatters = "주식이 비싼지 싼지 판단하는 가장 기본적인 지표입니다.",
                            investmentRelevance = "15배 이하는 저평가, 25배 이상은 고평가 가능성. 성장률과 함께 봐야 합니다.",
                            example = "S&P 500 평균 PER 20배 대비 Google PER 25배는 AI 성장 기대를 반영합니다.",
                            relatedTerms = listOf("PEG Ratio", "Forward P/E", "Shiller P/E"),
                            warningSignals = listOf("업종 평균의 2배 초과", "이익은 감소인데 PER 상승", "마이너스 EPS"),
                            idealRange = "10-20배 (성장률 고려)"
                    ),

                    // === CASH FLOW METRICS ===
                    FinancialGlossaryTerm(
                            term = "Free Cash Flow",
                            category = GlossaryCategory.CASH_FLOW,
                            simpleDefinition = "영업활동으로 번 현금에서 필수 투자를 뺀 자유 현금",
                            technicalDefinition = "Operating Cash Flow - Capital Expenditures",
                            realWorldAnalogy = "월급에서 생활비, 적금 다 내고 자유롭게 쓸 수 있는 돈. 진짜 남는 현금입니다.",
                            whyItMatters = "회계 조작이 어려운 진짜 현금 창출력. 배당과 자사주 매입의 원천입니다.",
                            investmentRelevance =
                                    "양수이고 증가하는 FCF는 강력한 매수 신호입니다. 성장하면서 FCF를 만드는 회사가 최고입니다.",
                            example = "Microsoft의 FCF $70B는 클라우드 비즈니스의 강력한 현금 창출력을 보여줍니다.",
                            relatedTerms = listOf("Operating Cash Flow", "CapEx", "FCF Yield"),
                            warningSignals = listOf("마이너스 전환", "연속 감소", "순이익은 흑자인데 FCF 적자"),
                            idealRange = "매출의 10% 이상"
                    ),
                    FinancialGlossaryTerm(
                            term = "Operating Cash Flow",
                            category = GlossaryCategory.CASH_FLOW,
                            simpleDefinition = "영업활동으로 실제 들어온 현금",
                            technicalDefinition =
                                    "Net Income + Non-cash Expenses - Working Capital Changes",
                            realWorldAnalogy = "장사해서 실제로 통장에 들어온 돈. 장부상 이익과 다를 수 있습니다.",
                            whyItMatters = "순이익은 회계상 이익이지만, 영업현금흐름은 실제 현금입니다. 더 정직한 지표입니다.",
                            investmentRelevance =
                                    "순이익보다 영업현금흐름이 크면 건강한 기업입니다. 순이익의 80% 이상이면 우수합니다.",
                            example = "Amazon의 영업현금흐름 $84B는 강력한 현금 창출력을 보여줍니다.",
                            relatedTerms =
                                    listOf(
                                            "Free Cash Flow",
                                            "Cash Conversion Ratio",
                                            "Working Capital"
                                    ),
                            warningSignals =
                                    listOf("순이익은 흑자인데 현금흐름 적자", "3년 연속 감소", "매출 증가 대비 현금 흐름 정체"),
                            idealRange = "순이익의 80% 이상"
                    ),

                    // === REPORTING CONCEPTS ===
                    FinancialGlossaryTerm(
                            term = "GAAP (Generally Accepted Accounting Principles)",
                            category = GlossaryCategory.REPORTING_CONCEPTS,
                            simpleDefinition = "미국의 공식 회계 기준",
                            technicalDefinition = "미국 재무회계기준위원회(FASB)가 정한 회계 원칙",
                            realWorldAnalogy = "모든 회사가 같은 방식으로 성적표를 쓰도록 하는 규칙. 비교 가능성을 높입니다.",
                            whyItMatters = "GAAP을 따르면 재무제표를 신뢰할 수 있고 회사간 비교가 가능합니다.",
                            investmentRelevance =
                                    "Non-GAAP 실적만 강조하는 회사는 주의해야 합니다. GAAP 기준이 더 보수적입니다.",
                            example = "Tesla는 Non-GAAP 지표도 제시하지만 GAAP 순이익이 핵심입니다.",
                            relatedTerms = listOf("IFRS", "Non-GAAP", "SEC", "10-K"),
                            warningSignals =
                                    listOf("Non-GAAP와 GAAP 차이 큼", "조정 항목이 너무 많음", "GAAP 실적 숨김"),
                            idealRange = "N/A"
                    ),
                    FinancialGlossaryTerm(
                            term = "Material Event",
                            category = GlossaryCategory.REPORTING_CONCEPTS,
                            simpleDefinition = "투자자 의사결정에 영향을 줄 수 있는 중요한 사건",
                            technicalDefinition = "합리적인 투자자가 알았다면 투자 결정이 달라질 수 있는 정보",
                            realWorldAnalogy = "집 사기 전에 꼭 알아야 할 정보(누수, 균열 등). 숨기면 법적 문제가 생깁니다.",
                            whyItMatters =
                                    "회사는 Material Event를 8-K로 4영업일 내 공시해야 합니다. 투자자 보호를 위해 필수입니다.",
                            investmentRelevance =
                                    "인수합병, CEO 교체, 대형 소송 등은 주가에 큰 영향을 줍니다. 8-K를 놓치지 마세요.",
                            example = "Twitter CEO Elon Musk 인수 제안은 8-K로 즉시 공시된 Material Event입니다.",
                            relatedTerms = listOf("8-K", "Form 4", "Insider Trading", "SEC"),
                            warningSignals = listOf("공시 지연", "모호한 표현", "8-K 빈도 급증"),
                            idealRange = "N/A"
                    ),
                    FinancialGlossaryTerm(
                            term = "MD&A (Management Discussion and Analysis)",
                            category = GlossaryCategory.REPORTING_CONCEPTS,
                            simpleDefinition = "경영진이 직접 설명하는 회사 상황과 전망",
                            technicalDefinition = "10-K/10-Q의 Item 7(연간) 또는 Item 2(분기)로 경영진의 재무 분석",
                            realWorldAnalogy = "성적표의 담임 선생님 코멘트. 숫자 뒤의 스토리를 알 수 있습니다.",
                            whyItMatters = "경영진이 보는 회사의 강점, 약점, 기회, 위협을 알 수 있습니다. 미래 전망도 포함됩니다.",
                            investmentRelevance =
                                    "MD&A에서 'uncertainty', 'headwind', 'challenge' 같은 단어의 빈도가 높아지면 주의해야 합니다.",
                            example =
                                    "Meta의 MD&A에서 'AI investment'와 'metaverse'의 언급 빈도 변화가 전략 전환을 보여줍니다.",
                            relatedTerms =
                                    listOf(
                                            "10-K",
                                            "10-Q",
                                            "Risk Factors",
                                            "Forward-Looking Statements"
                                    ),
                            warningSignals = listOf("모호한 표현 증가", "전년 대비 설명 축소", "리스크 과소평가"),
                            idealRange = "N/A"
                    ),
                    FinancialGlossaryTerm(
                            term = "EBITDA",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "이자, 세금, 감가상각비를 빼기 전 영업이익",
                            technicalDefinition =
                                    "Earnings Before Interest, Taxes, Depreciation, and Amortization",
                            realWorldAnalogy =
                                    "가게 운영으로 번 돈에서 이자, 세금, 건물/기계 가치 하락을 빼기 전 순수 영업 현금. 진짜 벌이를 보여줍니다.",
                            whyItMatters =
                                    "회계상 비용이지만 현금 유출이 없는 감가상각을 제외해 실제 현금 창출력에 가깝습니다. 업종간 비교에 유용합니다.",
                            investmentRelevance =
                                    "EBITDA margin이 높고 증가하면 강력한 수익성을 의미합니다. 단, Free Cash Flow와 차이가 크면 주의해야 합니다.",
                            example = "Netflix EBITDA $6.9B는 콘텐츠 상각 전 강력한 현금 창출력을 보여줍니다.",
                            relatedTerms =
                                    listOf(
                                            "Adjusted EBITDA",
                                            "Operating Income",
                                            "Free Cash Flow",
                                            "EV/EBITDA"
                                    ),
                            warningSignals =
                                    listOf(
                                            "EBITDA는 흑자인데 FCF 적자",
                                            "CapEx가 감가상각비의 2배 초과",
                                            "이자비용이 EBITDA의 30% 초과"
                                    ),
                            idealRange = "매출의 15-25%"
                    ),
                    FinancialGlossaryTerm(
                            term = "Adjusted EBITDA",
                            category = GlossaryCategory.PROFITABILITY,
                            simpleDefinition = "이자, 세금, 감가상각비, 특별 항목을 빼기 전 이익",
                            technicalDefinition =
                                    "Earnings Before Interest, Taxes, Depreciation, Amortization + Adjustments",
                            realWorldAnalogy = "일시적 비용을 제외한 진짜 벌이. 하지만 회사가 마음대로 조정할 수 있어 주의 필요합니다.",
                            whyItMatters =
                                    "Non-GAAP 지표로 현금 흐름 proxy입니다. 회사간 비교와 인수합병 가치 평가에 사용됩니다.",
                            investmentRelevance =
                                    "EBITDA margin 20% 이상이면 우수하지만, GAAP 순이익과 차이가 크면 조작 의심해야 합니다.",
                            example = "WeWork는 'Community Adjusted EBITDA'로 마케팅비를 제외해 비판받았습니다.",
                            relatedTerms =
                                    listOf(
                                            "EBIT",
                                            "Operating Income",
                                            "Non-GAAP",
                                            "Free Cash Flow"
                                    ),
                            warningSignals =
                                    listOf("GAAP 이익과 격차 확대", "조정 항목이 매번 다름", "EBITDA는 흑자인데 FCF 적자"),
                            idealRange = "매출의 20% 이상"
                    ),

                    // === RISK INDICATORS ===
                    FinancialGlossaryTerm(
                            term = "Working Capital",
                            category = GlossaryCategory.RISK_INDICATORS,
                            simpleDefinition = "단기 자산에서 단기 부채를 뺀 운전자금",
                            technicalDefinition = "Current Assets - Current Liabilities",
                            realWorldAnalogy = "당장 쓸 수 있는 돈에서 당장 갚아야 할 빚을 뺀 것. 사업 운영에 쓸 여유 자금입니다.",
                            whyItMatters = "음수면 단기 유동성 위기입니다. 양수라도 급격히 감소하면 주의해야 합니다.",
                            investmentRelevance = "운전자본이 마이너스로 전환되면 부도 위험이 급증합니다.",
                            example = "Tesla의 운전자본 $26B는 공격적 성장에도 유동성이 충분함을 보여줍니다.",
                            relatedTerms =
                                    listOf(
                                            "Current Ratio",
                                            "Cash Conversion Cycle",
                                            "Net Working Capital"
                                    ),
                            warningSignals = listOf("마이너스 전환", "급격한 감소", "매출은 늘지만 운전자본 감소"),
                            idealRange = "매출의 10-20%"
                    ),
                    FinancialGlossaryTerm(
                            term = "Burn Rate",
                            category = GlossaryCategory.RISK_INDICATORS,
                            simpleDefinition = "월별 현금 소진 속도",
                            technicalDefinition = "Monthly Operating Cash Flow (negative)",
                            realWorldAnalogy = "매달 통장에서 빠져나가는 돈. 스타트업이 망하기 전까지 남은 '활주로' 길이입니다.",
                            whyItMatters =
                                    "현금이 얼마나 빨리 줄어드는지 보여줍니다. 적자 기업은 Burn Rate > 매출 증가율이면 위험합니다.",
                            investmentRelevance =
                                    "현금 ÷ Burn Rate = Runway (생존 개월). 12개월 미만이면 증자나 파산 위험입니다.",
                            example = "Uber의 월 Burn Rate $1B였으나 수익성 개선으로 플러스 전환했습니다.",
                            relatedTerms =
                                    listOf("Cash Runway", "Operating Cash Flow", "Capital Raise"),
                            warningSignals =
                                    listOf("Burn Rate 가속", "Runway 12개월 미만", "추가 자금 조달 실패"),
                            idealRange = "흑자 전환 경로 명확"
                    ),
                    FinancialGlossaryTerm(
                            term = "Goodwill Impairment",
                            category = GlossaryCategory.RISK_INDICATORS,
                            simpleDefinition = "과거 인수한 회사의 가치가 떨어져서 장부에서 깎아내는 것",
                            technicalDefinition =
                                    "Reduction in the carrying value of goodwill when fair value < book value",
                            realWorldAnalogy =
                                    "10억에 산 가게가 실제론 5억 가치밖에 안 되면 5억 손실 인식. 과거 인수 실패를 인정하는 것입니다.",
                            whyItMatters = "대규모 손상차손은 순이익을 급격히 낮추고, 과거 M&A가 실패했음을 의미합니다.",
                            investmentRelevance =
                                    "Goodwill이 총자산의 30% 이상이면 손상차손 리스크가 큽니다. 경영진 판단 능력 의심됩니다.",
                            example = "GE의 $22B Goodwill 손상차손은 잘못된 에너지 부문 인수를 반영합니다.",
                            relatedTerms = listOf("Acquisition", "Intangible Assets", "Fair Value"),
                            warningSignals =
                                    listOf("반복적 손상차손", "Goodwill 비율 30% 초과", "M&A 직후 손상차손"),
                            idealRange = "총자산의 20% 이하"
                    ),

                    // === ADDITIONAL VALUATION METRICS ===
                    FinancialGlossaryTerm(
                            term = "PEG Ratio",
                            category = GlossaryCategory.VALUATION,
                            simpleDefinition = "PER을 성장률로 나눈 값, 성장 대비 주가가 비싼지",
                            technicalDefinition = "P/E Ratio / Earnings Growth Rate",
                            realWorldAnalogy = "PER 30배라도 성장률이 30%면 PEG 1.0. 성장을 고려하면 적정 가격입니다.",
                            whyItMatters = "PER만으로는 성장주를 제대로 평가할 수 없습니다. PEG가 더 공정한 비교를 제공합니다.",
                            investmentRelevance =
                                    "PEG < 1.0은 저평가, PEG > 2.0은 고평가 가능성. 성장주 투자의 핵심 지표입니다.",
                            example = "NVIDIA PER 60배 ÷ 성장률 80% = PEG 0.75, 성장 대비 저평가 신호입니다.",
                            relatedTerms = listOf("P/E Ratio", "EPS Growth", "Forward P/E"),
                            warningSignals = listOf("PEG 3.0 이상", "성장률 둔화인데 PEG 상승", "음수 성장률"),
                            idealRange = "0.5-1.5"
                    ),
                    FinancialGlossaryTerm(
                            term = "Price to Book Ratio (P/B)",
                            category = GlossaryCategory.VALUATION,
                            simpleDefinition = "주가를 주당 순자산으로 나눈 값",
                            technicalDefinition = "Stock Price / Book Value Per Share",
                            realWorldAnalogy =
                                    "회사 청산하면 주당 1만원 받는데 주가가 1.5만원이면 PBR 1.5배. 1배 이하면 청산가치 이하입니다.",
                            whyItMatters = "자산 대비 주가 수준을 보여줍니다. 은행, 부동산 같은 자산 중심 산업에서 중요합니다.",
                            investmentRelevance =
                                    "PBR 1배 이하는 저평가 가능성. 하지만 0.5배 이하는 구조적 문제일 수 있습니다.",
                            example = "JP Morgan PBR 1.3배는 은행업 평균 수준으로 적정 평가를 보여줍니다.",
                            relatedTerms = listOf("Book Value", "Tangible Book Value", "ROE"),
                            warningSignals = listOf("지속적으로 1배 이하", "자산 가치 과대평가", "무형자산 비중 높음"),
                            idealRange = "1.0-3.0 (산업별 차이)"
                    ),
                    FinancialGlossaryTerm(
                            term = "Dividend Yield",
                            category = GlossaryCategory.MARKET_METRICS,
                            simpleDefinition = "주가 대비 연간 배당금 비율",
                            technicalDefinition = "Annual Dividend Per Share / Stock Price × 100",
                            realWorldAnalogy = "주가 10만원인데 연간 배당 3천원이면 배당수익률 3%. 은행 금리처럼 현금 수익입니다.",
                            whyItMatters = "주식 보유만으로 얻는 현금 수익률. 안정적 현금 흐름을 원하는 투자자에게 중요합니다.",
                            investmentRelevance =
                                    "배당수익률 3% 이상이면 매력적. 하지만 너무 높으면 주가 급락이나 배당 삭감 징조일 수 있습니다.",
                            example = "AT&T 배당수익률 7%는 안정적 통신사업 현금흐름을 반영합니다.",
                            relatedTerms =
                                    listOf("Dividend Payout Ratio", "Dividend Growth Rate", "FCF"),
                            warningSignals =
                                    listOf("10% 초과 (지속 불가능)", "배당성향 100% 초과", "FCF가 배당보다 적음"),
                            idealRange = "2-5%"
                    ),
                    FinancialGlossaryTerm(
                            term = "Beta",
                            category = GlossaryCategory.MARKET_METRICS,
                            simpleDefinition = "시장 대비 주가 변동성",
                            technicalDefinition = "Covariance(Stock, Market) / Variance(Market)",
                            realWorldAnalogy =
                                    "베타 1.5면 시장이 10% 오를 때 이 주식은 15% 오릅니다. 변동성이 크다는 뜻입니다.",
                            whyItMatters = "투자 위험도를 수치화합니다. 베타가 높으면 수익도 크지만 손실도 큽니다.",
                            investmentRelevance = "베타 < 1은 방어주, 베타 > 1은 공격주. 포트폴리오 리스크 관리에 필수입니다.",
                            example = "Tesla 베타 2.0은 시장 대비 2배 변동성으로 고위험 고수익 특성을 보여줍니다.",
                            relatedTerms = listOf("Alpha", "Sharpe Ratio", "Volatility"),
                            warningSignals = listOf("베타 3.0 이상", "급격한 베타 변화", "음수 베타"),
                            idealRange = "0.8-1.5 (투자 성향별 차이)"
                    ),
                    FinancialGlossaryTerm(
                            term = "Market Capitalization",
                            category = GlossaryCategory.MARKET_METRICS,
                            simpleDefinition = "회사의 시장 가치, 모든 주식의 총 가격",
                            technicalDefinition = "Stock Price × Shares Outstanding",
                            realWorldAnalogy =
                                    "주가 10만원 × 100만주 = 시가총액 1,000억원. 회사를 통째로 사려면 이만큼 필요합니다.",
                            whyItMatters = "회사 크기를 나타냅니다. 시가총액이 크면 안정적이지만 성장 여력은 작습니다.",
                            investmentRelevance =
                                    "대형주(10조 이상)는 안정적, 소형주(1조 이하)는 고성장 가능. 투자 전략이 달라집니다.",
                            example = "Apple 시가총액 $3T는 세계 최대 기업으로 안정성을 보여줍니다.",
                            relatedTerms =
                                    listOf("Shares Outstanding", "Enterprise Value", "Float"),
                            warningSignals = listOf("급격한 시총 감소", "유동주식 부족", "상장폐지 기준 근접"),
                            idealRange = "투자 전략별 차이"
                    ),
                    FinancialGlossaryTerm(
                            term = "Enterprise Value (EV)",
                            category = GlossaryCategory.VALUATION,
                            simpleDefinition = "부채까지 포함한 회사의 진짜 인수 가격",
                            technicalDefinition = "Market Cap + Total Debt - Cash and Equivalents",
                            realWorldAnalogy = "집값 10억에 대출 3억 있으면 실제 인수 비용은 13억 - 통장 1억 = 12억원입니다.",
                            whyItMatters = "부채가 많은 회사는 시가총액만으로 평가하면 안 됩니다. EV가 더 정확한 가치입니다.",
                            investmentRelevance = "EV/EBITDA 비율로 기업 가치를 더 공정하게 비교할 수 있습니다.",
                            example = "Tesla EV $800B = 시총 $700B + 부채 $150B - 현금 $50B",
                            relatedTerms = listOf("EV/EBITDA", "Market Cap", "Net Debt"),
                            warningSignals = listOf("EV가 시총의 2배 초과", "순부채 비중 과다", "현금 고갈"),
                            idealRange = "시총의 1.0-1.5배"
                    ),

                    // === INCOME STATEMENT ITEMS ===
                    FinancialGlossaryTerm(
                            term = "Revenue (Sales)",
                            category = GlossaryCategory.INCOME_STATEMENT,
                            simpleDefinition = "회사가 제품/서비스를 팔아서 벌어들인 총 매출",
                            technicalDefinition =
                                    "Total income from goods sold or services provided during a period",
                            realWorldAnalogy = "가게에서 하루 총 판매 금액. 비용은 빼기 전 순수 매출액입니다.",
                            whyItMatters = "회사의 성장성을 가장 먼저 보여줍니다. 매출이 늘어야 이익도 커집니다.",
                            investmentRelevance =
                                    "연 15% 이상 매출 성장은 우수합니다. 매출 없는 이익은 일회성일 가능성이 높습니다.",
                            example = "Amazon 매출 $574B는 전자상거래 지배력을 보여줍니다.",
                            relatedTerms = listOf("Revenue Growth", "Gross Margin", "Top Line"),
                            warningSignals = listOf("매출 감소", "성장률 둔화", "일회성 매출 의존"),
                            idealRange = "지속적 증가 추세"
                    ),
                    FinancialGlossaryTerm(
                            term = "COGS (Cost of Goods Sold)",
                            category = GlossaryCategory.INCOME_STATEMENT,
                            simpleDefinition = "제품을 만드는데 든 직접 비용",
                            technicalDefinition =
                                    "Direct costs of producing goods sold (materials, labor, manufacturing)",
                            realWorldAnalogy = "빵집이 빵 만드는데 든 밀가루, 계란, 인건비. 판매비나 광고비는 제외입니다.",
                            whyItMatters = "매출에서 COGS를 빼면 매출총이익입니다. COGS가 높으면 이익률이 낮습니다.",
                            investmentRelevance = "COGS 비율이 낮을수록 가격 결정력이 강합니다. 원자재 가격에 민감합니다.",
                            example = "Ford COGS 비율 88%는 자동차 산업의 낮은 마진을 반영합니다.",
                            relatedTerms =
                                    listOf("Gross Margin", "Operating Expenses", "Inventory"),
                            warningSignals = listOf("급격한 COGS 증가", "매출은 정체인데 COGS 상승", "재고 손실 증가"),
                            idealRange = "매출의 50-70% (산업별 차이)"
                    ),
                    FinancialGlossaryTerm(
                            term = "SG&A (Selling, General & Administrative)",
                            category = GlossaryCategory.INCOME_STATEMENT,
                            simpleDefinition = "판매비와 관리비, 영업을 위한 간접 비용",
                            technicalDefinition =
                                    "Operating expenses not directly tied to production (marketing, salaries, rent)",
                            realWorldAnalogy = "가게 월세, 직원 급여, 광고비. 물건 만드는 비용은 아니지만 장사에 필수적입니다.",
                            whyItMatters = "SG&A 효율성은 경영 능력을 보여줍니다. 무분별한 확장은 SG&A를 급증시킵니다.",
                            investmentRelevance =
                                    "SG&A 비율이 낮고 안정적이면 효율적 경영입니다. 매출 대비 20% 이하가 우수합니다.",
                            example = "Costco SG&A 비율 10%는 극도로 효율적인 운영을 보여줍니다.",
                            relatedTerms =
                                    listOf("Operating Expenses", "Operating Margin", "EBITDA"),
                            warningSignals = listOf("매출보다 빠른 SG&A 증가", "30% 초과", "비용 통제 실패"),
                            idealRange = "매출의 15-25%"
                    ),
                    FinancialGlossaryTerm(
                            term = "R&D Expense",
                            category = GlossaryCategory.INCOME_STATEMENT,
                            simpleDefinition = "연구개발에 투자하는 비용",
                            technicalDefinition =
                                    "Costs incurred for research and development of new products/technologies",
                            realWorldAnalogy = "신제품 개발, 기술 연구에 쓰는 돈. 당장 수익은 없지만 미래 성장의 씨앗입니다.",
                            whyItMatters = "R&D 투자는 미래 경쟁력을 만듭니다. 기술 기업은 R&D가 생존의 핵심입니다.",
                            investmentRelevance =
                                    "매출 대비 R&D 비율이 높으면 혁신 중심 기업입니다. 일관된 R&D 투자는 긍정적 신호입니다.",
                            example = "Pfizer R&D 비율 15%는 신약 개발에 적극 투자함을 보여줍니다.",
                            relatedTerms = listOf("Patent", "Innovation", "Intangible Assets"),
                            warningSignals = listOf("R&D 급감", "경쟁사 대비 현저히 낮음", "R&D 효율 저하"),
                            idealRange = "기술업 10-20%, 제약 15-25%"
                    ),

                    // === BALANCE SHEET ITEMS ===
                    FinancialGlossaryTerm(
                            term = "Total Assets",
                            category = GlossaryCategory.BALANCE_SHEET,
                            simpleDefinition = "회사가 소유한 모든 자산의 총합",
                            technicalDefinition =
                                    "Sum of current assets, fixed assets, and intangible assets",
                            realWorldAnalogy = "개인 재산의 부동산, 현금, 자동차, 주식 다 합친 것. 회사의 전체 규모입니다.",
                            whyItMatters = "총자산은 회사의 크기와 운영 규모를 보여줍니다. 자산이 효율적으로 쓰이는지가 중요합니다.",
                            investmentRelevance =
                                    "자산은 늘지만 수익은 정체면 비효율적 투자입니다. Asset Turnover로 효율성을 봐야 합니다.",
                            example = "Berkshire Hathaway 총자산 $1T는 거대 지주회사의 규모를 보여줍니다.",
                            relatedTerms = listOf("Current Assets", "Fixed Assets", "ROA"),
                            warningSignals = listOf("자산 증가 > 매출 증가율", "자산 가치 과대평가", "유동성 부족"),
                            idealRange = "N/A (산업별 차이)"
                    ),
                    FinancialGlossaryTerm(
                            term = "Accounts Receivable",
                            category = GlossaryCategory.BALANCE_SHEET,
                            simpleDefinition = "외상으로 팔아서 아직 못 받은 돈",
                            technicalDefinition =
                                    "Money owed by customers for goods/services sold on credit",
                            realWorldAnalogy = "물건은 넘겨줬는데 돈은 나중에 받기로 한 것. 현금화까지 시간이 걸립니다.",
                            whyItMatters = "매출채권이 급증하면 현금 흐름 문제가 생깁니다. 회수가 안 되면 대손상각 위험이 있습니다.",
                            investmentRelevance = "매출채권 회전율이 높을수록 좋습니다. 매출 증가보다 빠르게 늘면 경고 신호입니다.",
                            example = "Tesla 매출채권 회전율 20배는 빠른 현금 회수를 보여줍니다.",
                            relatedTerms =
                                    listOf("Days Sales Outstanding", "Bad Debt", "Cash Conversion"),
                            warningSignals = listOf("회수 기간 90일 초과", "대손충당금 급증", "매출 대비 과다"),
                            idealRange = "매출의 10-20%"
                    ),
                    FinancialGlossaryTerm(
                            term = "Inventory",
                            category = GlossaryCategory.BALANCE_SHEET,
                            simpleDefinition = "아직 팔지 못한 재고 상품",
                            technicalDefinition =
                                    "Goods available for sale or raw materials for production",
                            realWorldAnalogy = "창고에 쌓인 물건. 너무 많으면 현금이 묶이고, 너무 적으면 품절로 매출을 놓칩니다.",
                            whyItMatters = "재고 관리는 수익성과 현금 흐름에 직접 영향을 줍니다. 과잉 재고는 손실입니다.",
                            investmentRelevance = "재고회전율이 높을수록 효율적입니다. 재고가 급증하면 매출 둔화 징조입니다.",
                            example = "Zara 재고회전율 6배는 Fast Fashion의 빠른 재고 순환을 보여줍니다.",
                            relatedTerms = listOf("Inventory Turnover", "COGS", "Obsolescence"),
                            warningSignals = listOf("재고 증가 > 매출 증가", "재고자산 손상", "회전율 급락"),
                            idealRange = "매출의 15-30% (산업별 차이)"
                    ),
                    FinancialGlossaryTerm(
                            term = "Retained Earnings",
                            category = GlossaryCategory.BALANCE_SHEET,
                            simpleDefinition = "창업 이후 쌓아온 누적 순이익",
                            technicalDefinition =
                                    "Cumulative net income retained in business after dividends",
                            realWorldAnalogy = "급여에서 지출하고 저축한 돈이 쌓인 것. 회사가 재투자에 쓸 수 있는 돈입니다.",
                            whyItMatters = "많을수록 재무적으로 건강합니다. 음수면 누적 적자로 재무 구조가 약합니다.",
                            investmentRelevance = "이익잉여금이 증가하면 자생력이 강한 회사입니다. 음수는 위험 신호입니다.",
                            example = "Microsoft 이익잉여금 $120B는 수십년 수익 창출의 결과입니다.",
                            relatedTerms =
                                    listOf("Shareholders Equity", "Dividend Policy", "Book Value"),
                            warningSignals = listOf("음수 전환", "지속적 감소", "자본잠식"),
                            idealRange = "총자산의 30% 이상"
                    ),

                    // === CASH FLOW ITEMS ===
                    FinancialGlossaryTerm(
                            term = "CapEx (Capital Expenditures)",
                            category = GlossaryCategory.CASH_FLOW,
                            simpleDefinition = "공장, 설비, 부동산 등 고정자산 구입 비용",
                            technicalDefinition =
                                    "Cash spent on acquiring or upgrading physical assets",
                            realWorldAnalogy = "가게 확장을 위해 건물 매입, 기계 구입. 한 번 쓰면 오래 쓰는 투자입니다.",
                            whyItMatters =
                                    "CapEx가 크면 성장 중이지만 현금 흐름은 나빠집니다. 유지 CapEx vs 성장 CapEx를 구분해야 합니다.",
                            investmentRelevance = "CapEx가 감가상각비보다 적으면 투자 부족, 훨씬 크면 공격적 확장입니다.",
                            example = "Tesla CapEx $7B는 기가팩토리 확장에 대규모 투자함을 보여줍니다.",
                            relatedTerms = listOf("Free Cash Flow", "Depreciation", "PP&E"),
                            warningSignals =
                                    listOf("과도한 CapEx 대비 수익 정체", "유지보수 CapEx 부족", "급격한 CapEx 증가"),
                            idealRange = "매출의 5-15% (산업별 차이)"
                    ),
                    FinancialGlossaryTerm(
                            term = "Cash Conversion Cycle (CCC)",
                            category = GlossaryCategory.CASH_FLOW,
                            simpleDefinition = "재고 구매부터 현금 회수까지 걸리는 일수",
                            technicalDefinition = "Days Inventory + Days Receivable - Days Payable",
                            realWorldAnalogy = "원자재 사서 제품 만들고 팔아서 돈 받기까지. 짧을수록 현금이 빨리 돕니다.",
                            whyItMatters = "CCC가 짧으면 운전자본이 적어도 됩니다. 음수면 매우 효율적인 비즈니스 모델입니다.",
                            investmentRelevance =
                                    "CCC 30일 이하는 우수, 음수는 exceptional. Dell, Amazon 같은 회사의 강점입니다.",
                            example = "Amazon CCC -20일은 고객 돈을 먼저 받고 공급자에게 나중에 지불함을 보여줍니다.",
                            relatedTerms = listOf("Working Capital", "DIO", "DSO", "DPO"),
                            warningSignals = listOf("90일 초과", "지속적 증가", "산업 평균 대비 2배"),
                            idealRange = "30-60일 (음수면 최고)"
                    )
            )

    fun getAllTerms(): List<FinancialGlossaryTerm> = glossaryTerms

    fun getTermByName(termName: String): FinancialGlossaryTerm? =
            glossaryTerms.find { it.term.equals(termName, ignoreCase = true) }

    fun getTermsByCategory(category: GlossaryCategory): List<FinancialGlossaryTerm> =
            glossaryTerms.filter { it.category == category }

    fun searchTerms(query: String): List<FinancialGlossaryTerm> {
        val lowerQuery = query.lowercase()
        return glossaryTerms.filter {
            it.term.lowercase().contains(lowerQuery) ||
                    it.simpleDefinition.lowercase().contains(lowerQuery) ||
                    it.technicalDefinition.lowercase().contains(lowerQuery) ||
                    it.relatedTerms.any { related -> related.lowercase().contains(lowerQuery) }
        }
    }
}
