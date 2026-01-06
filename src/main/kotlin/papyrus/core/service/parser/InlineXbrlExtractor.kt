package papyrus.core.service.parser

import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.XPathExecutable
import net.sf.saxon.s9api.Axis
import net.sf.saxon.s9api.XdmNode
import net.sf.saxon.s9api.XdmValue
import org.jsoup.helper.W3CDom
import org.jsoup.nodes.Document
import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.MetricCategory
import papyrus.core.model.MetricUnit
import java.math.BigDecimal
import java.math.RoundingMode
import javax.xml.transform.dom.DOMSource

/**
 * Extract inline XBRL (iXBRL) facts from SEC HTML filings.
 *
 * Goal: turn structured facts (contextRef/unitRef/scale/decimals) into high-confidence metrics.
 * Keep it conservative: only map a small set of widely-used GAAP concepts.
 */
object InlineXbrlExtractor {

    private val processor = Processor(false)
    private val xpath = processor.newXPathCompiler()
    private val contextExpr = xpath.compile("//*[local-name()='context']")
    private val factExpr =
        xpath.compile("//*[local-name()='nonFraction' or local-name()='nonNumeric' or @contextRef or @contextref or @unitRef or @unitref]")

    private fun XPathExecutable.eval(root: XdmNode): XdmValue {
        val selector = this.load()
        selector.contextItem = root
        return selector.evaluate()
    }

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
        val w3c = W3CDom().fromJsoup(doc)
        val root = processor.newDocumentBuilder().build(DOMSource(w3c))

        val contexts = mergeContexts(
            primary = parseContexts(root),
            fallback = parseContextsFallback(doc)
        )
        val facts = extractFacts(root).ifEmpty { extractFactsFallback(doc) }

        return facts.mapNotNull { fact ->
            val mapping = mapConceptToMetric(fact.concept) ?: return@mapNotNull null
            val (name, category) = mapping

            val unit = inferUnit(fact)
            val period = fact.contextRef?.let { contexts[normalizeId(it)] }?.let { ctx ->
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

    private fun parseContexts(root: XdmNode): Map<String, XbrlContext> {
        val contexts = mutableMapOf<String, XbrlContext>()

        for (ctx in contextExpr.eval(root).nodes()) {
            val id = ctx.attrAny("id") ?: continue
            val key = normalizeId(id)
            val instant = stringChild(ctx, "instant")
            val startDate = stringChild(ctx, "startDate")
            val endDate = stringChild(ctx, "endDate")

            contexts[key] = XbrlContext(id = id, instant = instant, startDate = startDate, endDate = endDate)
        }

        return contexts
    }

    private fun parseContextsFallback(doc: Document): Map<String, XbrlContext> {
        val contexts = mutableMapOf<String, XbrlContext>()

        doc.select("context, xbrli\\:context").forEach { ctxEl ->
            val id = ctxEl.attr("id").ifBlank { return@forEach }
            val key = normalizeId(id)
            val instant = ctxEl.selectFirst("instant, xbrli\\:instant")?.text()?.trim()?.ifBlank { null }
            val startDate = ctxEl.selectFirst("startDate, xbrli\\:startDate")?.text()?.trim()?.ifBlank { null }
            val endDate = ctxEl.selectFirst("endDate, xbrli\\:endDate")?.text()?.trim()?.ifBlank { null }

            contexts[key] = XbrlContext(id = id, instant = instant, startDate = startDate, endDate = endDate)
        }

        return contexts
    }

    private fun extractFacts(root: XdmNode): List<XbrlFact> {
        val facts = mutableListOf<XbrlFact>()

        for (node in factExpr.eval(root).nodes()) {
            val (concept, rawText) = extractConceptAndText(node) ?: continue
            val contextRef = node.attrAny("contextRef", "contextref")
            val unitRef = node.attrAny("unitRef", "unitref")
            val decimals = node.attrAny("decimals")
            val scale = node.attrAny("scale")?.toIntOrNull()
            val sign = node.attrAny("sign")

            val parsed = parseNumericFact(rawText, scale = scale, sign = sign) ?: continue

            facts.add(
                XbrlFact(
                    concept = concept,
                    value = parsed,
                    unitRef = unitRef,
                    contextRef = contextRef,
                    decimals = decimals,
                    scale = scale,
                    source = describeSource(node, concept, contextRef, unitRef),
                )
            )
        }

        return facts
    }

    private fun extractFactsFallback(doc: Document): List<XbrlFact> {
        val facts = mutableListOf<XbrlFact>()

        val candidates = doc.select("*[contextref], *[contextRef], *[unitref], *[unitRef], ix\\:nonFraction, ix\\:nonNumeric")

        for (el in candidates) {
            val (concept, rawText) = extractConceptAndTextFallback(el) ?: continue
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
                    source = describeSourceFallback(el),
                )
            )
        }

        return facts
    }

