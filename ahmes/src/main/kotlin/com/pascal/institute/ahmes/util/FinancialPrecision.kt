package com.pascal.institute.ahmes.util

import java.math.BigDecimal
import java.math.RoundingMode
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount
import org.javamoney.moneta.Money

/**
 * Financial Precision Utilities
 *
 * AGENTS.MD Principle 4: Embed Absolute Financial Precision
 * - Use BigDecimal to prevent rounding errors
 * - Handle data with integrity using precise data types
 * - Implement financial validation checks
 * - Maintain traceability
 */
object FinancialPrecision {

    private val USD: CurrencyUnit = Monetary.getCurrency("USD")
    private const val DEFAULT_SCALE = 2
    private const val CALCULATION_SCALE = 10

    // Sanity check thresholds
    private val MAX_PLAUSIBLE_REVENUE = BigDecimal("10000000000000") // $10 trillion
    private val MAX_PLAUSIBLE_GROWTH_RATE = BigDecimal("1000") // 1000% (10x)
    private val MIN_PLAUSIBLE_RATIO = BigDecimal("-100") // -100%

    /**
     * Create a monetary amount from a double value
     * WARNING: Only use this when parsing external data. Prefer string-based creation.
     */
    fun createMoney(amount: Double, currencyCode: String = "USD"): MonetaryAmount {
        return Money.of(
            BigDecimal(amount.toString()).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP),
            currencyCode
        )
    }

    /** Create a monetary amount from a BigDecimal (preferred method) */
    fun createMoney(amount: BigDecimal, currencyCode: String = "USD"): MonetaryAmount {
        return Money.of(amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), currencyCode)
    }

    /** Create a monetary amount from a string (safest method) */
    fun createMoneyFromString(amount: String, currencyCode: String = "USD"): MonetaryAmount? {
        return try {
            val cleaned = cleanFinancialString(amount)
            val isNegative = cleaned.startsWith("-") || cleaned.startsWith("(")
            val numberStr = cleaned.replace(Regex("[^0-9.]"), "")

            if (numberStr.isEmpty()) return null

            var value = BigDecimal(numberStr)
            if (isNegative && value > BigDecimal.ZERO) {
                value = value.negate()
            }

            Money.of(value.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), currencyCode)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse SEC-style financial values with unit scaling
     * e.g., "1,234.56" in millions = 1,234,560,000
     */
    fun parseSecValue(
        valueStr: String,
        unit: String,
        currencyCode: String = "USD"
    ): MonetaryAmount? {
        val cleaned = cleanFinancialString(valueStr)
        if (cleaned.isEmpty() || cleaned in listOf("-", "—", "–", "n/a")) return null

        return try {
            val isNegative = cleaned.startsWith("(") && cleaned.endsWith(")")
            val numberStr = cleaned.replace(Regex("[^0-9.]"), "")

            var value = BigDecimal(numberStr)

            // Apply unit scaling
            value = when (unit.lowercase()) {
                "billions", "b" -> value.multiply(BigDecimal("1000000000"))
                "millions", "m" -> value.multiply(BigDecimal("1000000"))
                "thousands", "k" -> value.multiply(BigDecimal("1000"))
                else -> value
            }

            if (isNegative) {
                value = value.negate()
            }

            // Validation
            validateFinancialAmount(value)

            Money.of(value.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), currencyCode)
        } catch (e: Exception) {
            null
        }
    }

    /** Calculate percentage with proper precision */
    fun calculatePercentage(numerator: MonetaryAmount, denominator: MonetaryAmount): BigDecimal? {
        if (denominator.isZero ||
            denominator.number.numberValue(BigDecimal::class.java) == BigDecimal.ZERO
        ) {
            return null
        }

        val num = numerator.number.numberValue(BigDecimal::class.java)
        val denom = denominator.number.numberValue(BigDecimal::class.java)

        return num.divide(denom, CALCULATION_SCALE, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP)
    }

    /** Calculate percentage change (Year-over-Year growth, etc.) */
    fun calculatePercentageChange(
        currentValue: MonetaryAmount,
        previousValue: MonetaryAmount
    ): BigDecimal? {
        if (previousValue.isZero) return null

        val current = currentValue.number.numberValue(BigDecimal::class.java)
        val previous = previousValue.number.numberValue(BigDecimal::class.java)

        val change = current.subtract(previous)
        val percentChange = change.divide(previous.abs(), CALCULATION_SCALE, RoundingMode.HALF_UP)
            .multiply(BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP)

        // Validation
        if (percentChange.abs() > MAX_PLAUSIBLE_GROWTH_RATE) {
            throw IllegalFinancialStateException(
                "Implausible growth rate: ${percentChange}% exceeds maximum threshold"
            )
        }

        return percentChange
    }

    /** Calculate ratio (e.g., debt-to-equity, current ratio) */
    fun calculateRatio(numerator: MonetaryAmount, denominator: MonetaryAmount): BigDecimal? {
        if (denominator.isZero) return null

        val num = numerator.number.numberValue(BigDecimal::class.java)
        val denom = denominator.number.numberValue(BigDecimal::class.java)

        return num.divide(denom, CALCULATION_SCALE, RoundingMode.HALF_UP)
            .setScale(4, RoundingMode.HALF_UP)
    }

    /** Format monetary amount for display */
    fun formatMoney(amount: MonetaryAmount): String {
        val value = amount.number.numberValue(BigDecimal::class.java)
        val absValue = value.abs()

        val formatted = when {
            absValue >= BigDecimal("1000000000") ->
                "$${absValue.divide(BigDecimal("1000000000"), 2, RoundingMode.HALF_UP)}B"
            absValue >= BigDecimal("1000000") ->
                "$${absValue.divide(BigDecimal("1000000"), 2, RoundingMode.HALF_UP)}M"
            absValue >= BigDecimal("1000") ->
                "$${absValue.divide(BigDecimal("1000"), 2, RoundingMode.HALF_UP)}K"
            else -> "$${absValue.setScale(2, RoundingMode.HALF_UP)}"
        }

        return if (value < BigDecimal.ZERO) "-$formatted" else formatted
    }

    /**
     * Validate financial amount for plausibility
     * AGENTS.MD Principle 4: Financial Validation Checks
     */
    private fun validateFinancialAmount(amount: BigDecimal) {
        val absAmount = amount.abs()

        if (absAmount > MAX_PLAUSIBLE_REVENUE) {
            throw IllegalFinancialStateException(
                "Implausible financial amount: $${absAmount} exceeds maximum threshold"
            )
        }
    }

    /** Clean financial string for parsing */
    private fun cleanFinancialString(input: String): String {
        return input.trim()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")
            .replace("\u00A0", "") // Non-breaking space
    }

    /** Convert BigDecimal to Double (use sparingly, only for compatibility) */
    @Deprecated("Use BigDecimal for precision")
    fun toDouble(amount: MonetaryAmount): Double {
        return amount.number.numberValue(BigDecimal::class.java).toDouble()
    }
}

/** Exception for financial validation failures */
class IllegalFinancialStateException(message: String) : Exception(message)
