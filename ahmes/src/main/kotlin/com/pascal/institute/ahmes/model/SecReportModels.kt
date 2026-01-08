package com.pascal.institute.ahmes.model

import kotlinx.serialization.Serializable

/** SEC report type */
@Serializable
enum class SecReportType(val displayName: String, val importance: Int) {
    // Core financial reports
    FORM_10K("10-K Annual Report", 10),
    FORM_10Q("10-Q Quarterly Report", 9),
    FORM_8K("8-K Current Report", 8),

    // IPO & Registration
    FORM_S1("S-1 IPO Registration", 7),
    FORM_S4("S-4 M&A Registration", 6),

    // Shareholder and Governance
    FORM_DEF14A("DEF 14A Proxy Statement", 5),

    // Foreign Companies
    FORM_20F("20-F Foreign Annual Report", 8),
    FORM_6K("6-K Foreign Current Report", 6),

    // Special Purpose
    FORM_11K("11-K Employee Stock Plans", 3),
    FORM_NT_10K("NT 10-K Late Filing Notice", 2),
    FORM_NT_10Q("NT 10-Q Late Filing Notice", 2),

    // Others
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
// Common SEC Report Parse Results
// ========================================

/** Base interface for all SEC report parse results */
interface SecReportParseResult {
    val metadata: SecReportMetadata
    val rawContent: String
    val sections: Map<String, String>
}

// ========================================
// 10-K Report Model
// ========================================

/** 10-K annual report parse result */
@Serializable
data class Form10KParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>,

    // 10-K specialized sections
    val businessDescription: String? = null,
    val riskFactors: List<RiskFactor> = emptyList(),
    val properties: String? = null,
    val legalProceedings: String? = null,
    val mdAndA: ManagementDiscussion? = null,
    val financialStatements: StructuredFinancialData? = null,
    val controlsAndProcedures: String? = null,
    val executiveCompensation: String? = null,
    val directorInfo: String? = null,
    val exhibits: List<String> = emptyList()
) : SecReportParseResult

/** 10-K report Item (section) definition */
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
// 10-Q Report Model
// ========================================

/** 10-Q quarterly report parse result */
@Serializable
data class Form10QParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>,

    // 10-Q specialized sections
    val financialStatements: StructuredFinancialData? = null,
    val mdAndA: ManagementDiscussion? = null,
    val marketRiskDisclosures: String? = null,
    val controlsAndProcedures: String? = null,
    val legalProceedings: String? = null,
    val riskFactors: List<RiskFactor> = emptyList(),
    val exhibits: List<String> = emptyList(),

    // Quarterly information
    val quarter: String? = null,
    val fiscalYear: String? = null
) : SecReportParseResult

/** 10-Q report Item (section) definition */
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
// 8-K Report Model
// ========================================

/** 8-K current report parse result */
@Serializable
data class Form8KParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>,

    // 8-K specialized information
    val eventDate: String? = null,
    val eventItems: List<String> = emptyList(),
    val eventDescriptions: Map<String, String> = emptyMap(),

    // Major events by category
    val financialResults: String? = null,
    val acquisitions: String? = null,
    val dispositions: String? = null,
    val executiveChanges: String? = null,
    val bankruptcy: String? = null,
    val exhibits: List<String> = emptyList()
) : SecReportParseResult

/** 8-K report Item (event) definition */
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
// S-1 Report Model
// ========================================

/** S-1 IPO registration statement parse result */
@Serializable
data class FormS1ParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>,

    // S-1 specialized sections
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
// DEF 14A Report Model
// ========================================

/** DEF 14A Proxy Statement parse result */
@Serializable
data class FormDEF14AParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>,

    // DEF 14A specialized sections
    val meetingDate: String? = null,
    val votingMatters: List<String> = emptyList(),
    val executiveCompensation: String? = null,
    val compensationTables: String? = null,
    val directorInfo: String? = null,
    val corporateGovernance: String? = null,
    val auditInfo: String? = null
) : SecReportParseResult

// ========================================
// 20-F Report Model
// ========================================

/** 20-F foreign company annual report parse result */
@Serializable
data class Form20FParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>,

    // 20-F specialized sections
    val businessDescription: String? = null,
    val riskFactors: List<RiskFactor> = emptyList(),
    val financialStatements: StructuredFinancialData? = null,
    val mdAndA: ManagementDiscussion? = null,
    val corporateGovernance: String? = null,

    // Foreign company specialized information
    val countryOfIncorporation: String? = null,
    val accountingStandard: String? = null
) : SecReportParseResult

// ========================================
// Other Report Models
// ========================================

/** Generic SEC report parse result (unknown format) */
@Serializable
data class GenericSecReportParseResult(
    override val metadata: SecReportMetadata,
    override val rawContent: String,
    override val sections: Map<String, String>
) : SecReportParseResult

// ========================================
// XBRL Company Facts Models
// ========================================

/**
 * Root model for SEC company facts JSON format.
 * This is the format returned by SEC's companyfacts API.
 */
@Serializable
data class CompanyFacts(
    val cik: Int = 0,
    val entityName: String = "",
    val facts: Map<String, Map<String, ConceptData>> = emptyMap()
)

/**
 * Data for a single XBRL concept (e.g., "Assets", "Revenues")
 */
@Serializable
data class ConceptData(
    val label: String = "",
    val description: String = "",
    val units: Map<String, List<FactValue>> = emptyMap()
)

/**
 * Individual fact value with period and filing information
 */
@Serializable
data class FactValue(
    val start: String? = null,
    val end: String? = null,
    val `val`: Double = 0.0,
    val accn: String? = null,
    val fy: Int? = null,
    val fp: String? = null,
    val form: String? = null,
    val filed: String? = null,
    val frame: String? = null
) {
    /** Get amount as BigDecimal for precision */
    val amount: java.math.BigDecimal get() = java.math.BigDecimal.valueOf(`val`)
    /** Fiscal year as string */
    val fiscalYear: String? get() = fy?.toString()
}

/**
 * Extracted XBRL company fact (simplified output format)
 */
@Serializable
data class XbrlCompanyFact(
    val concept: String,
    val label: String,
    val unit: String,
    val periodEnd: String?,
    val value: String // BigDecimal stored as String for serialization
) {
    /** Get value as BigDecimal for calculations */
    fun getValueBigDecimal(): java.math.BigDecimal = java.math.BigDecimal(value)
}
