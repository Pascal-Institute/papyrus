# AGENTS.md

## Project Mission

**The ultimate goal of this project is to derive investment insights from SEC Fillings reports.**

Everything we build—from data extraction to UI display—must serve this purpose. We represent the investor's perspective.
- **Accuracy is non-negotiable**: Insights based on wrong data are dangerous.
- **Clarity is key**: Complex financial data must be distilled into actionable insights.
- **Completeness**: Don't just show numbers; explain "Why" and "So What?".

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

- **Resolve Ambiguity with Real Data:** When it is unclear which specific values or insights to extract from SEC filings, do not rely on assumptions.
- **Reference Authority:** Analyze the actual SEC reports located in `src\main\resources\samples` to understand the standard data structures and reporting formats.
- **Model Development based on Samples:** Use these samples to determine the most relevant financial metrics and narrative elements to target, ensuring the agent extracts high-value information that reflects real-world reporting.

#### 6. Please Write Your Comments in English

---

### Philosophy-Driven Principles

The following principles are inspired by timeless wisdom from legendary investors, explorers, and business leaders. They guide not just what we build, but how we think.

---

#### 7. Radical Truth in Code (Ray Dalio's Principle)

> "The reason we use the word 'radical' is that you need to think this extremely to achieve even the 'minimum' level of openness, transparency, and truth."

**Application to Code:**

- **Expose Errors Explicitly:** Never silently swallow exceptions or return null without explanation. Make failures loud and traceable.
  ```kotlin
  // ❌ Bad: Silent failure
  fun parseFinancialData(data: String): BigDecimal? {
      return try { BigDecimal(data) } catch (e: Exception) { null }
  }

  // ✅ Good: Radical truth
  fun parseFinancialData(data: String): BigDecimal {
      return try {
          BigDecimal(data)
      } catch (e: NumberFormatException) {
          throw FinancialDataParseException("Failed to parse '$data' as BigDecimal. Source: [SEC Filing XYZ, Line 42]", e)
      }
  }
  ```

- **Radical Transparency in Logging:** Log all critical financial calculations with full context (inputs, formula, outputs).
  ```kotlin
  logger.info("ROI Calculation: principal=$principal, profit=$profit, tax=$tax → ROI=$roi%")
  ```

- **Radical Open-mindedness in Code Reviews:** Assume your code might be wrong. Welcome scrutiny. Build validation layers that challenge your own assumptions.

**Why This Matters:**
In investing, a single hidden bug in a financial calculation can lead to catastrophic decisions. Extreme transparency prevents this.

---

#### 8. Build with Margin of Safety (Benjamin Graham's Principle)

> "The future is unknowable. Therefore, to defend yourself, you must pay a price well below intrinsic value." — Benjamin Graham

**Application to Code:**

- **Defensive Programming:** Always validate inputs, even from "trusted" sources.
  ```kotlin
  fun calculatePriceToBookRatio(marketCap: BigDecimal, bookValue: BigDecimal): BigDecimal {
      // Margin of Safety: Guard against division by zero
      require(bookValue > BigDecimal.ZERO) {
          "Book value must be positive. Received: $bookValue"
      }
      return marketCap.divide(bookValue, 4, RoundingMode.HALF_UP)
  }
  ```

- **Graceful Degradation:** If external data sources fail (e.g., SEC API is down), provide cached results with clear warnings rather than crashing.
  ```kotlin
  val filingData = try {
      fetchFromSEC(ticker)
  } catch (e: NetworkException) {
      logger.warn("SEC API unreachable. Using cached data from ${cache.lastUpdated}")
      cache.get(ticker) ?: throw InsufficientDataException("No fallback available")
  }
  ```

- **Sanity Bounds:** Define "impossible" ranges for financial metrics to catch outliers.
  ```kotlin
  private val PLAUSIBLE_PE_RATIO_RANGE = 0.0..1000.0

  fun validatePERatio(pe: Double) {
      if (pe !in PLAUSIBLE_PE_RATIO_RANGE) {
          logger.error("Anomalous P/E ratio detected: $pe. Check data source integrity.")
      }
  }
  ```

**Why This Matters:**
Just as investors buy stocks below intrinsic value to protect against uncertainty, we build systems that tolerate unexpected inputs without breaking.

---

#### 9. Independent Thinking Over Cargo Cult Programming (Contrarian Mindset)

> "To succeed in investing, you must think independently. The same applies to code." — Inspired by Warren Buffett

**Application to Code:**

- **Question Frameworks:** Don't use a framework just because it's popular. Ask: "Does this solve *our* problem, or am I following the herd?"
  ```text
  ❌ "Everyone uses Microservices, so we should too!"
  ✅ "Our data extraction needs are batch-oriented. A monolith with scheduled jobs is simpler and more reliable."
  ```