    private fun extractConceptAndText(node: XdmNode): Pair<String, String>? {
        val concept =
            node.attrAny("name")?.takeIf { it.isNotBlank() }
                ?: node.nodeName?.localName?.takeIf { !it.isNullOrBlank() }
                ?: return null

        val text = node.stringValue.trim()
        if (text.isBlank()) return null

        // Filter out obvious non-metrics (very long text blocks)
        if (text.length > 200) return null

        return concept to text
    }

    private fun extractConceptAndTextFallback(el: org.jsoup.nodes.Element): Pair<String, String>? {
        val concept =
            el.attrAnyCase("name")?.takeIf { it.isNotBlank() }
                ?: el.tagName().takeIf { it.isNotBlank() }
                ?: return null
        val text = el.text().trim()
        if (text.isBlank()) return null

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

    private fun describeSource(node: XdmNode, concept: String, contextRef: String?, unitRef: String?): String {
        return buildString {
            append("iXBRL:")
            append(concept)
            if (!contextRef.isNullOrBlank()) append(" contextRef=$contextRef")
            if (!unitRef.isNullOrBlank()) append(" unitRef=$unitRef")
        }
    }

    private fun describeSourceFallback(el: org.jsoup.nodes.Element): String {
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

    private fun org.jsoup.nodes.Element.attrAnyCase(name: String): String? {
        val direct = attr(name)
        if (direct.isNotBlank()) return direct
        val lower = attr(name.lowercase())
        if (lower.isNotBlank()) return lower

        val found = attributes().asList().firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
        return found?.takeIf { it.isNotBlank() }
    }

    private fun XdmNode.attrAny(vararg names: String): String? {
        val attrs = this.axisIterator(Axis.ATTRIBUTE)
        while (attrs.hasNext()) {
            val attr = attrs.next() as XdmNode
            val local = attr.nodeName?.localName ?: attr.nodeName?.toString()
            if (local != null && names.any { it.equals(local, ignoreCase = true) }) {
                val value = attr.stringValue.trim()
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }

    private fun stringChild(node: XdmNode, localName: String): String? {
        return xpath
            .evaluate("./*[local-name()='$localName']/text()", node)
            .firstText()
    }

    private fun XdmValue.firstText(): String? {
        val first = this.asIterable().firstOrNull() ?: return null
        val text = first.stringValue.trim()
        return text.ifBlank { null }
    }

    private fun XdmValue.nodes(): Sequence<XdmNode> = this.asIterable().asSequence().mapNotNull { it as? XdmNode }

    private fun normalizeId(id: String): String = id.trim().lowercase()

    private fun mergeContexts(
            primary: Map<String, XbrlContext>,
            fallback: Map<String, XbrlContext>
    ): Map<String, XbrlContext> {
        if (primary.isEmpty()) return fallback
        if (fallback.isEmpty()) return primary

        val merged = mutableMapOf<String, XbrlContext>()
        (primary.keys + fallback.keys).forEach { key ->
            val p = primary[key]
            val f = fallback[key]
            merged[key] = when {
                p == null -> f!!
                p.instant != null || p.startDate != null || p.endDate != null -> p
                else -> f ?: p
            }
        }
        return merged
    }

    private fun BigDecimal.toDoubleOrNullSafe(): Double? {
        return try {
            toDouble()
        } catch (_: Exception) {
            null
        }
    }
}
