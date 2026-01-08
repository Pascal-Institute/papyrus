package io.ahmes.parser

import io.ahmes.model.CompanyFacts
import io.ahmes.model.XbrlCompanyFact

/**
 * XBRL Company Facts Extractor
 *
 * Extracts key facts from SEC companyfacts JSON format.
 * Provides a small, high-value subset of XBRL company facts.
 */
object XbrlCompanyFactsExtractor {

    private data class ConceptSpec(val concept: String, val label: String)

    private val keyConceptSpecs = listOf(
        ConceptSpec("Assets", "Total Assets"),
        ConceptSpec("Liabilities", "Total Liabilities"),
        ConceptSpec("StockholdersEquity", "Total Equity"),
        ConceptSpec("Revenues", "Revenue"),
        ConceptSpec("NetIncomeLoss", "Net Income"),
        ConceptSpec("NetCashProvidedByUsedInOperatingActivities", "Operating Cash Flow"),
        ConceptSpec("CashAndCashEquivalentsAtCarryingValue", "Cash and Cash Equivalents"),
        ConceptSpec("EarningsPerShareBasic", "Basic EPS"),
        ConceptSpec("EarningsPerShareDiluted", "Diluted EPS"),
        ConceptSpec("CommonStockSharesOutstanding", "Shares Outstanding")
    )

    /**
     * Extract key facts from XBRL company facts
     */
    fun extractKeyFacts(companyFacts: CompanyFacts): List<XbrlCompanyFact> {
        return keyConceptSpecs.mapNotNull { spec ->
            latestFact(companyFacts, taxonomy = "us-gaap", concept = spec.concept, label = spec.label)
        }
    }

    /**
     * Extract all available facts from company facts
     */
    fun extractAllFacts(companyFacts: CompanyFacts): List<XbrlCompanyFact> {
        val facts = mutableListOf<XbrlCompanyFact>()

        companyFacts.facts.forEach { (taxonomy, concepts) ->
            concepts.forEach { (conceptName, conceptData) ->
                conceptData.units.forEach { (unitKey, values) ->
                    val latest = values.maxByOrNull { it.end ?: it.fiscalYear ?: "" }
                    if (latest != null) {
                        facts.add(
                            XbrlCompanyFact(
                                concept = conceptName,
                                label = formatLabel(conceptName),
                                unit = unitKey,
                                periodEnd = latest.end,
                                value = latest.amount.toPlainString()
                            )
                        )
                    }
                }
            }
        }

        return facts
    }

    /**
     * Extract facts for specific period
     */
    fun extractFactsForPeriod(companyFacts: CompanyFacts, periodEnd: String): List<XbrlCompanyFact> {
        val facts = mutableListOf<XbrlCompanyFact>()

        companyFacts.facts.forEach { (taxonomy, concepts) ->
            concepts.forEach { (conceptName, conceptData) ->
                conceptData.units.forEach { (unitKey, values) ->
                    val periodValue = values.find { it.end == periodEnd }
                    if (periodValue != null) {
                        facts.add(
                            XbrlCompanyFact(
                                concept = conceptName,
                                label = formatLabel(conceptName),
                                unit = unitKey,
                                periodEnd = periodValue.end,
                                value = periodValue.amount.toPlainString()
                            )
                        )
                    }
                }
            }
        }

        return facts
    }

    private fun latestFact(
        root: CompanyFacts,
        taxonomy: String,
        concept: String,
        label: String
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
            value = latest.amount.toPlainString()
        )
    }

    private fun formatLabel(conceptName: String): String {
        return conceptName
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
    }
}
