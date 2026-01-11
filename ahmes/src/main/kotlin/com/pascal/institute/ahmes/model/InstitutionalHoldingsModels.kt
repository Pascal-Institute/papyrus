package com.pascal.institute.ahmes.model

import kotlinx.serialization.Serializable

/** Result of parsing a Form 13F (Institutional Holdings Report) */
data class Form13FParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,
        val reportInfo: Form13FReportInfo?,
        val holdings: List<HoldingEntry>,
        val summary: HoldingsSummary?
) : SecReportParseResult

/** Information about the 13F Report and Manager */
@Serializable
data class Form13FReportInfo(
        val periodOfReport: String?,
        val filingManagerName: String?,
        val filingManagerAddress: String?,
        val signatureDate: String?,
        val reportType: String? // e.g., "13F HOLDINGS REPORT"
)

/** Individual Holding Entry (Information Table) */
@Serializable
data class HoldingEntry(
        val nameOfIssuer: String,
        val titleOfClass: String?,
        val cusip: String?,
        val value: Long?, // In thousands usually
        val sharesOrPrincipalAmount: Long?,
        val sharesOrPrincipalType: String?, // "SH" or "PRN"
        val putCall: String? = null,
        val investmentDiscretion: String?,
        val otherManager: String? = null,
        val votingAuthority: VotingAuthority?
)

/** Voting Authority Breakdown */
@Serializable
data class VotingAuthority(val sole: Long = 0, val shared: Long = 0, val none: Long = 0)

/** Summary of Holdings */
@Serializable
data class HoldingsSummary(
        val totalHoldingsCount: Int,
        val totalValue: Long, // Sum of all values
        val topHoldings: List<HoldingEntry> // Top 5/10 by value
)
