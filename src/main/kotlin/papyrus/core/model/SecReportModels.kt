package papyrus.core.model

import kotlinx.serialization.Serializable

/** SEC 보고서 타입 */
enum class SecReportType(val displayName: String, val importance: Int) {
    // 핵심 재무 보고서 (Financial Reports)
    FORM_10K("10-K Annual Report", 10),
    FORM_10Q("10-Q Quarterly Report", 9),
    FORM_8K("8-K Current Report", 8),

    // IPO & 등록
    FORM_S1("S-1 IPO Registration", 7),
    FORM_S4("S-4 M&A Registration", 6),

    // 주주 및 지배구조
    FORM_DEF14A("DEF 14A Proxy Statement", 5),

    // 외국 기업
    FORM_20F("20-F Foreign Annual Report", 8),
    FORM_6K("6-K Foreign Current Report", 6),

    // 특수 목적
    FORM_11K("11-K Employee Stock Plans", 3),
    FORM_NT_10K("NT 10-K Late Filing Notice", 2),
    FORM_NT_10Q("NT 10-Q Late Filing Notice", 2),

    // 기타
    FORM_4("Form 4 Insider Trading", 4),
    FORM_3("Form 3 Initial Ownership", 3),
    FORM_SC_13G("SC 13G Ownership Report", 4),
    UNKNOWN("Unknown Form", 0);

    companion object {
        fun fromFormType(formType: String): SecReportType {
            return when (formType.uppercase().replace("-", "").replace(" ", "")) {
                "10K" -> FORM_10K
                "10Q" -> FORM_10Q
                "8K" -> FORM_8K
                "S1" -> FORM_S1
                "S4" -> FORM_S4
                "DEF14A" -> FORM_DEF14A
                "20F" -> FORM_20F
                "6K" -> FORM_6K
                "11K" -> FORM_11K
                "NT10K" -> FORM_NT_10K
                "NT10Q" -> FORM_NT_10Q
                "4" -> FORM_4
                "3" -> FORM_3
                "SC13G" -> FORM_SC_13G
                else -> UNKNOWN
            }
        }
    }
}

// ========================================
// 공통 SEC 보고서 파싱 결과
// ========================================

/** 모든 SEC 보고서 파싱 결과의 기본 인터페이스 */
interface SecReportParseResult {
    val metadata: SecReportMetadata
    val rawContent: String
    val sections: Map<String, String>
}

// ========================================
// 10-K 보고서 모델
// ========================================

/** 10-K 연간 보고서 파싱 결과 */
@Serializable
data class Form10KParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,

        // 10-K 특화 섹션
        val businessDescription: String? = null, // Item 1: Business
        val riskFactors: List<RiskFactor> = emptyList(), // Item 1A: Risk Factors
        val properties: String? = null, // Item 2: Properties
        val legalProceedings: String? = null, // Item 3: Legal Proceedings
        val mdAndA: ManagementDiscussion? = null, // Item 7: MD&A
        val financialStatements: StructuredFinancialData? = null, // Item 8: Financial Statements
        val controlsAndProcedures: String? = null, // Item 9A: Controls and Procedures
        val executiveCompensation: String? = null, // Item 11: Executive Compensation
        val directorInfo: String? = null, // Item 10: Directors & Officers
        val exhibits: List<String> = emptyList() // Item 15: Exhibits
) : SecReportParseResult

/** 10-K 보고서 Item (섹션) 정의 */
enum class Form10KItem(val itemNumber: String, val title: String) {
    ITEM_1("1", "Business"),
    ITEM_1A("1A", "Risk Factors"),
    ITEM_1B("1B", "Unresolved Staff Comments"),
    ITEM_2("2", "Properties"),
    ITEM_3("3", "Legal Proceedings"),
    ITEM_4("4", "Mine Safety Disclosures"),
    ITEM_5("5", "Market for Registrant's Common Equity"),
    ITEM_6("6", "Selected Financial Data"),
    ITEM_7("7", "Management's Discussion and Analysis"),
    ITEM_7A("7A", "Quantitative and Qualitative Disclosures About Market Risk"),
    ITEM_8("8", "Financial Statements and Supplementary Data"),
    ITEM_9("9", "Changes in and Disagreements with Accountants"),
    ITEM_9A("9A", "Controls and Procedures"),
    ITEM_9B("9B", "Other Information"),
    ITEM_10("10", "Directors, Executive Officers and Corporate Governance"),
    ITEM_11("11", "Executive Compensation"),
    ITEM_12("12", "Security Ownership of Certain Beneficial Owners and Management"),
    ITEM_13("13", "Certain Relationships and Related Transactions"),
    ITEM_14("14", "Principal Accounting Fees and Services"),
    ITEM_15("15", "Exhibits, Financial Statement Schedules")
}

