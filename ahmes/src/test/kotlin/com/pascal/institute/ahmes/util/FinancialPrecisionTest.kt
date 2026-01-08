package com.pascal.institute.ahmes.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Tests for Financial Precision Utilities
 */
class FinancialPrecisionTest {

    @Test
    fun `createMoney from BigDecimal should work correctly`() {
        val amount = BigDecimal("1234.56")
        val money = FinancialPrecision.createMoney(amount)

        assertNotNull(money)
        assertEquals(BigDecimal("1234.56").setScale(2, RoundingMode.HALF_UP),
            money.number.numberValue(BigDecimal::class.java))
    }

    @Test
    fun `createMoney from Double should work correctly`() {
        val money = FinancialPrecision.createMoney(1234.56)

        assertNotNull(money)
    }

    @Test
    fun `createMoneyFromString should parse currency strings`() {
        val money = FinancialPrecision.createMoneyFromString("$1,234.56")

        assertNotNull(money)
        assertEquals(BigDecimal("1234.56").setScale(2, RoundingMode.HALF_UP),
            money?.number?.numberValue(BigDecimal::class.java))
    }

    @Test
    fun `createMoneyFromString should handle negative with parentheses`() {
        val money = FinancialPrecision.createMoneyFromString("($500.00)")

        assertNotNull(money)
        val value = money?.number?.numberValue(BigDecimal::class.java)
        assertTrue(value != null && value < BigDecimal.ZERO)
    }

    @Test
    fun `createMoneyFromString should return null for invalid input`() {
        assertNull(FinancialPrecision.createMoneyFromString("N/A"))
        assertNull(FinancialPrecision.createMoneyFromString("--"))
        assertNull(FinancialPrecision.createMoneyFromString(""))
        assertNull(FinancialPrecision.createMoneyFromString("—"))
    }

    @Test
    fun `parseSecValue should handle millions`() {
        val money = FinancialPrecision.parseSecValue("1500", "millions")

        assertNotNull(money)
        val value = money?.number?.numberValue(BigDecimal::class.java)
        // Using compareTo for BigDecimal comparison
        assertTrue(BigDecimal("1500000000.00").compareTo(value) == 0, "Expected 1500000000.00 but got $value")
    }

    @Test
    fun `parseSecValue should handle billions`() {
        val money = FinancialPrecision.parseSecValue("2.5", "billions")

        assertNotNull(money)
        val value = money?.number?.numberValue(BigDecimal::class.java)
        assertTrue(BigDecimal("2500000000.00").compareTo(value) == 0, "Expected 2500000000.00 but got $value")
    }

    @Test
    fun `parseSecValue should handle thousands`() {
        val money = FinancialPrecision.parseSecValue("500", "thousands")

        assertNotNull(money)
        val value = money?.number?.numberValue(BigDecimal::class.java)
        assertTrue(BigDecimal("500000.00").compareTo(value) == 0, "Expected 500000.00 but got $value")
    }

    @Test
    fun `parseSecValue should return null for invalid values`() {
        assertNull(FinancialPrecision.parseSecValue("-", "millions"))
        assertNull(FinancialPrecision.parseSecValue("—", "millions"))
        assertNull(FinancialPrecision.parseSecValue("n/a", "millions"))
    }

    @Test
    fun `calculatePercentage should compute correctly`() {
        val numerator = FinancialPrecision.createMoney(BigDecimal("50"))
        val denominator = FinancialPrecision.createMoney(BigDecimal("200"))

        val percentage = FinancialPrecision.calculatePercentage(numerator, denominator)

        assertNotNull(percentage)
        assertEquals(BigDecimal("25.00"), percentage)
    }

    @Test
    fun `calculatePercentage should return null for zero denominator`() {
        val numerator = FinancialPrecision.createMoney(BigDecimal("50"))
        val denominator = FinancialPrecision.createMoney(BigDecimal("0"))

        val percentage = FinancialPrecision.calculatePercentage(numerator, denominator)

        assertNull(percentage)
    }

    @Test
    fun `calculatePercentageChange should compute YoY correctly`() {
        val current = FinancialPrecision.createMoney(BigDecimal("120"))
        val previous = FinancialPrecision.createMoney(BigDecimal("100"))

        val change = FinancialPrecision.calculatePercentageChange(current, previous)

        assertNotNull(change)
        assertEquals(BigDecimal("20.00"), change)
    }

    @Test
    fun `calculatePercentageChange should handle negative change`() {
        val current = FinancialPrecision.createMoney(BigDecimal("75"))
        val previous = FinancialPrecision.createMoney(BigDecimal("100"))

        val change = FinancialPrecision.calculatePercentageChange(current, previous)

        assertNotNull(change)
        assertEquals(BigDecimal("-25.00"), change)
    }

    @Test
    fun `calculatePercentageChange should return null for zero previous`() {
        val current = FinancialPrecision.createMoney(BigDecimal("100"))
        val previous = FinancialPrecision.createMoney(BigDecimal("0"))

        val change = FinancialPrecision.calculatePercentageChange(current, previous)

        assertNull(change)
    }
}
