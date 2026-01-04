package papyrus.core.service

import papyrus.core.model.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Report-type specific analysis following AGENTS.md principles
 * Provides meaningful, actionable insights tailored to each SEC filing type
 */
object ReportTypeAnalyzer {
    
    /** Analyze 10-K (Annual Report) with year-over-year insights */
    fun analyze10K(analysis: FinancialAnalysis, metrics: List<ExtendedFinancialMetric>): ReportSpecificInsights {
        return ReportSpecificInsights(
            reportType = "10-K",
            keyFocus = listOf(
                "연간 트렌드 및 성장 패턴",
                "장기 재무 건전성",
                "경영진 전략 및 비전 (MD&A)",
                "위험 요소 (Risk Factors)",
                "감사 의견 및 내부통제"
            ),
            criticalSections = mapOf(
                "Part I, Item 1 - Business" to "회사의 비즈니스 모델, 경쟁 우위, 시장 포지션 파악",
                "Part I, Item 1A - Risk Factors" to "투자 리스크 요인 15-30개 나열, 우선순위 확인",
                "Part II, Item 7 - MD&A" to "경영진 시각의 재무 성과 분석, 전년 대비 변화",
                "Part II, Item 8 - Financial Statements" to "감사 받은 재무제표, 가장 신뢰할 수 있는 숫자",
                "Part IV, Item 15 - Exhibits" to "계약서, 임원 보상, 주요 합의사항"
            ),
            recommendedActions = listOf(
                "전년 10-K와 비교하여 Risk Factors 변화 확인",
                "MD&A에서 'uncertainty', 'challenge', 'headwind' 키워드 빈도 체크",
                "재무제표 주석(Notes)에서 비정상적 회계 처리 찾기",
                "경쟁사 10-K와 비교하여 상대적 포지션 파악",
                "감사 의견이 'Unqualified (적정)'인지 확인"
            ),
            redFlags = listOf(
                "Goodwill 손상차손 발생",
                "감사 의견이 'Qualified' 또는 'Adverse'",
                "Going Concern 경고 (계속기업 의문)",
                "CEO/CFO 교체가 10-K 제출 직전 발생",
                "전년 대비 Risk Factors 항목이 2배 이상 증가",
                "특별 손실(Special Charges)이 3년 연속 발생"
            ),
            timelinessCheck = TimelinessInfo(
                deadline = "회계연도 종료 후 60일 (대형주) 또는 90일 (소형주)",
                lateFilingSignal = "지연 제출은 내부 통제 문제나 회계 분쟁 신호"
            )
        )
    }
    
    /** Analyze 10-Q (Quarterly Report) with QoQ and YoY insights */
    fun analyze10Q(analysis: FinancialAnalysis, metrics: List<ExtendedFinancialMetric>): ReportSpecificInsights {
        return ReportSpecificInsights(
            reportType = "10-Q",
            keyFocus = listOf(
                "분기별 추세 (QoQ, YoY)",
                "계절성 패턴 파악",
                "단기 유동성 변화",
                "가이던스 대비 실적",
                "경영진 코멘터리 톤 변화"
            ),
            criticalSections = mapOf(
                "Part I, Item 1 - Financial Statements" to "미감사 재무제표, 검토만 받음 (감사 아님)",
                "Part I, Item 2 - MD&A" to "분기 실적 설명, 전 분기 및 전년 동기 대비",
                "Part I, Item 4 - Controls" to "내부통제 중요 변경사항",
                "Part II, Item 1A - Risk Factors" to "10-K 대비 추가된 리스크만 업데이트"
            ),
            recommendedActions = listOf(
                "직전 분기(QoQ)와 비교하여 개선/악화 트렌드 파악",
                "전년 동기(YoY)와 비교하여 성장률 계산",
                "4개 분기 합산하여 연간 추정치 도출 (TTM)",
                "가이던스 제시 여부 확인 (상향/하향/유지)",
                "Earnings Call 일정 확인하여 경영진 발언 청취"
            ),
            redFlags = listOf(
                "연속 3분기 매출 감소",
                "매출은 늘지만 영업현금흐름 감소",
                "재고 증가율 > 매출 증가율 (재고 쌓임)",
                "MD&A에서 낙관론 사라지고 보수적 표현 증가",
                "전년 동기 대비 실적 가이던스 하향",
                "DSO(매출채권회수기간) 급증"
            ),
            timelinessCheck = TimelinessInfo(
                deadline = "분기 종료 후 40일 (대형주) 또는 45일 (소형주)",
                lateFilingSignal = "분기 실적 악화나 회계 이슈 가능성"
            )
        )
    }
    
