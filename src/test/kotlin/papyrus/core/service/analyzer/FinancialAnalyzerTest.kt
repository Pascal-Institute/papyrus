package papyrus.core.service.analyzer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import papyrus.core.model.ExtendedFinancialMetric
import papyrus.core.model.MetricCategory
import papyrus.util.finance.RatioCalculator
import java.math.BigDecimal

class FinancialAnalyzerTest {

    @Test
    fun `calculateRatios computes Net Margin correctly`() {
        // Given
        val metrics =
                mapOf(
                        MetricCategory.REVENUE to createMetric("1000", MetricCategory.REVENUE),
                        MetricCategory.NET_INCOME to createMetric("200", MetricCategory.NET_INCOME)
                )

        // When
        val ratios = RatioCalculator.calculateRatios(metrics)

        // Then
        // Net Margin = (200 / 1000) * 100 = 20.0
        val netMargin = ratios.find { it.name == "Net Margin" }
        assertNotNull(netMargin, "Net Margin should be calculated")
        val actualValue = BigDecimal(netMargin!!.value).toDouble()
        assertEquals(20.0, actualValue, 0.01)
        assertEquals("20.00%", netMargin.formattedValue)
    }

    @Test
    fun `calculateRatios computes Current Ratio correctly`() {
        // Given
        val metrics =
                mapOf(
                        MetricCategory.CURRENT_ASSETS to
                                createMetric("500", MetricCategory.CURRENT_ASSETS),
                        MetricCategory.CURRENT_LIABILITIES to
                                createMetric("250", MetricCategory.CURRENT_LIABILITIES)
                )

        // When
        val ratios = RatioCalculator.calculateRatios(metrics)

        // Then
        // Current Ratio = 500 / 250 = 2.0
        val currentRatio = ratios.find { it.name == "Current Ratio" }
        assertNotNull(currentRatio)
        val actualValue = BigDecimal(currentRatio!!.value).toDouble()
        assertEquals(2.0, actualValue, 0.01)
        assertEquals("2.00x", currentRatio.formattedValue)
    }

    @Test
    fun `calculateRatios computes Debt-to-Equity correctly`() {
        // Given
        val metrics =
                mapOf(
                        MetricCategory.TOTAL_LIABILITIES to
                                createMetric("2000", MetricCategory.TOTAL_LIABILITIES),
                        MetricCategory.TOTAL_EQUITY to
                                createMetric("1000", MetricCategory.TOTAL_EQUITY)
                )

        // When
        val ratios = RatioCalculator.calculateRatios(metrics)

        // Then
        // D/E = 2000 / 1000 = 2.0
        val deRatio = ratios.find { it.name == "Debt-to-Equity" }
        assertNotNull(deRatio)
        val actualValue = BigDecimal(deRatio!!.value).toDouble()
        assertEquals(2.0, actualValue, 0.01)
    }

    private fun createMetric(value: String, category: MetricCategory): ExtendedFinancialMetric {
        return ExtendedFinancialMetric(
                name = "Test Metric",
                value = value,
                rawValue = value,
                category = category,
                source = "Test"
        )
    }
}
