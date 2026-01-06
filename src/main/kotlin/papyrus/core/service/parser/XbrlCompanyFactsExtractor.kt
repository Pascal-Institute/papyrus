package papyrus.core.service.parser

import papyrus.core.model.CompanyFacts
import papyrus.core.model.XbrlCompanyFact

/**
 * Extract a small, high-value subset of XBRL company facts (SEC companyfacts JSON).
 *
 * We keep this conservative and UI-friendly: latest value for a handful of core concepts.
 */
object XbrlCompanyFactsExtractor {

    fun extractKeyFacts(companyFacts: CompanyFacts): List<XbrlCompanyFact> {
        val concepts = listOf(
            ConceptSpec("Assets", "Total Assets"),
            ConceptSpec("Liabilities", "Total Liabilities"),
            ConceptSpec("StockholdersEquity", "Total Equity"),
            ConceptSpec("Revenues", "Revenue"),
            ConceptSpec("NetIncomeLoss", "Net Income"),
            ConceptSpec("NetCashProvidedByUsedInOperatingActivities", "Operating Cash Flow"),
            ConceptSpec("CashAndCashEquivalentsAtCarryingValue", "Cash and Cash Equivalents"),
        )

        return concepts.mapNotNull { spec -> latestFact(companyFacts, taxonomy = "us-gaap", concept = spec.concept, label = spec.label) }
    }

    private data class ConceptSpec(val concept: String, val label: String)

    private fun latestFact(
        root: CompanyFacts,
        taxonomy: String,
        concept: String,
        label: String,
    ): XbrlCompanyFact? {
        val conceptByTax = root.facts[taxonomy] ?: return null
        val conceptObj = conceptByTax[concept] ?: return null
        val unitEntry = conceptObj.units.entries.firstOrNull() ?: return null

        val latest = unitEntry.value
            .maxByOrNull { it.end ?: it.fiscalYear ?: "" }
            ?: return null

        return XbrlCompanyFact(
            concept = concept,
            label = label,
            unit = unitEntry.key,
            periodEnd = latest.end,
            value = latest.amount,
        )
    }
}