// ========================================
// 10-Q 보고서 모델
// ========================================

/** 10-Q 분기 보고서 파싱 결과 */
@Serializable
data class Form10QParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,

        // 10-Q 특화 섹션
        val financialStatements: StructuredFinancialData? = null, // Part I, Item 1
        val mdAndA: ManagementDiscussion? = null, // Part I, Item 2
        val marketRiskDisclosures: String? = null, // Part I, Item 3
        val controlsAndProcedures: String? = null, // Part I, Item 4
        val legalProceedings: String? = null, // Part II, Item 1
        val riskFactors: List<RiskFactor> = emptyList(), // Part II, Item 1A
        val exhibits: List<String> = emptyList(), // Part II, Item 6

        // 분기 정보
        val quarter: String? = null, // Q1, Q2, Q3
        val fiscalYear: String? = null
) : SecReportParseResult

/** 10-Q 보고서 Item (섹션) 정의 */
enum class Form10QItem(val part: String, val itemNumber: String, val title: String) {
    PART1_ITEM1("Part I", "1", "Financial Statements"),
    PART1_ITEM2("Part I", "2", "Management's Discussion and Analysis"),
    PART1_ITEM3("Part I", "3", "Quantitative and Qualitative Disclosures About Market Risk"),
    PART1_ITEM4("Part I", "4", "Controls and Procedures"),
    PART2_ITEM1("Part II", "1", "Legal Proceedings"),
    PART2_ITEM1A("Part II", "1A", "Risk Factors"),
    PART2_ITEM2("Part II", "2", "Unregistered Sales of Equity Securities"),
    PART2_ITEM3("Part II", "3", "Defaults Upon Senior Securities"),
    PART2_ITEM4("Part II", "4", "Mine Safety Disclosures"),
    PART2_ITEM5("Part II", "5", "Other Information"),
    PART2_ITEM6("Part II", "6", "Exhibits")
}

// ========================================
// 8-K 보고서 모델
// ========================================

/** 8-K 현재 보고서 파싱 결과 */
@Serializable
data class Form8KParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,

        // 8-K 특화 정보
        val eventDate: String? = null,
        val eventItems: List<String> = emptyList(), // 예: "Item 2.02", "Item 5.02"
        val eventDescriptions: Map<String, String> = emptyMap(),

        // 주요 이벤트 카테고리별
        val financialResults: String? = null, // Item 2.02
        val acquisitions: String? = null, // Item 2.01
        val dispositions: String? = null, // Item 2.01
        val executiveChanges: String? = null, // Item 5.02
        val bankruptcy: String? = null, // Item 1.03
        val exhibits: List<String> = emptyList()
) : SecReportParseResult

