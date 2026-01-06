package papyrus.core.service.parser

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import papyrus.core.model.XbrlCompanyFact

/**
 * Extract a small, high-value subset of XBRL company facts (SEC companyfacts JSON).
 *
 * We keep this conservative and UI-friendly: latest value for a handful of core concepts.
 */
object XbrlCompanyFactsExtractor {

    fun extractKeyFacts(companyFactsJson: JsonObject): List<XbrlCompanyFact> {
        val concepts = listOf(
            ConceptSpec("Assets", "Total Assets"),
            ConceptSpec("Liabilities", "Total Liabilities"),
            ConceptSpec("StockholdersEquity", "Total Equity"),
            ConceptSpec("Revenues", "Revenue"),
            ConceptSpec("NetIncomeLoss", "Net Income"),
            ConceptSpec("NetCashProvidedByUsedInOperatingActivities", "Operating Cash Flow"),
            ConceptSpec("CashAndCashEquivalentsAtCarryingValue", "Cash and Cash Equivalents"),
        )

        return concepts.mapNotNull { spec -> latestFact(companyFactsJson, taxonomy = "us-gaap", concept = spec.concept, label = spec.label) }
    }

    private data class ConceptSpec(val concept: String, val label: String)

    private fun latestFact(
        root: JsonObject,
        taxonomy: String,
        concept: String,
        label: String,
    ): XbrlCompanyFact? {
        val facts = root["facts"] as? JsonObject ?: return null
        val tax = facts[taxonomy] as? JsonObject ?: return null
        val conceptObj = tax[concept] as? JsonObject ?: return null
        val units = conceptObj["units"] as? JsonObject ?: return null

        // Pick the first unit key (commonly USD). If multiple exist, this is still deterministic.
        val unitKey = units.keys.firstOrNull() ?: return null
        val arr = units[unitKey] as? JsonArray ?: return null

        val latest = arr
            .mapNotNull { it as? JsonObject }
            .maxByOrNull { it.string("end") ?: it.string("fy") ?: "" }
            ?: return null

        val end = latest.string("end")
        val value = latest.double("val")

        return XbrlCompanyFact(
            concept = concept,
            label = label,
            unit = unitKey,
            periodEnd = end,
            value = value,
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.double(key: String): Double? {
        val prim = this[key] as? JsonPrimitive ?: return null
        return prim.doubleOrNull
    }
}
