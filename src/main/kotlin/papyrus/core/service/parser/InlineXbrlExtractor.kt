package papyrus.core.service.parser

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.MetricCategory
import papyrus.core.model.MetricUnit
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Extract inline XBRL (iXBRL) facts from SEC HTML filings.
 *
 * Goal: turn structured facts (contextRef/unitRef/scale/decimals) into high-confidence metrics.
 * Keep it conservative: only map a small set of widely-used GAAP concepts.
 */
object InlineXbrlExtractor {

    data class XbrlContext(
        val id: String,
        val instant: String? = null,
        val startDate: String? = null,
        val endDate: String? = null,
    )

    data class XbrlFact(
        val concept: String,
        val value: BigDecimal,
        val unitRef: String? = null,
        val contextRef: String? = null,
        val decimals: String? = null,
        val scale: Int? = null,
        val source: String,
    )

    fun extractMetrics(doc: Document): List<ExtendedFinancialMetric> {
        val contexts = parseContexts(doc)
        val facts = extractFacts(doc)

        return facts.mapNotNull { fact ->
            val mapping = mapConceptToMetric(fact.concept) ?: return@mapNotNull null
            val (name, category) = mapping

            val unit = inferUnit(fact)
            val period = fact.contextRef?.let { contexts[it] }?.let { ctx ->
                ctx.instant ?: ctx.endDate ?: ctx.startDate
            }

            ExtendedFinancialMetric(
                name = name,
                value = fact.value.stripTrailingZeros().toPlainString(),
                rawValue = fact.value.toDoubleOrNullSafe(),
                unit = unit,
                period = period,
                category = category,
                source = fact.source,
                confidence = 0.97,
                context = buildString {
                    if (!period.isNullOrBlank()) append("period=$period ")
                    if (!fact.unitRef.isNullOrBlank()) append("unit=${fact.unitRef} ")
                    if (!fact.decimals.isNullOrBlank()) append("decimals=${fact.decimals} ")
                    if (fact.scale != null) append("scale=${fact.scale} ")
                }.trim(),
            )
        }
    }

    private fun parseContexts(doc: Document): Map<String, XbrlContext> {
        val contexts = mutableMapOf<String, XbrlContext>()

        // XBRL contexts are typically <xbrli:context id="..."> ... <xbrli:period> ...
        // Jsoup preserves tag names with ':' unless caller rewrites tags.
        doc.select("context, xbrli\\:context").forEach { ctxEl ->
            val id = ctxEl.attr("id").ifBlank { return@forEach }
            val instant = ctxEl.selectFirst("instant, xbrli\\:instant")?.text()?.trim()?.ifBlank { null }
            val startDate = ctxEl.selectFirst("startDate, xbrli\\:startDate")?.text()?.trim()?.ifBlank { null }
            val endDate = ctxEl.selectFirst("endDate, xbrli\\:endDate")?.text()?.trim()?.ifBlank { null }

            contexts[id] = XbrlContext(id = id, instant = instant, startDate = startDate, endDate = endDate)
        }

        return contexts
    }

    private fun extractFacts(doc: Document): List<XbrlFact> {
        val facts = mutableListOf<XbrlFact>()

        // Inline XBRL facts: ix:nonFraction, ix:nonNumeric, etc. Some filings also embed
        // us-gaap:* tags with contextRef/unitRef.
        val candidates = doc.select("*[contextref], *[contextRef], *[unitref], *[unitRef], ix\\:nonFraction, ix\\:nonNumeric")

        for (el in candidates) {
            val (concept, rawText) = extractConceptAndText(el) ?: continue
            val contextRef = el.attrAnyCase("contextRef")
            val unitRef = el.attrAnyCase("unitRef")
            val decimals = el.attrAnyCase("decimals")
            val scale = el.attrAnyCase("scale")?.toIntOrNull()
            val sign = el.attrAnyCase("sign")

            val parsed = parseNumericFact(rawText, scale = scale, sign = sign) ?: continue

            facts.add(
                XbrlFact(
                    concept = concept,
                    value = parsed,
                    unitRef = unitRef,
                    contextRef = contextRef,
                    decimals = decimals,
                    scale = scale,
                    source = describeSource(el),
                )
            )
        }

        return facts
    }

    private fun extractConceptAndText(el: Element): Pair<String, String>? {
        val concept =
            el.attrAnyCase("name")?.takeIf { it.isNotBlank() }
                ?: el.tagName().takeIf { it.isNotBlank() }
                ?: return null
        val text = el.text().trim()
        if (text.isBlank()) return null

        // Filter out obvious non-metrics (very long text blocks)
        if (text.length > 200) return null

        return concept to text
    }

