# AGENTS.md

## Development Principles for Agents

### Core Guideline

**Write code that is intuitive, concise, and self-explanatory.**

Every line of code in an agent should make its intent obvious to anyone reading it — including your future self.

---

### Key Practices

#### 1. Be Intuitive

- Favor readability over cleverness.
- Choose straightforward logic and common patterns.
- Avoid unnecessary abstraction or over-engineering.
- A new team member should understand the agent's behavior within minutes.

#### 2. Be Concise

- Eliminate redundant code and boilerplate.
- Use meaningful defaults and sensible shortcuts.
- Keep functions short and focused (ideally < 30 lines).
- Remove commented-out code and unused imports.

#### 3. Make the Code Itself Meaningful

- Use descriptive, domain-specific names for variables, functions, and classes.
- **Bad:** `data`, `tmp`, `handle_result`
- **Good:** `user_query`, `search_results`, `summarize_findings`
- Structure code so the flow tells the story:
  ```kotlin
  val query = extractUserIntent(message)
  val results = searchFinancialTools(query)
  val response = generateReply(results)
  return response
  ```

#### 4. Embed Absolute Financial Precision

- **Codify Financial Logic:** Ensure that core financial principles (e.g., accounting standards, IRR/NPV formulas, tax regulations) are accurately translated into the business logic, not just treated as strings.
- **Handle Data with Integrity:** Use precise data types (e.g., `BigDecimal`) to prevent rounding errors inherent in floating-point math. Always explicitly define currency units and decimal precision.
- **Financial Validation Checks:** Implement "sanity checks" to verify that outputs are financially plausible. Prevent the agent from providing "hallucinated" or mathematically impossible financial results.
- **Traceability:** Maintain clear references to the sources of financial data (e.g., specific regulatory filings or market indices) within the code or metadata to ensure auditability.

#### 5. Consult Sample SEC Reports for Data Extraction
- Resolve Ambiguity with Real Data: When it is unclear which specific values or insights to extract from SEC filings, do not rely on assumptions.

- Reference Authority: Analyze the actual SEC reports located in src\main\resources\samples to understand the standard data structures and reporting formats.

- Model Development based on Samples: Use these samples to determine the most relevant financial metrics and narrative elements to target, ensuring the agent extracts high-value information that reflects real-world reporting.

---

### Implementation Example (Kotlin)

The following example demonstrates how to apply **Principle 4** by using `BigDecimal` for precision and implementing a validation layer to ensure financial soundness.

```kotlin
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Example of Principle 4: Financial Precision & Validation
 */
class FinancialAgent {

    // Define constants with precision to avoid floating point errors
    private val CAPITAL_GAINS_TAX_RATE = BigDecimal("0.22")
    private val MAX_PLAUSIBLE_ANNUAL_ROI = BigDecimal("500.00") // 500% Sanity Check

    fun calculateInvestmentReturn(principal: BigDecimal, totalProfit: BigDecimal): FinancialResult {
        // 1. Data Integrity: Use BigDecimal for commercial rounding (HALF_UP)
        // Avoids the precision issues found in Double or Float
        val taxAmount = totalProfit.multiply(CAPITAL_GAINS_TAX_RATE).setScale(2, RoundingMode.HALF_UP)
        val netProfit = totalProfit.subtract(taxAmount)

        // Calculate ROI: (Net Profit / Principal) * 100
        val roi = netProfit.divide(principal, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))

        // 2. Financial Validation: Guard against hallucinations or calculation anomalies
        validateFinancialSoundness(roi)

        return FinancialResult(netProfit, roi, "USD")
    }

    private fun validateFinancialSoundness(roi: BigDecimal) {
        // Logic check: Is this ROI physically/economically possible for this context?
        // Prevents reporting impossible gains or losses exceeding 100% of principal
        if (roi > MAX_PLAUSIBLE_ANNUAL_ROI || roi < BigDecimal("-100")) {
            throw IllegalFinancialStateException("Anomalous financial result detected: ROI $roi% is outside plausible bounds.")
        }
    }
}

data class FinancialResult(
    val netProfit: BigDecimal,
    val roiPercentage: BigDecimal,
    val currency: String
)

class IllegalFinancialStateException(message: String) : Exception(message)
```
