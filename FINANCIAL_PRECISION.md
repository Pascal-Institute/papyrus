# Financial Precision Improvements

## Overview

This document describes the financial precision improvements implemented in Papyrus following **AGENTS.MD Principle 4: Embed Absolute Financial Precision**.

## Changes Summary

### 1. Dependencies Added (`build.gradle.kts`)
- **JavaMoney (Moneta)**: Precise monetary calculations using `BigDecimal`
  ```kotlin
  implementation("org.javamoney:moneta:1.4.2")
  implementation("javax.money:money-api:1.1")
  ```

### 2. New Utility Classes

#### `FinancialPrecision.kt`
Provides core financial calculation utilities with absolute precision:

**Key Methods:**
- `createMoney()`: Create monetary amounts from Double, BigDecimal, or String
- `parseSecValue()`: Parse SEC-style financial values with unit scaling (millions, billions, etc.)
- `calculatePercentage()`: Calculate percentages with proper precision
- `calculatePercentageChange()`: Calculate YoY growth rates with validation
- `calculateRatio()`: Calculate financial ratios (debt-to-equity, current ratio, etc.)
- `validateFinancialAmount()`: Sanity checks to prevent impossible values

**Example Usage:**
```kotlin
// Parse a value from SEC document
val revenue = FinancialPrecision.parseSecValue("1,234.56", "millions", "USD")
// Result: MonetaryAmount of $1,234,560,000.00

// Calculate growth rate
val currentRevenue = FinancialPrecision.createMoney(BigDecimal("1000000"))
val priorRevenue = FinancialPrecision.createMoney(BigDecimal("800000"))
val growthRate = FinancialPrecision.calculatePercentageChange(currentRevenue, priorRevenue)
// Result: BigDecimal("25.00") representing 25% growth
```

#### `FinancialCalculations.kt`
Provides validated financial ratio calculations:

**Key Methods:**
- `calculateKeyMetrics()`: Calculate all key financial ratios from structured data
- Profitability ratios: Gross Margin, Operating Margin, Net Profit Margin, ROA, ROE
- Liquidity ratios: Current Ratio, Quick Ratio, Cash Ratio
- Solvency ratios: Debt-to-Equity, Debt Ratio, Interest Coverage
- Efficiency ratios: Asset Turnover

### 3. Model Updates

#### `ExtendedFinancialMetric`
- **Before**: `rawValue: Double?`, `yearOverYearChange: Double?`
- **After**: `rawValue: String?`, `yearOverYearChange: String?` (BigDecimal stored as String for serialization)
- Added helper methods:
  - `getRawValueBigDecimal(): BigDecimal?`
  - `getYoyChangeBigDecimal(): BigDecimal?`

#### `MonetaryValue`
- **Before**: `amount: Double`
- **After**: `amount: String` (BigDecimal precision), `currency: String = "USD"`
- Added fields:
  - `source: String` for traceability
  - `yearOverYearChange: String?` as BigDecimal
- Added factory methods:
  - `fromDouble()`: Backward compatibility
  - `fromBigDecimal()`: Preferred method for precision
  - `fromMonetaryAmount()`: Integration with JavaMoney
- Added helper methods:
  - `toBigDecimal(): BigDecimal`
  - `getYoyChangeBigDecimal(): BigDecimal?`

### 4. Parser Updates

#### `EnhancedFinancialParser.kt`
- Updated `parseSecValue()` to use `FinancialPrecision.parseSecValue()` returning `Big Decimal`
- Updated `formatValue()` to accept `BigDecimal` instead of `Double`
- Updated YoY calculation to use `BigDecimal` arithmetic:
  ```kotlin
  currentValue.subtract(priorValue)
      .divide(priorValue.abs(), 10, RoundingMode.HALF_UP)
      .multiply(BigDecimal("100"))
      .setScale(2, RoundingMode.HALF_UP)
  ```

## Benefits

### 1. **Precision**
- No floating-point rounding errors
- Accurate calculations to the penny
- Proper handling of large financial values

### 2. **Validation**
- Sanity checks prevent impossible values (e.g., revenue > $10 trillion)
- Growth rate validation (e.g., > 1000% throws exception)
- Ratio validation within plausible bounds

### 3. **Traceability**
- Source tracking in `MonetaryValue` and `ExtendedFinancialMetric`
- Clear audit trail for financial data
- Confidence scoring for parsed values

### 4. **Compliance**
- Follows AGENTS.MD Principle 4
- Uses industry-standard JavaMoney (JSR 354)
- Proper currency handling

## Migration Guide

### For New Code
Use the new precision utilities:

```kotlin
// Instead of:
val revenue = 1234560000.0

// Use:
val revenue = FinancialPrecision.createMoney(BigDecimal("1234560000"))
```

### For Existing Code
Models maintain backward compatibility:

```kotlin
// Old code still works:
MonetaryValue.fromDouble(1234560000.0, MetricUnit.DOLLARS)

// But prefer:
MonetaryValue.fromBigDecimal(BigDecimal("1234560000"), MetricUnit.DOLLARS)
```

### Accessing Values
When reading from models:

```kotlin
val metric: ExtendedFinancialMetric = // ...

// Get as BigDecimal for calculations:
val value: BigDecimal? = metric.getRawValueBigDecimal()

// Get YoY change:
val growth: BigDecimal? = metric.getYoyChangeBigDecimal()
```

## Testing Recommendations

1. **Unit Tests**: Verify precision in calculations
2. **Integration Tests**: Ensure parser correctly uses BigDecimal
3. **Validation Tests**: Confirm sanity checks catch impossible values

## Future Enhancements

1. Multi-currency support (currently USD-only)
2. Currency conversion using exchange rates
3. Historical currency rate tracking
4. Additional validation rules for industry-specific ratios

## References

- AGENTS.MD: Principle 4 - Embed Absolute Financial Precision
- JSR 354: Money and Currency API
- JavaMoney Documentation: https://javamoney.github.io/
