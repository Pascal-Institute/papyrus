package papyrus.core.service.analyzer

import papyrus.core.model.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Trend analysis engine with BigDecimal precision
 * Following AGENTS.MD Principle 4: Financial Precision & Validation
 */
object TrendAnalyzer {
    
    private val DECIMAL_SCALE = 4
    private val PERCENTAGE_SCALE = 2
    
    /**
     * Calculate Year-over-Year growth rate with validation
     */
    fun calculateYoYGrowth(current: BigDecimal, previous: BigDecimal): GrowthMetric? {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return null
        
        val growth = current.subtract(previous)
                .divide(previous.abs(), DECIMAL_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
        
        // Sanity check: growth > 1000% is suspicious
        if (growth.abs() > BigDecimal("1000")) {
            println("WARNING: YoY growth $growth% seems unrealistic")
        }
        
        return GrowthMetric.create(
            periodType = "YoY",
            growthRate = growth,
            absoluteChange = current.subtract(previous),
            interpretation = interpretGrowthRate(growth, "YoY")
        )
    }
    
    /**
     * Calculate Quarter-over-Quarter growth rate
     */
    fun calculateQoQGrowth(currentQ: BigDecimal, previousQ: BigDecimal): GrowthMetric? {
        if (previousQ.compareTo(BigDecimal.ZERO) == 0) return null
        
        val growth = currentQ.subtract(previousQ)
                .divide(previousQ.abs(), DECIMAL_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
        
        return GrowthMetric.create(
            periodType = "QoQ",
            growthRate = growth,
            absoluteChange = currentQ.subtract(previousQ),
            interpretation = interpretGrowthRate(growth, "QoQ")
        )
    }
    
    /**
     * Calculate Compound Annual Growth Rate (CAGR)
     */
    fun calculateCAGR(
        endValue: BigDecimal,
        beginValue: BigDecimal,
        years: Int
    ): GrowthMetric? {
        if (beginValue.compareTo(BigDecimal.ZERO) <= 0 || years <= 0) return null
        
        // CAGR = (End / Begin)^(1/years) - 1
        val ratio = endValue.divide(beginValue, 6, RoundingMode.HALF_UP).toDouble()
        val cagr = (Math.pow(ratio, 1.0 / years) - 1) * 100
        val cagrDecimal = BigDecimal.valueOf(cagr).setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
        
        return GrowthMetric.create(
            periodType = "${years}Y CAGR",
            growthRate = cagrDecimal,
            absoluteChange = endValue.subtract(beginValue),
            interpretation = interpretGrowthRate(cagrDecimal, "CAGR")
        )
    }
    
    /**
     * Detect anomalies in metric values
     */
    fun detectAnomalies(
        metricName: String,
        currentValue: BigDecimal,
        historicalValues: List<BigDecimal>
    ): AnomalyDetection {
        if (historicalValues.size < 3) {
            return AnomalyDetection.create(
                isAnomaly = false,
                severity = AnomalySeverity.NONE,
                description = "Insufficient historical data for anomaly detection"
            )
        }
        
        val mean = historicalValues.reduce { acc, value -> acc.add(value) }
                .divide(BigDecimal(historicalValues.size), DECIMAL_SCALE, RoundingMode.HALF_UP)
        
        // Calculate standard deviation
        val variance = historicalValues
                .map { it.subtract(mean).pow(2) }
                .reduce { acc, value -> acc.add(value) }
                .divide(BigDecimal(historicalValues.size), DECIMAL_SCALE, RoundingMode.HALF_UP)
        val stdDev = BigDecimal(Math.sqrt(variance.toDouble()))
        
        val zScore = if (stdDev.compareTo(BigDecimal.ZERO) > 0) {
            currentValue.subtract(mean)
                    .divide(stdDev, DECIMAL_SCALE, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        val severity = when {
            zScore.abs() > BigDecimal("3") -> AnomalySeverity.CRITICAL
            zScore.abs() > BigDecimal("2") -> AnomalySeverity.HIGH
            zScore.abs() > BigDecimal("1.5") -> AnomalySeverity.MEDIUM
            else -> AnomalySeverity.NONE
        }
        
        val isAnomaly = severity != AnomalySeverity.NONE
        
        val description = if (isAnomaly) {
            val direction = if (zScore > BigDecimal.ZERO) "higher" else "lower"
            "$metricName is ${zScore.abs()}σ $direction than historical average (Mean: ${mean.setScale(2, RoundingMode.HALF_UP)})"
        } else {
            "$metricName is within normal range"
        }
        
        return AnomalyDetection.create(
            isAnomaly = isAnomaly,
            severity = severity,
            description = description,
            zScore = zScore,
            historicalMean = mean,
            standardDeviation = stdDev
        )
    }
    
    /**
     * Calculate margin trends with precision
     */
    fun calculateMarginTrend(
        revenue: List<BigDecimal>,
        costs: List<BigDecimal>
    ): MarginTrend? {
        if (revenue.size != costs.size || revenue.size < 2) return null
        
        val margins = revenue.zip(costs).mapNotNull { (rev, cost) ->
            if (rev.compareTo(BigDecimal.ZERO) > 0) {
                rev.subtract(cost).divide(rev, DECIMAL_SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal("100"))
                        .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
            } else null
        }
        
        if (margins.size < 2) return null
        
        val latestMargin = margins.last()
        val firstMargin = margins.first()
        val marginChange = latestMargin.subtract(firstMargin)
        
        val trend = when {
            marginChange > BigDecimal("2") -> TrendDirection.IMPROVING
            marginChange < BigDecimal("-2") -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
        
        val volatility = calculateVolatility(margins)
        
        return MarginTrend.create(
            currentMargin = latestMargin,
            periodChange = marginChange,
            trend = trend,
            volatility = volatility,
            interpretation = interpretMarginTrend(latestMargin, marginChange, trend)
        )
    }
    
    private fun calculateVolatility(values: List<BigDecimal>): BigDecimal {
        if (values.size < 2) return BigDecimal.ZERO
        
        val changes = values.zipWithNext { a, b -> b.subtract(a).abs() }
        return changes.reduce { acc, change -> acc.add(change) }
                .divide(BigDecimal(changes.size), PERCENTAGE_SCALE, RoundingMode.HALF_UP)
    }
    
    private fun interpretGrowthRate(rate: BigDecimal, periodType: String): String {
        return when (periodType) {
            "YoY" -> when {
                rate > BigDecimal("20") -> "🚀 Exceptional growth (${rate}%)"
                rate > BigDecimal("10") -> "📈 Strong growth (${rate}%)"
                rate > BigDecimal("5") -> "✅ Moderate growth (${rate}%)"
                rate > BigDecimal("0") -> "➡️ Slight growth (${rate}%)"
                rate > BigDecimal("-5") -> "⚠️ Slight decline (${rate}%)"
                else -> "🚨 Significant decline (${rate}%)"
            }
            "QoQ" -> when {
                rate > BigDecimal("15") -> "🚀 Exceptional quarterly growth (${rate}%)"
                rate > BigDecimal("5") -> "📈 Strong quarter (${rate}%)"
                rate > BigDecimal("0") -> "✅ Positive quarter (${rate}%)"
                rate > BigDecimal("-5") -> "⚠️ Weak quarter (${rate}%)"
                else -> "🚨 Poor quarter (${rate}%)"
            }
            else -> when {
                rate > BigDecimal("15") -> "🏆 Outstanding CAGR (${rate}%)"
                rate > BigDecimal("10") -> "⭐ Excellent CAGR (${rate}%)"
                rate > BigDecimal("5") -> "✅ Good CAGR (${rate}%)"
                else -> "➡️ Modest CAGR (${rate}%)"
            }
        }
    }
    
    private fun interpretMarginTrend(
        current: BigDecimal,
        change: BigDecimal,
        trend: TrendDirection
    ): String {
        val level = when {
            current > BigDecimal("30") -> "excellent"
            current > BigDecimal("20") -> "strong"
            current > BigDecimal("10") -> "moderate"
            current > BigDecimal("5") -> "weak"
            else -> "concerning"
        }
        
        val direction = when (trend) {
            TrendDirection.IMPROVING -> "and improving"
            TrendDirection.DECLINING -> "but declining"
            TrendDirection.STABLE -> "and stable"
        }
        
        return "Margin is $level (${current}%) $direction (${if (change >= BigDecimal.ZERO) "+" else ""}${change}pp)"
    }
}

@kotlinx.serialization.Serializable
data class GrowthMetric(
    val periodType: String,
    val growthRateString: String,
    val absoluteChangeString: String,
    val interpretation: String
) {
    val growthRate: BigDecimal get() = BigDecimal(growthRateString)
    val absoluteChange: BigDecimal get() = BigDecimal(absoluteChangeString)
    
    companion object {
        fun create(periodType: String, growthRate: BigDecimal, absoluteChange: BigDecimal, interpretation: String) =
            GrowthMetric(periodType, growthRate.toString(), absoluteChange.toString(), interpretation)
    }
}

@kotlinx.serialization.Serializable
data class AnomalyDetection(
    val isAnomaly: Boolean,
    val severity: AnomalySeverity,
    val description: String,
    val zScoreString: String? = null,
    val historicalMeanString: String? = null,
    val standardDeviationString: String? = null
) {
    val zScore: BigDecimal? get() = zScoreString?.let { BigDecimal(it) }
    val historicalMean: BigDecimal? get() = historicalMeanString?.let { BigDecimal(it) }
    val standardDeviation: BigDecimal? get() = standardDeviationString?.let { BigDecimal(it) }
    
    companion object {
        fun create(
            isAnomaly: Boolean,
            severity: AnomalySeverity,
            description: String,
            zScore: BigDecimal? = null,
            historicalMean: BigDecimal? = null,
            standardDeviation: BigDecimal? = null
        ) = AnomalyDetection(
            isAnomaly,
            severity,
            description,
            zScore?.toString(),
            historicalMean?.toString(),
            standardDeviation?.toString()
        )
    }
}

enum class AnomalySeverity {
    NONE,
    MEDIUM,
    HIGH,
    CRITICAL
}

@kotlinx.serialization.Serializable
data class MarginTrend(
    val currentMarginString: String,
    val periodChangeString: String,
    val trend: TrendDirection,
    val volatilityString: String,
    val interpretation: String
) {
    val currentMargin: BigDecimal get() = BigDecimal(currentMarginString)
    val periodChange: BigDecimal get() = BigDecimal(periodChangeString)
    val volatility: BigDecimal get() = BigDecimal(volatilityString)
    
    companion object {
        fun create(
            currentMargin: BigDecimal,
            periodChange: BigDecimal,
            trend: TrendDirection,
            volatility: BigDecimal,
            interpretation: String
        ) = MarginTrend(
            currentMargin.toString(),
            periodChange.toString(),
            trend,
            volatility.toString(),
            interpretation
        )
    }
}

enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
}
