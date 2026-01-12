package com.pascal.institute.ahmes.model

import kotlinx.serialization.Serializable

/** Result of parsing a Form 4 (Statement of Changes in Beneficial Ownership) */
data class Form4ParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,
        val issuerName: String?,
        val issuerTicker: String?,
        val reportingOwner: ReportingOwner?,
        val nonDerivativeTransactions: List<InsiderTransaction>,
        val derivativeTransactions: List<InsiderTransaction>,
        val signatures: List<String>
) : SecReportParseResult

/** Reporting Owner Information */
@Serializable
data class ReportingOwner(
        val cik: String?,
        val name: String?,
        val address: String?,
        val relationship: OwnerRelationship?
)

/** Relationship of the reporting owner to the issuer */
@Serializable
data class OwnerRelationship(
        val isDirector: Boolean = false,
        val isOfficer: Boolean = false,
        val isTenPercentOwner: Boolean = false,
        val isOther: Boolean = false,
        val officerTitle: String? = null,
        val otherText: String? = null
)

/** Insider Transaction Details (Table I and Table II) */
@Serializable
data class InsiderTransaction(
        val titleOfSecurity: String?,
        val transactionDate: String?,
        val transactionCode: String?,
        val isAcquisition: Boolean?, // A (Acquire) or D (Dispose)
        val sharesError: Boolean = false,

        // Transaction details
        val amount: String?,
        val pricePerShare: String?,

        // Post-transaction
        val sharesOwnedFollowing: String?,
        val ownershipForm: String?, // Direct (D) or Indirect (I)
        val natureOfIndirectOwnership: String?
)

/** Helper enum for transaction codes */
enum class TransactionCode(val code: String, val description: String) {
    P("P", "Open market or private purchase"),
    S("S", "Open market or private sale"),
    A("A", "Grant, award or other acquisition"),
    D("D", "Disposition to the issuer"),
    F("F", "Payment of exercise price or tax liability by delivering or withholding securities"),
    I("I", "Discretionary transaction"),
    M("M", "Exercise or conversion of derivative security"),
    C("C", "Conversion of derivative security"),
    E("E", "Expiration of short derivative position"),
    H("H", "Expiration (or cancellation) of long derivative position with value received"),
    O("O", "Exercise of out-of-the-money derivative security"),
    X("X", "Exercise of in-the-money or at-the-money derivative security"),
    G("G", "Bona fide gift"),
    W("W", "Will or laws of descent or distribution"),
    J("J", "Other")
}

/** Result of parsing a Form 144 (Notice of Proposed Sale of Securities) */
data class Form144ParseResult(
        override val metadata: SecReportMetadata,
        override val rawContent: String,
        override val sections: Map<String, String>,
        val issuerName: String?,
        val issuerTicker: String?,
        val personSelling: SellerInfo?,
        val proposedSaleInfo: ProposedSaleInfo?,
        val remarks: String?
) : SecReportParseResult

/** Information about the person/entity planning to sell securities */
@Serializable
data class SellerInfo(
        val name: String,
        val cik: String? = null,
        val relationship: String // e.g., "Officer", "Director", "10% Owner", "Affiliate"
)

/** Details about the proposed sale */
@Serializable
data class ProposedSaleInfo(
        val securityType: String, // e.g., "Common Stock", "Preferred Stock"
        val numberOfShares: String,
        val aggregateMarketValue: String? = null,
        val proposedSaleDate: String? = null,
        val brokerName: String? = null
)