    /** Analyze 8-K (Current Report) for material events */
    fun analyze8K(analysis: FinancialAnalysis, content: String): ReportSpecificInsights {
        val eventTypes = detect8KEventType(content)
        
        return ReportSpecificInsights(
            reportType = "8-K",
            keyFocus = listOf(
                "발생한 중요 사건 유형",
                "사건의 재무적 영향",
                "타이밍 및 시장 반응",
                "연속성 패턴 (반복 사건)"
            ),
            criticalSections = mapOf(
                "Item 1.01 - Entry into Material Agreement" to "중요 계약 체결 (공급, 파트너십 등)",
                "Item 1.02 - Termination of Material Agreement" to "계약 해지 (악재 가능성)",
                "Item 2.01 - Completion of Acquisition" to "인수합병 완료, 가격 및 조건",
                "Item 2.02 - Results of Operations" to "실적 발표 (Earnings Release)",
                "Item 2.03 - Creation of Direct Financial Obligation" to "차입금 발생",
                "Item 2.04 - Triggering Events" to "채무불이행, 계약 위반 등",
                "Item 5.02 - Departure/Election of Directors" to "CEO, CFO, 이사 교체",
                "Item 7.01 - Regulation FD Disclosure" to "공정 공시 (보도자료, 프레젠테이션)",
                "Item 8.01 - Other Events" to "기타 중요 사건",
                "Item 9.01 - Financial Statements and Exhibits" to "인수 대상 재무제표, 프로포마"
            ),
            recommendedActions = listOf(
                *eventTypes.map { "[$it] 사건의 주가 영향 분석" }.toTypedArray(),
                "동종 업계 유사 사건 발생 시 주가 변동 패턴 참고",
                "Exhibit에 첨부된 계약서, 프레젠테이션 상세 검토",
                "Item 2.02 (실적)는 Earnings Call과 연계하여 분석",
                "Item 5.02 (임원 교체)는 이유 파악 (자발적 vs 강제)",
                "8-K 제출 빈도가 높으면 변동성 큰 회사"
            ),
            redFlags = listOf(
                "Item 2.04 (채무불이행 트리거)",
                "Item 4.01 (감사인 교체, 특히 의견 불일치로)",
                "Item 4.02 (감사받지 않은 재무제표 신뢰 불가)",
                "Item 5.02에서 CFO 급작스런 사임",
                "Item 8.01에서 중요 소송 패소",
                "한 달에 8-K 5개 이상 제출 (혼란스러운 경영)"
            ),
            timelinessCheck = TimelinessInfo(
                deadline = "사건 발생 후 4영업일 이내",
                lateFilingSignal = "지연은 정보 숨기기 시도 가능성"
            )
        )
    }
    
    /** Analyze DEF 14A (Proxy Statement) for governance */
    fun analyzeDEF14A(analysis: FinancialAnalysis): ReportSpecificInsights {
        return ReportSpecificInsights(
            reportType = "DEF 14A",
            keyFocus = listOf(
                "임원 보상 (CEO Pay Ratio)",
                "이사회 구성 및 독립성",
                "주주 의결 안건",
                "지배구조 품질"
            ),
            criticalSections = mapOf(
                "Proposal 1 - Election of Directors" to "이사 선임, 독립 이사 비율 확인",
                "Proposal 2 - Ratification of Auditors" to "감사인 승인, 교체 시 이유 확인",
                "Proposal 3 - Say on Pay" to "임원 보상 승인, 주주 동의율",
                "Executive Compensation" to "CEO 연봉, 스톡옵션, 성과 연동 여부",
                "Compensation Discussion & Analysis" to "보상 철학 및 정당성 설명",
                "Security Ownership" to "내부자 지분율, Skin in the game"
            ),
            recommendedActions = listOf(
                "CEO Pay Ratio가 업계 평균 대비 과도한지 확인",
                "스톡옵션 행사가가 현재 주가보다 낮으면 희석 리스크",
                "독립 이사 비율 50% 이상인지 확인 (지배구조)",
                "이사회 다양성 (연령, 성별, 전문성)",
                "내부자 지분율 5% 이상이면 이해관계 정렬",
                "주주 제안(Shareholder Proposal) 내용 및 경영진 입장"
            ),
            redFlags = listOf(
                "CEO 보상이 매출 대비 0.5% 초과",
                "실적 악화인데 임원 보수 증가",
                "Say on Pay 반대표 30% 이상",
                "내부자 지분 1% 미만 (이해관계 약함)",
                "이사회 평균 재임 기간 15년 이상 (경직)",
                "Golden Parachute(퇴직금)가 과도함"
            ),
            timelinessCheck = TimelinessInfo(
                deadline = "주주총회 40일 전",
                lateFilingSignal = "주주총회 일정 변경이나 내부 갈등 가능"
            )
        )
    }
    