/** 8-K 보고서 Item (이벤트) 정의 */
enum class Form8KItem(val itemNumber: String, val title: String, val category: String) {
    ITEM_1_01("1.01", "Entry into Material Agreement", "Corporate"),
    ITEM_1_02("1.02", "Termination of Material Agreement", "Corporate"),
    ITEM_1_03("1.03", "Bankruptcy or Receivership", "Corporate"),
    ITEM_1_04("1.04", "Mine Safety", "Corporate"),
    ITEM_2_01("2.01", "Completion of Acquisition or Disposition", "Financial"),
    ITEM_2_02("2.02", "Results of Operations and Financial Condition", "Financial"),
    ITEM_2_03("2.03", "Creation of Direct Financial Obligation", "Financial"),
    ITEM_2_04("2.04", "Triggering Events That Accelerate Obligations", "Financial"),
    ITEM_2_05("2.05", "Costs Associated with Exit or Disposal Activities", "Financial"),
    ITEM_2_06("2.06", "Material Impairments", "Financial"),
    ITEM_3_01("3.01", "Notice of Delisting or Failure to Satisfy Listing Rule", "Securities"),
    ITEM_3_02("3.02", "Unregistered Sales of Equity Securities", "Securities"),
    ITEM_3_03("3.03", "Material Modification to Rights of Security Holders", "Securities"),
    ITEM_4_01("4.01", "Changes in Registrant's Certifying Accountant", "Governance"),
    ITEM_4_02("4.02", "Non-Reliance on Previously Issued Financial Statements", "Governance"),
    ITEM_5_01("5.01", "Changes in Control of Registrant", "Governance"),
    ITEM_5_02("5.02", "Departure/Election of Directors or Officers", "Governance"),
    ITEM_5_03("5.03", "Amendments to Articles of Incorporation or Bylaws", "Governance"),
    ITEM_5_04("5.04", "Temporary Suspension of Trading", "Governance"),
    ITEM_5_05("5.05", "Amendments to Registrant's Code of Ethics", "Governance"),
    ITEM_5_06("5.06", "Change in Shell Company Status", "Governance"),
    ITEM_5_07("5.07", "Submission of Matters to Vote of Security Holders", "Governance"),
    ITEM_5_08("5.08", "Shareholder Director Nominations", "Governance"),
    ITEM_6_01("6.01", "ABS Informational and Computational Material", "Asset-Backed Securities"),
    ITEM_6_02("6.02", "Change of Servicer or Trustee", "Asset-Backed Securities"),
    ITEM_6_03("6.03", "Change in Credit Enhancement", "Asset-Backed Securities"),
    ITEM_7_01("7.01", "Regulation FD Disclosure", "Other"),
    ITEM_8_01("8.01", "Other Events", "Other"),
    ITEM_9_01("9.01", "Financial Statements and Exhibits", "Other")
}

// ========================================
// S-1 보고서 모델
// ========================================

/** S-1 IPO 등록서 파싱 결과 */
@Serializable
data class FormS1ParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,

        // S-1 특화 섹션
        val prospectus: String? = null,
        val businessDescription: String? = null,
        val riskFactors: List<RiskFactor> = emptyList(),
        val useOfProceeds: String? = null,
        val dilution: String? = null,
        val financialStatements: StructuredFinancialData? = null,
        val mdAndA: ManagementDiscussion? = null,
        val underwriting: String? = null,
        val offeringPrice: String? = null,
        val sharesOffered: String? = null
) : SecReportParseResult

// ========================================
// DEF 14A 보고서 모델
// ========================================

/** DEF 14A Proxy Statement 파싱 결과 */
@Serializable
data class FormDEF14AParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,

        // DEF 14A 특화 섹션
        val meetingDate: String? = null,
        val votingMatters: List<String> = emptyList(),
        val executiveCompensation: String? = null,
        val compensationTables: String? = null,
        val directorInfo: String? = null,
        val corporateGovernance: String? = null,
        val auditInfo: String? = null
) : SecReportParseResult

// ========================================
// 20-F 보고서 모델
// ========================================

/** 20-F 외국 기업 연간 보고서 파싱 결과 */
@Serializable
data class Form20FParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,

        // 20-F 특화 섹션 (10-K와 유사하지만 외국 기업용)
        val businessDescription: String? = null,
        val riskFactors: List<RiskFactor> = emptyList(),
        val financialStatements: StructuredFinancialData? = null,
        val mdAndA: ManagementDiscussion? = null,
        val corporateGovernance: String? = null,

        // 외국 기업 특화 정보
        val countryOfIncorporation: String? = null,
        val accountingStandard: String? = null // US GAAP, IFRS, etc.
) : SecReportParseResult

// ========================================
// 기타 보고서 모델
// ========================================

/** 범용 SEC 보고서 파싱 결과 (알 수 없는 형식) */
@Serializable
data class GenericSecReportParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>
) : SecReportParseResult
