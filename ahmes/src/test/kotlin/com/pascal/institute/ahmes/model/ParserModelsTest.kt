package com.pascal.institute.ahmes.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Tests for Parser Models
 */
class ParserModelsTest {

    @Test
    fun `ExtendedFinancialMetric should convert to FinancialMetric`() {
        val extended = ExtendedFinancialMetric(
            name = "Total Revenue",
            value = "$50.5 billion",
            rawValue = "50500000000",
            unit = MetricUnit.BILLIONS,
            period = "FY 2024",
            periodType = PeriodType.ANNUAL,
            category = MetricCategory.REVENUE,
            source = "Income Statement",
            confidence = 0.95,
            yearOverYearChange = "10.5",
            context = "Consolidated revenue"
        )

        val simple = extended.toFinancialMetric()

        assertEquals("Total Revenue", simple.name)
        assertEquals("$50.5 billion", simple.value)
        assertEquals("50500000000", simple.rawValue)
        assertEquals("Consolidated revenue", simple.context)
    }

    @Test
    fun `ExtendedFinancialMetric should handle BigDecimal conversion`() {
        val extended = ExtendedFinancialMetric(
            name = "Gross Profit",
            value = "$25.5M",
            rawValue = "25500000.00",
            category = MetricCategory.GROSS_PROFIT,
            yearOverYearChange = "5.25"
        )

        assertEquals(BigDecimal("25500000.00"), extended.getRawValueBigDecimal())
        assertEquals(BigDecimal("5.25"), extended.getYoyChangeBigDecimal())
    }

    @Test
    fun `ExtendedFinancialMetric with null values should return null`() {
        val extended = ExtendedFinancialMetric(
            name = "Unknown",
            value = "N/A",
            category = MetricCategory.REVENUE
        )

        assertNull(extended.getRawValueBigDecimal())
        assertNull(extended.getYoyChangeBigDecimal())
    }

    @Test
    fun `MetricUnit enum should have all expected values`() {
        val units = MetricUnit.values()

        assertTrue(units.contains(MetricUnit.DOLLARS))
        assertTrue(units.contains(MetricUnit.THOUSANDS))
        assertTrue(units.contains(MetricUnit.MILLIONS))
        assertTrue(units.contains(MetricUnit.BILLIONS))
        assertTrue(units.contains(MetricUnit.PERCENTAGE))
        assertTrue(units.contains(MetricUnit.SHARES))
        assertTrue(units.contains(MetricUnit.PER_SHARE))
        assertTrue(units.contains(MetricUnit.RATIO))
        assertTrue(units.contains(MetricUnit.NONE))
    }

    @Test
    fun `MetricCategory should include all income statement types`() {
        assertNotNull(MetricCategory.REVENUE)
        assertNotNull(MetricCategory.NET_INCOME)
        assertNotNull(MetricCategory.GROSS_PROFIT)
        assertNotNull(MetricCategory.OPERATING_INCOME)
        assertNotNull(MetricCategory.COST_OF_REVENUE)
        assertNotNull(MetricCategory.EBITDA)
    }

    @Test
    fun `MetricCategory should include all balance sheet types`() {
        assertNotNull(MetricCategory.TOTAL_ASSETS)
        assertNotNull(MetricCategory.TOTAL_LIABILITIES)
        assertNotNull(MetricCategory.CASH_AND_EQUIVALENTS)
        assertNotNull(MetricCategory.CURRENT_ASSETS)
        assertNotNull(MetricCategory.CURRENT_LIABILITIES)
        assertNotNull(MetricCategory.TOTAL_EQUITY)
    }

    @Test
    fun `MetricCategory should include all cash flow types`() {
        assertNotNull(MetricCategory.OPERATING_CASH_FLOW)
        assertNotNull(MetricCategory.INVESTING_CASH_FLOW)
        assertNotNull(MetricCategory.FINANCING_CASH_FLOW)
        assertNotNull(MetricCategory.FREE_CASH_FLOW)
        assertNotNull(MetricCategory.CAPITAL_EXPENDITURES)
    }

    @Test
    fun `PeriodType enum should have correct values`() {
        assertEquals(4, PeriodType.values().size)

        assertNotNull(PeriodType.QUARTERLY)
        assertNotNull(PeriodType.ANNUAL)
        assertNotNull(PeriodType.YTD)
        assertNotNull(PeriodType.TTM)
    }

    @Test
    fun `ExtendedFinancialMetric should use default values`() {
        val metric = ExtendedFinancialMetric(
            name = "Test",
            value = "100",
            category = MetricCategory.REVENUE
        )

        assertEquals(MetricUnit.DOLLARS, metric.unit)
        assertEquals("", metric.source)
        assertEquals(1.0, metric.confidence)
        assertEquals("", metric.context)
    }
}