    /** Analyze 20-F (Foreign Company Annual Report) */
    fun analyze20F(analysis: FinancialAnalysis): ReportSpecificInsights {
        return ReportSpecificInsights(
            reportType = "20-F",
            keyFocus = listOf(
                "IFRS vs US GAAP 차이",
                "환율 리스크",
                "지정학적 리스크",
                "본국 규제 환경"
            ),
            criticalSections = mapOf(
                "Item 3 - Key Information" to "위험 요소, 과거 주가, 배당 정책",
                "Item 4 - Company Information" to "사업 개요, 조직도",
                "Item 5 - Operating and Financial Review" to "MD&A와 유사, 경영진 분석",
                "Item 17/18 - Financial Statements" to "IFRS 또는 자국 GAAP 재무제표",
                "Item 18 Note - US GAAP Reconciliation" to "미국 GAAP과의 차이 조정"
            ),
            recommendedActions = listOf(
                "IFRS와 US GAAP 조정표에서 큰 차이 항목 파악",
                "본국 통화 기준 재무제표와 USD 기준 비교",
                "ADR 스폰서 레벨 확인 (Level 3이 가장 투명)",
                "VIE 구조 사용 여부 확인 (중국 기업)",
                "본국 규제 변화가 사업에 미치는 영향 평가"
            ),
            redFlags = listOf(
                "US GAAP 조정 후 순이익 50% 이상 감소",
                "VIE 구조에서 계약적 통제권 약함",
                "본국 정부의 산업 규제 강화",
                "환율 변동으로 실적 왜곡 (hedging 여부 확인)",
                "20-F 감사인이 Big 4가 아님",
                "ADR Level 1 (공시 의무 최소)"
            ),
            timelinessCheck = TimelinessInfo(
                deadline = "회계연도 종료 후 4-6개월",
                lateFilingSignal = "회계 기준 차이 조정이나 감사 지연"
            )
        )
    }
    
    private fun detect8KEventType(content: String): List<String> {
        val events = mutableListOf<String>()
        val lowerContent = content.lowercase()
        
        if (lowerContent.contains("item 1.01")) events.add("중요 계약 체결")
        if (lowerContent.contains("item 1.02")) events.add("계약 해지")
        if (lowerContent.contains("item 2.01")) events.add("인수합병 완료")
        if (lowerContent.contains("item 2.02")) events.add("실적 발표")
        if (lowerContent.contains("item 2.03")) events.add("차입금 발생")
        if (lowerContent.contains("item 2.04")) events.add("채무불이행 트리거")
        if (lowerContent.contains("item 4.01")) events.add("감사인 교체")
        if (lowerContent.contains("item 4.02")) events.add("재무제표 신뢰성 문제")
        if (lowerContent.contains("item 5.02")) events.add("임원 교체")
        if (lowerContent.contains("item 7.01")) events.add("공정공시")
        if (lowerContent.contains("item 8.01")) events.add("기타 중요 사건")
        
        return events.ifEmpty { listOf("미분류 사건") }
    }
}

@kotlinx.serialization.Serializable
data class ReportSpecificInsights(
    val reportType: String,
    val keyFocus: List<String>,
    val criticalSections: Map<String, String>,
    val recommendedActions: List<String>,
    val redFlags: List<String>,
    val timelinessCheck: TimelinessInfo
)

@kotlinx.serialization.Serializable
data class TimelinessInfo(
    val deadline: String,
    val lateFilingSignal: String
)