- **Avoid Groupthink in Abstractions:** If a pattern feels over-engineered for the problem, simplify it.
  ```kotlin
  // ❌ Cargo Cult: Unnecessary abstraction
  interface FinancialDataProcessor {
      fun process(data: RawData): ProcessedData
  }
  class SECDataProcessor : FinancialDataProcessor { ... }
  class FactoryRegistry { ... }

  // ✅ Independent Thinking: Direct and clear
  fun extractFinancialMetrics(secFiling: SECDocument): FinancialMetrics {
      // Simple, focused, and sufficient
  }
  ```

- **Challenge Legacy Code:** Just because code exists doesn't mean it's right. If it violates our principles, refactor it.

**Why This Matters:**
The best investors don't follow trends—they find undervalued opportunities others miss. The best code doesn't follow fads—it solves problems elegantly.

---

#### 10. Learn from Failures Systematically (Ray Dalio's "Pain + Reflection = Progress")

> "Mistakes are okay, but not learning from them is unacceptable."

**Application to Code:**

- **Post-Mortem Documentation:** When a bug causes wrong financial insights, document:
  1. What went wrong?
  2. Why did it happen?
  3. How do we prevent recurrence?

  Create a `LESSONS_LEARNED.md` file tracking critical failures.

- **Automated Regression Tests:** Every bug fix must include a test that prevents the same issue.
  ```kotlin
  @Test
  fun `should not allow negative revenue values`() {
      val invalidData = FinancialMetric(name = "Revenue", value = BigDecimal("-1000"))
      assertThrows<IllegalArgumentException> { validateFinancialMetric(invalidData) }
  }
  ```

- **Embrace "Pre-Mortems":** Before deploying, ask: "How could this feature fail catastrophically?" Then build safeguards.

**Why This Matters:**
In investing, ignoring losses leads to ruin. In code, ignoring bugs leads to wrong data → wrong decisions → investor harm.

---

#### 11. Plan Like Amundsen, Execute with Precision (The Amundsen Mindset)

> "Victory awaits those who have everything in order. People call this luck." — Roald Amundsen

**Application to Code:**

- **Thorough Preparation Before Coding:**
  1. **Define Success Criteria:** What financial insight does this feature unlock?
  2. **Identify Edge Cases:** What if the SEC filing format changes? What if a ticker has no filings?
  3. **Plan Rollback Strategy:** If this deployment fails, how do we revert quickly?

- **Systematic Project Management:**
  ```text
  ✅ Amundsen's 10-Step Project Checklist (Adapted for Code):
  1. Confirm the goal is clear and motivating.
  2. Acquire necessary skills (e.g., learn XBRL parsing if needed).
  3. Build a network of domain experts (accountants, financial analysts).
  4. Plan every milestone with documentation, not assumptions.
  5. Source the best tools (libraries, APIs) — reject mediocrity.
  6. Share the vision with the team. Align on "Why this matters."
  7. Launch at the right time. Don't wait for perfection; iterate.
  8. Anticipate failures. Build retries, fallbacks, and monitoring.
  9. Track progress with metrics. Measure deviation from goals.
  10. Prioritize sustainability: code health, team well-being, knowledge sharing.
  ```

- **Principle of Least Astonishment:** Code should behave exactly as its name/signature suggests. No surprises.
  ```kotlin
  // ❌ Astonishing: Function mutates global state
  fun calculateROI(investment: Investment): BigDecimal {
      globalCache.save(investment) // Unexpected side effect!
      return investment.profit / investment.principal
  }

  // ✅ Predictable: Pure function
  fun calculateROI(profit: BigDecimal, principal: BigDecimal): BigDecimal {
      return profit.divide(principal, 4, RoundingMode.HALF_UP)
  }
  ```

**Why This Matters:**
Amundsen reached the South Pole because he planned obsessively and executed flawlessly. Our code must do the same to deliver reliable financial insights.

---

#### 12. Seek the Essence, Not the Noise (Peter Drucker's "What is needed?")

> "Don't ask 'What is right?' Ask 'What is needed?'" — Peter Drucker

**Application to Code:**

- **Focus on High-Value Features:** A beautiful UI means nothing if the underlying financial calculations are wrong.
  ```text
  Priority Order:
  1. Accurate data extraction (the essence)
  2. Clear presentation of insights (the need)
  3. Visual polish (the nice-to-have)
  ```