    private fun parseNumericFact(text: String, scale: Int?, sign: String?): BigDecimal? {
        val cleaned = text
            .replace("\u00A0", " ")
            .trim()

        // Skip non-numeric facts
        val hasDigit = cleaned.any { it.isDigit() }
        if (!hasDigit) return null

        val isNegativeByParens = cleaned.startsWith("(") && cleaned.endsWith(")")
        val isNegativeBySign = sign == "-" || sign.equals("minus", ignoreCase = true)

        val numberStr = cleaned
            .replace("$", "")
            .replace(",", "")
            .replace("(", "")
            .replace(")", "")
            .replace(" ", "")

        val base = numberStr.toBigDecimalOrNull() ?: return null

        val scaled = if (scale != null) {
            // iXBRL scale means multiply by 10^scale
            base.multiply(BigDecimal.TEN.pow(scale))
        } else {
            base
        }

        val signed = if (isNegativeByParens || isNegativeBySign) scaled.negate() else scaled

        // Keep reasonable precision (avoid scientific notation surprises in downstream display)
        return signed.setScale(minOf(6, signed.scale().coerceAtLeast(0)), RoundingMode.HALF_UP)
    }

    private fun mapConceptToMetric(conceptRaw: String): Pair<String, MetricCategory>? {
        val concept = conceptRaw.lowercase()

        // Prefer exact-ish GAAP concepts.
        return when {
            concept.contains("us-gaap:revenues") || concept.contains("us-gaap:salesrevenuenet") || concept.endsWith(":revenues") ->
                "Revenue" to MetricCategory.REVENUE

            concept.contains("us-gaap:netincomeloss") || concept.endsWith(":netincomeloss") ->
                "Net Income" to MetricCategory.NET_INCOME

            concept.contains("us-gaap:grossprofit") || concept.endsWith(":grossprofit") ->
                "Gross Profit" to MetricCategory.GROSS_PROFIT

            concept.contains("us-gaap:operatingincomeloss") || concept.endsWith(":operatingincomeloss") ->
                "Operating Income" to MetricCategory.OPERATING_INCOME

            concept.contains("us-gaap:assets") || concept.endsWith(":assets") ->
                "Total Assets" to MetricCategory.TOTAL_ASSETS

            concept.contains("us-gaap:liabilities") || concept.endsWith(":liabilities") ->
                "Total Liabilities" to MetricCategory.TOTAL_LIABILITIES

            concept.contains("us-gaap:stockholdersequity") || concept.contains("us-gaap:stockholdersequityincludingportionattributabletononcontrollinginterest") ->
                "Total Equity" to MetricCategory.TOTAL_EQUITY

            concept.contains("us-gaap:cashandcashequivalentsatcarryingvalue") ->
                "Cash and Cash Equivalents" to MetricCategory.CASH_AND_EQUIVALENTS

            concept.contains("us-gaap:netcashprovidedbyusedinoperatingactivities") ->
                "Operating Cash Flow" to MetricCategory.OPERATING_CASH_FLOW

            concept.contains("us-gaap:earningspersharebasic") ->
                "EPS (Basic)" to MetricCategory.EPS_BASIC

            concept.contains("us-gaap:earningspersharediluted") ->
                "EPS (Diluted)" to MetricCategory.EPS_DILUTED

            else -> null
        }
    }

    private fun inferUnit(fact: XbrlFact): MetricUnit {
        val unit = fact.unitRef?.lowercase()?.trim()
        val concept = fact.concept.lowercase()

        return when {
            concept.contains("earningspershare") -> MetricUnit.PER_SHARE
            unit == null -> MetricUnit.DOLLARS
            unit.contains("usd") || unit.contains("us") || unit.contains("iso4217") -> MetricUnit.DOLLARS
            unit.contains("shares") -> MetricUnit.SHARES
            unit.contains("pure") -> MetricUnit.NONE
            else -> MetricUnit.DOLLARS
        }
    }

    private fun describeSource(el: Element): String {
        val concept = el.attrAnyCase("name") ?: el.tagName()
        val ctx = el.attrAnyCase("contextRef")
        val unit = el.attrAnyCase("unitRef")
        return buildString {
            append("iXBRL:")
            append(concept)
            if (!ctx.isNullOrBlank()) append(" contextRef=$ctx")
            if (!unit.isNullOrBlank()) append(" unitRef=$unit")
        }
    }

    private fun Element.attrAnyCase(name: String): String? {
        // Jsoup stores attribute keys lowercase in most cases; be defensive.
        val direct = attr(name)
        if (direct.isNotBlank()) return direct
        val lower = attr(name.lowercase())
        if (lower.isNotBlank()) return lower

        // Last resort: scan attributes.
        val found = attributes().asList().firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
        return found?.takeIf { it.isNotBlank() }
    }

    private fun BigDecimal.toDoubleOrNullSafe(): Double? {
        return try {
            toDouble()
        } catch (_: Exception) {
            null
        }
    }
}
