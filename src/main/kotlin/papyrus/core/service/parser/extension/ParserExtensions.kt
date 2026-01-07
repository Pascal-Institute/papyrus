package papyrus.core.service.parser

import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.FinancialMetric

/**
 * Common extension functions for document parsers.
 *
 * Provides shared utilities to reduce code duplication across HtmlParser, PdfParser, and TxtParser.
 */

/**
 * Converts an ExtendedFinancialMetric to a simpler FinancialMetric.
 *
 * This is a common operation across all document parsers to map the extended internal
 * representation to the public API model.
 */
fun ExtendedFinancialMetric.toFinancialMetric(): FinancialMetric {
    return FinancialMetric(
            name = this.name,
            value = this.value,
            rawValue = this.rawValue,
            context = this.context
    )
}