- **Eliminate Complexity That Doesn't Serve the Mission:**
  ```kotlin
  // ❌ Over-engineered: Adds complexity without value
  class FinancialMetricFactoryBuilder { ... }

  // ✅ Essential: Directly serves investor needs
  data class FinancialMetric(val name: String, val value: BigDecimal, val unit: String)
  ```

- **Question Every Abstraction:** Does this abstraction make the code more understandable, or just more abstract?

**Why This Matters:**
In investing, noise kills signal. In code, unnecessary complexity kills maintainability.

---

### Implementation Example (Enhanced with Philosophy)

```kotlin
import java.math.BigDecimal
import java.math.RoundingMode
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Demonstrates Principles 4, 7, 8, and 11:
 * - Financial Precision (BigDecimal)
 * - Radical Truth (Explicit errors)
 * - Margin of Safety (Validation)
 * - Amundsen Planning (Clear preconditions & logging)
 */
class FinancialAgent {

    private val CAPITAL_GAINS_TAX_RATE = BigDecimal("0.22")
    private val MAX_PLAUSIBLE_ANNUAL_ROI = BigDecimal("500.00") // 500% sanity check
    private val MIN_PLAUSIBLE_ANNUAL_ROI = BigDecimal("-100.00") // -100% (total loss)

    /**
     * Calculates net investment return after tax.
     *
     * Preconditions (Amundsen Principle):
     * - principal > 0
     * - totalProfit can be negative (loss scenario)
     *
     * @throws IllegalArgumentException if preconditions violated
     * @throws IllegalFinancialStateException if result is financially implausible
     */
    fun calculateInvestmentReturn(principal: BigDecimal, totalProfit: BigDecimal): FinancialResult {
        // Principle 8: Margin of Safety - Defensive validation
        require(principal > BigDecimal.ZERO) {
            "Principal must be positive. Received: $principal"
        }

        // Principle 4: Financial Precision - Use BigDecimal with explicit rounding
        val taxAmount = totalProfit.multiply(CAPITAL_GAINS_TAX_RATE).setScale(2, RoundingMode.HALF_UP)
        val netProfit = totalProfit.subtract(taxAmount)
        val roi = netProfit.divide(principal, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100"))

        // Principle 7: Radical Truth - Log all critical calculations
        logger.info { "ROI Calculation: principal=$principal, totalProfit=$totalProfit, tax=$taxAmount, netProfit=$netProfit, ROI=$roi%" }

        // Principle 8: Margin of Safety - Guard against anomalies
        validateFinancialSoundness(roi)

        return FinancialResult(netProfit, roi, "USD")
    }

    /**
     * Principle 7: Radical Truth in Validation
     * Don't silently accept impossible values. Make errors explicit and actionable.
     */
    private fun validateFinancialSoundness(roi: BigDecimal) {
        if (roi > MAX_PLAUSIBLE_ANNUAL_ROI) {
            // Principle 7: Radical Truth - Expose the problem loudly
            throw IllegalFinancialStateException(
                "Anomalous ROI detected: $roi% exceeds plausible maximum of $MAX_PLAUSIBLE_ANNUAL_ROI%. " +
                "Possible causes: Data corruption, calculation error, or extraordinary event requiring manual review."
            )
        }
        if (roi < MIN_PLAUSIBLE_ANNUAL_ROI) {
            throw IllegalFinancialStateException(
                "Anomalous ROI detected: $roi% is below plausible minimum of $MIN_PLAUSIBLE_ANNUAL_ROI%. " +
                "Loss cannot exceed 100% of principal in standard scenarios."
            )
        }
    }
}

/**
 * Principle 3: Make the Code Meaningful
 * Clear domain model with explicit units and precision.
 */
data class FinancialResult(
    val netProfit: BigDecimal,
    val roiPercentage: BigDecimal,
    val currency: String
)

/**
 * Principle 7: Radical Truth
 * Custom exception makes financial errors traceable and actionable.
 */
class IllegalFinancialStateException(message: String) : Exception(message)
```

---

## Conclusion: The Lattice of Mental Models in Code

These principles form a **lattice of mental models** (Charlie Munger's concept) applied to software development:

1. **Ray Dalio's Extreme Transparency** → Explicit error handling and logging
2. **Benjamin Graham's Margin of Safety** → Defensive programming and validation
3. **Warren Buffett's Independent Thinking** → Question frameworks, avoid cargo cult
4. **Amundsen's Systematic Planning** → Thorough preparation and predictable execution
5. **Peter Drucker's Essence-Seeking** → Focus on investor value, eliminate noise

**The Unified Philosophy:**

> **"Build financial agents that think like world-class investors: independently, systematically, and with absolute integrity."**

Every line of code is an investment decision. Make it count.
