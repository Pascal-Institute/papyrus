package papyrus.core.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Typed mapping for SEC companyfacts endpoint to reduce manual JSON traversal.
 */
data class CompanyFacts(
        @JsonProperty("cik") val cik: String? = null,
        @JsonProperty("entityName") val entityName: String? = null,
        @JsonProperty("facts") val facts: Map<String, Map<String, CompanyConcept>> = emptyMap(),
)

data class CompanyConcept(
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("description") val description: String? = null,
        @JsonProperty("units") val units: Map<String, List<CompanyFactValue>> = emptyMap(),
)

data class CompanyFactValue(
        @JsonProperty("start") val start: String? = null,
        @JsonProperty("end") val end: String? = null,
        @JsonProperty("val") val amount: Double? = null,
        @JsonProperty("fy") val fiscalYear: String? = null,
        @JsonProperty("fp") val fiscalPeriod: String? = null,
        @JsonProperty("form") val form: String? = null,
)
