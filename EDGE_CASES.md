# EDGE_CASES.md

**Pre-Mortem Analysis for Papyrus SEC Filing Parser**

> "Victory awaits those who have everything in order. People call this luck." — Roald Amundsen
> (AGENTS.md Principle #11: Plan Like Amundsen)

This document catalogs potential catastrophic failures and edge cases for the Papyrus/Ahmes SEC filing parser. Following the Pre-mortem methodology, we ask: **"How could this system fail spectacularly?"** and then build safeguards.

---

## 📊 Risk Matrix

| Risk ID | Category | Likelihood | Impact | Priority |
|---------|----------|------------|--------|----------|
| EC-001 | SEC Format Change | **HIGH** | **CRITICAL** | P0 |
| EC-002 | XBRL Schema Update | **MEDIUM** | **HIGH** | P1 |
| EC-003 | Company-Specific Formats | **HIGH** | **MEDIUM** | P1 |
| EC-004 | Corrupt/Incomplete Files | **MEDIUM** | **MEDIUM** | P2 |
| EC-005 | Extreme Data Values | **LOW** | **HIGH** | P2 |
| EC-006 | Encoding Issues | **MEDIUM** | **LOW** | P3 |
| EC-007 | API Rate Limiting | **HIGH** | **MEDIUM** | P1 |
| EC-008 | Memory Exhaustion | **LOW** | **CRITICAL** | P1 |
| EC-009 | Concurrent Access | **MEDIUM** | **MEDIUM** | P2 |
| EC-010 | Version Incompatibility | **LOW** | **MEDIUM** | P3 |

---

## EC-001: SEC Format Change 🔴 CRITICAL

### Problem Description
The SEC periodically updates HTML/XBRL formats, section structures, or introduces new filing types. A format change could break our parsing logic overnight.

**Example Scenarios:**
- SEC changes "ITEM 1A. RISK FACTORS" to "Section 1A: Risk Disclosures"
- XBRL namespace changes from `us-gaap:` to `us-gaap-v2:`
- New mandatory sections added to 10-K (e.g., "Item 1C: Cybersecurity")
- HTML structure changes from `<div class="text">` to `<section data-type="text">`

### Likelihood: **HIGH** (SEC updates formats 1-2 times per year)

### Impact: **CRITICAL**
- ❌ **Complete parsing failure** for new filings
- ❌ **Silent failures** (returns empty metrics instead of erroring)
- ❌ **Investor decisions based on incomplete data** (DANGEROUS!)

### Current Safeguards
✅ **Logging**: Comprehensive logging tracks parsing success/failure
✅ **Confidence Scores**: Each metric has a confidence score (0.0-1.0)
✅ **Fallback Parsing**: Multi-strategy approach (table → pattern → XBRL)
⚠️ **Validation**: Basic sanity checks for extreme values

### Gaps
❌ **No automated monitoring** of SEC format changes
❌ **No regression detection** when parsing results change
❌ **No alerts** when confidence scores drop significantly

### Mitigation Strategy

#### Immediate Actions (P0)
1. **Create Format Monitoring Test Suite**
   ```kotlin
   @Test
   fun `SEC format regression detector`() {
       val knownGoodFiling = loadTestResource("apple-10k-2023.html")
       val baseline = parseAndSnapshot(knownGoodFiling)

       val currentResult = EnhancedFinancialParser.parseFinancialMetrics(knownGoodFiling)

       // Alert if critical metrics missing
       assertContainsMetric(currentResult, MetricCategory.REVENUE, baseline.revenue)
       assertContainsMetric(currentResult, MetricCategory.NET_INCOME, baseline.netIncome)

       // Alert if confidence drops > 20%
       val avgConfidence = currentResult.map { it.confidence }.average()
       assertTrue(avgConfidence >= baseline.avgConfidence - 0.2,
           "Confidence dropped from ${baseline.avgConfidence} to $avgConfidence")
   }
   ```

2. **Implement Weekly SEC Format Check**
   - Download 3 recent 10-Ks from different industries
   - Run full parsing pipeline
   - Compare against expected baseline
   - Send alert if parsing success rate < 80%

3. **Add Format Version Detection**
   ```kotlin
   data class SecFormatVersion(
       val htmlVersion: String,      // e.g., "HTML 5.0"
       val xbrlVersion: String,       // e.g., "XBRL 2.1"
       val taxonomyVersion: String,   // e.g., "us-gaap/2023"
       val detectedAt: Instant
   )

   fun detectFormatVersion(content: String): SecFormatVersion {
       // Parse DOCTYPE, XBRL namespace, schema references
       // Log warning if unknown version detected
   }
   ```

#### Long-term Solutions (P1)
4. **Machine Learning Fallback**
   - Train NER model to extract financial entities regardless of format
   - Use as fallback when structured parsing fails
   - Already partially implemented via DJL in `ahmes.ai` package

5. **Community Format Database**
   - Maintain crowdsourced database of format changes
   - Subscribe to SEC EDGAR system updates
   - Auto-update parsing rules based on community reports

#### Testing Strategy
```kotlin
// src/test/resources/edge_cases/sec_format_changes/
// - apple-10k-2023-baseline.html (known good)
// - apple-10k-2024-new-format.html (simulated format change)
// - tesla-10q-xbrl-v3.html (new XBRL version)

@Test
fun `should handle ITEM 1A format change`() {
    val oldFormat = """ITEM 1A. RISK FACTORS"""
    val newFormat = """Section 1A: Risk Disclosures"""

    val risks1 = parseRiskFactors(oldFormat)
    val risks2 = parseRiskFactors(newFormat)

    assertTrue(risks1.isNotEmpty() || risks2.isNotEmpty(),
        "Parser should handle both old and new formats")
}
```

---

## EC-002: XBRL Schema Update 🟡 HIGH

### Problem Description
XBRL taxonomies (us-gaap, dei, etc.) are updated annually. New tags are added, old tags deprecated, and namespaces can change.

**Example Scenarios:**
- `us-gaap:RevenueFromContractWithCustomerExcludingAssessedTax` shortened to `us-gaap:Revenue`
- New required tag: `us-gaap:CarbonEmissions` (ESG reporting)
- Deprecated: `us-gaap:GoodwillAndIntangibleAssetsGross`

### Likelihood: **MEDIUM** (Annual updates, but gradual adoption)

### Impact: **HIGH**
- ⚠️ Missing metrics from new XBRL tags
- ⚠️ Over-reliance on deprecated tags

### Current Safeguards
✅ **Multi-strategy parsing**: Table + Pattern + XBRL (not dependent on XBRL alone)
✅ **Saxon XPath engine**: Robust XBRL parsing

### Mitigation Strategy

1. **XBRL Tag Mapping Table**
   ```kotlin
   object XbrlTagEvolution {
       val REVENUE_TAGS = listOf(
           "us-gaap:RevenueFromContractWithCustomerExcludingAssessedTax", // 2018+
           "us-gaap:Revenues",                                              // 2014-2017
           "us-gaap:SalesRevenueNet"                                        // Legacy
       )

       fun findRevenue(xbrlDoc: Document): BigDecimal? {
           return REVENUE_TAGS.firstNotNullOfOrNull { tag ->
               extractXbrlValue(xbrlDoc, tag)
           }
       }
   }
   ```

2. **XBRL Schema Validator**
   - Parse schema version from filing
   - Warn if unknown schema version detected
   - Suggest updating parser

3. **Fallback to Label Matching**
   ```kotlin
   // If XBRL tag not found, search by human-readable label
   fun findByLabel(xbrlDoc: Document, labels: List<String>): BigDecimal? {
       // Search presentation linkbase for matching labels
   }
   ```

---

## EC-003: Company-Specific Formats 🟡 HIGH

### Problem Description
Some companies use non-standard formats, especially:
- Foreign filers (20-F) with different structures
- Small-cap companies with simplified filings
- Newly public companies (S-1) with unique sections

**Real Examples:**
- **Alibaba (BABA)**: Chinese fiscal calendar, RMB → USD conversion
- **Tesla**: Non-GAAP metrics prominently featured
- **Berkshire Hathaway**: Extremely long (300+ page) filings

### Likelihood: **HIGH** (100+ unique formats among S&P 500)

### Impact: **MEDIUM**
- ⚠️ Lower extraction accuracy for specific companies
- ⚠️ Need manual verification for critical stocks

### Current Safeguards
✅ **Fallback parsing strategies**
✅ **Confidence scoring**

### Mitigation Strategy

1. **Company-Specific Parser Registry**
   ```kotlin
   object CompanyParserRegistry {
       private val customParsers = mapOf(
           "0001318605" to AlibabaParser(),  // CIK for Alibaba
           "1318605" to TeslaParser()
       )

       fun getParser(cik: String): SecReportParser? {
           return customParsers[cik]
       }
   }
   ```

2. **Foreign Filer Handling**
   ```kotlin
   // Detect non-USD currencies and conversion rates
   fun detectCurrency(filing: String): Currency {
       val currencyPattern = Regex("""(?:CNY|EUR|GBP|JPY|RMB)""")
       // ...
   }
   ```

3. **Create Test Suite of Edge Case Companies**
   - Alibaba (foreign, different calendar)
   - Berkshire Hathaway (extremely long)
   - Tesla (non-GAAP heavy)
   - SPACs (unique structure)

---

## EC-004: Corrupt or Incomplete Files 🟡 MEDIUM

### Problem Description
Downloads can fail, files can be truncated, or SEC might serve partial content.

**Scenarios:**
- Network timeout during download → truncated HTML
- Corrupted ZIP file from SEC EDGAR
- Malformed HTML (unclosed tags)
- PDF with missing pages

### Likelihood: **MEDIUM** (1-2% of downloads)

### Impact: **MEDIUM**
- ⚠️ Parsing crashes or hangs
- ⚠️ Partial data extraction

### Current Safeguards
✅ **Exception handling** in parsing logic
⚠️ **Basic HTML validation** via Jsoup

### Mitigation Strategy

1. **File Integrity Checks**
   ```kotlin
   fun validateSecFiling(content: String): ValidationResult {
       return ValidationResult(
           isComplete = content.contains("</html>") || content.contains("END OF DOCUMENT"),
           hasMinimumLength = content.length > 10_000, // 10KB minimum
           hasExpectedSections = content.contains("ITEM 1") || content.contains("Item 1"),
           estimatedCompleteness = calculateCompleteness(content)
       )
   }

   fun parseWithValidation(content: String): FinancialAnalysis {
       val validation = validateSecFiling(content)

       if (validation.estimatedCompleteness < 0.8) {
           logger.warn { "Filing appears incomplete (${validation.estimatedCompleteness * 100}%)" }
           // Retry download or flag for manual review
       }

       return parse(content)
   }
   ```

2. **Retry Logic with Exponential Backoff**
   ```kotlin
   suspend fun downloadFilingWithRetry(url: String, maxRetries: Int = 3): String {
       var attempt = 0
       var lastException: Exception? = null

       while (attempt < maxRetries) {
           try {
               val content = httpClient.get(url).bodyAsText()

               if (validateSecFiling(content).isComplete) {
                   return content
               }

               logger.warn { "Downloaded content incomplete, retry ${attempt + 1}/$maxRetries" }
           } catch (e: Exception) {
               lastException = e
               logger.warn(e) { "Download failed, retry ${attempt + 1}/$maxRetries" }
           }

           delay(2.0.pow(attempt).seconds) // Exponential backoff
           attempt++
       }

       throw DownloadException("Failed after $maxRetries retries", lastException)
   }
   ```

3. **Graceful Degradation**
   - If only partial content available, extract what we can
   - Flag metrics with lower confidence scores
   - Provide "Data Completeness" indicator to user

---

## EC-005: Extreme Data Values 🔴 HIGH IMPACT

### Problem Description
Financial data can have extreme values that break assumptions or cause overflow.

**Scenarios:**
- **Revenue = $0**: Pre-revenue company or restructuring
- **Negative equity**: Accumulated losses exceed assets
- **P/E ratio = Infinity**: Division by zero (zero earnings)
- **Debt-to-Equity = 10,000%**: Highly leveraged company
- **Revenue growth = -99%**: Near bankruptcy

### Likelihood: **LOW** (but inevitable in edge cases)

### Impact: **HIGH**
- ❌ Calculation errors (division by zero)
- ❌ Misleading ratios shown to investors
- ❌ System crashes on unexpected values

### Current Safeguards
✅ **FinancialPrecision.validateFinancialAmount()**: Checks plausibility
✅ **Division by zero guards** in ratio calculations
✅ **BigDecimal** prevents overflow

### Gaps
❌ **No handling for negative equity**
❌ **No special case for pre-revenue companies**

### Mitigation Strategy

1. **Enhanced Validation**
   ```kotlin
   data class FinancialContext(
       val companyStage: CompanyStage, // PRE_REVENUE, GROWTH, MATURE, DISTRESSED
       val industry: Industry,
       val fiscalYear: Int
   )

   fun validateMetric(
       metric: ExtendedFinancialMetric,
       context: FinancialContext
   ): ValidationResult {
       return when (context.companyStage) {
           CompanyStage.PRE_REVENUE -> {
               // Revenue = 0 is expected
               if (metric.category == MetricCategory.REVENUE && metric.rawValue == "0") {
                   ValidationResult.VALID
               } else ValidationResult.REVIEW_REQUIRED
           }
           CompanyStage.DISTRESSED -> {
               // Negative equity is possible
               if (metric.category == MetricCategory.TOTAL_EQUITY &&
                   metric.getRawValueBigDecimal()?.signum() == -1) {
                   ValidationResult.VALID_BUT_ALARMING
               } else ValidationResult.VALID
           }
           else -> standardValidation(metric)
       }
   }
   ```

2. **Ratio Calculation with Context**
   ```kotlin
   fun calculatePERatio(
       netIncome: BigDecimal,
       sharesOutstanding: BigDecimal,
       context: FinancialContext
   ): FinancialRatio? {
       if (netIncome <= BigDecimal.ZERO) {
           logger.info { "P/E ratio not applicable: negative earnings" }
           return FinancialRatio(
               name = "P/E Ratio",
               value = "N/A",
               formattedValue = "N/A (Negative Earnings)",
               healthStatus = HealthStatus.WARNING,
               interpretation = "Company is currently unprofitable"
           )
       }

       // Normal calculation
       // ...
   }
   ```

3. **Test Suite for Edge Cases**
   ```kotlin
   @Test
   fun `should handle zero revenue gracefully`() {
       val metrics = listOf(
           ExtendedFinancialMetric(name = "Revenue", rawValue = "0", category = MetricCategory.REVENUE)
       )

       val ratios = EnhancedFinancialParser.calculateRatios(metrics)

       // Should not crash, should explain why ratios unavailable
       assertTrue(ratios.isEmpty() || ratios.all { it.formattedValue.contains("N/A") })
   }

   @Test
   fun `should handle negative equity`() {
       val metrics = listOf(
           ExtendedFinancialMetric(name = "Total Equity", rawValue = "-5000000000", category = MetricCategory.TOTAL_EQUITY)
       )

       // Should not throw exception
       assertDoesNotThrow { EnhancedFinancialParser.calculateRatios(metrics) }
   }
   ```

---

## EC-006: Encoding Issues 🟢 LOW IMPACT

### Problem Description
SEC filings can contain various encodings, special characters, or malformed UTF-8.

**Scenarios:**
- Latin-1 encoded file served as UTF-8
- Special characters: ©, ™, ®, —, •
- Chinese characters in foreign filer 20-Fs
- Emoji in modern filings (rare but possible)

### Likelihood: **MEDIUM**

### Impact: **LOW**
- ⚠️ Garbled text in extracted content
- ⚠️ Parsing failures on special character patterns

### Current Safeguards
✅ **Jsoup handles most HTML entities**
⚠️ **Ktor default encoding handling**

### Mitigation Strategy

1. **Encoding Detection**
   ```kotlin
   fun detectEncoding(bytes: ByteArray): Charset {
       // Check BOM (Byte Order Mark)
       if (bytes.startsWith(byteArrayOf(0xEF, 0xBB, 0xBF))) return Charsets.UTF_8
       if (bytes.startsWith(byteArrayOf(0xFF, 0xFE))) return Charsets.UTF_16LE

       // Use charset detector library
       return CharsetDetector().setText(bytes).detect().name
   }
   ```

2. **Normalize Special Characters**
   ```kotlin
   fun normalizeText(text: String): String {
       return text
           .replace("—", "-")  // Em dash → hyphen
           .replace("–", "-")  // En dash → hyphen
           .replace("•", "*")  // Bullet → asterisk
           .replace(Regex("\\s+"), " ") // Multiple spaces → single
           .trim()
   }
   ```

---

## EC-007: API Rate Limiting 🟡 HIGH LIKELIHOOD

### Problem Description
SEC EDGAR enforces rate limits: **10 requests/second** per IP address. Exceeding this returns 403 Forbidden.

### Likelihood: **HIGH** (Easy to trigger during batch processing)

### Impact: **MEDIUM**
- ⚠️ Download failures
- ⚠️ Need to retry with backoff

### Current Safeguards
✅ **User-Agent header** set correctly
❌ **No rate limiting on our side**

### Mitigation Strategy

```kotlin
class SecApiRateLimiter {
    private val requestTimes = ConcurrentLinkedQueue<Instant>()
    private val maxRequestsPerSecond = 10

    suspend fun <T> rateLimit(block: suspend () -> T): T {
        // Remove requests older than 1 second
        val oneSecondAgo = Instant.now().minusSeconds(1)
        while (requestTimes.peek()?.isBefore(oneSecondAgo) == true) {
            requestTimes.poll()
        }

        // If at limit, wait
        if (requestTimes.size >= maxRequestsPerSecond) {
            val oldestRequest = requestTimes.peek()!!
            val waitTime = Duration.between(Instant.now(), oldestRequest.plusSeconds(1))
            if (waitTime.isPositive) {
                logger.debug { "Rate limit reached, waiting ${waitTime.toMillis()}ms" }
                delay(waitTime.toMillis())
            }
        }

        requestTimes.offer(Instant.now())
        return block()
    }
}
```

---

## EC-008: Memory Exhaustion 🔴 CRITICAL IMPACT

### Problem Description
Large filings (300+ pages) can consume excessive memory, especially when:
- Loading entire HTML into DOM
- Parsing large XBRL files
- Batch processing 100+ filings

**Example:**
- Berkshire Hathaway 10-K: ~30 MB HTML
- Loading 100 filings simultaneously: 3 GB memory

### Likelihood: **LOW** (only with very large files or batch ops)

### Impact: **CRITICAL**
- ❌ OutOfMemoryError
- ❌ Application crash
- ❌ JVM heap exhaustion

### Mitigation Strategy

1. **Streaming Parser for Large Files**
   ```kotlin
   fun parseVeryLargeFiling(file: File): FinancialAnalysis {
       if (file.length() > 10_000_000) { // 10 MB threshold
           logger.warn { "Large file detected (${file.length()} bytes), using streaming parser" }
           return streamingParse(file)
       }
       return standardParse(file.readText())
   }

   fun streamingParse(file: File): FinancialAnalysis {
       // Use BufferedReader instead of loading entire file
       val metrics = mutableListOf<ExtendedFinancialMetric>()

       file.useLines { lines ->
           var buffer = StringBuilder()
           for (line in lines) {
               buffer.append(line)

               // Process buffer in chunks
               if (buffer.length > 100_000) {
                   metrics.addAll(parseChunk(buffer.toString()))
                   buffer.clear()
               }
           }
       }

       return buildAnalysis(metrics)
   }
   ```

2. **Memory Monitoring**
   ```kotlin
   fun checkMemoryBefore Parsing() {
       val runtime = Runtime.getRuntime()
       val usedMemory = runtime.totalMemory() - runtime.freeMemory()
       val maxMemory = runtime.maxMemory()
       val usagePercent = (usedMemory.toDouble() / maxMemory) * 100

       if (usagePercent > 80) {
           logger.warn { "High memory usage: ${usagePercent}%, running GC" }
           System.gc()

           if (usagePercent > 90) {
               throw InsufficientMemoryException("Memory usage critical: ${usagePercent}%")
           }
       }
   }
   ```

---

## EC-009: Concurrent Access Issues 🟡 MEDIUM

### Problem Description
If multiple threads parse simultaneously and share state, race conditions can occur.

### Current Status
✅ **EnhancedFinancialParser is an `object` (singleton) with no mutable state** → Thread-safe
⚠️ **Caching mechanisms** might have concurrency issues

### Mitigation Strategy

```kotlin
// Use thread-safe collections for any caching
object ParserCache {
    private val cache = ConcurrentHashMap<String, FinancialAnalysis>()

    fun getOrParse(key: String, parser: () -> FinancialAnalysis): FinancialAnalysis {
        return cache.getOrPut(key, parser)
    }
}
```

---

## EC-010: Version Incompatibility 🟢 LOW IMPACT

### Problem Description
Ahmes library updates could break client code (Papyrus app).

### Mitigation Strategy

1. **Semantic Versioning**
   - MAJOR: Breaking changes
   - MINOR: Backward-compatible features
   - PATCH: Bug fixes

2. **Compatibility Tests**
   ```kotlin
   // Run against older Ahmes versions
   @Test
   fun `ensure backward compatibility`() {
       val oldResult = parseWithAhmes_v1_0_0(content)
       val newResult = parseWithAhmes_v1_1_0(content)

       assertEquals(oldResult.metrics.size, newResult.metrics.size)
   }
   ```

---

## 🛡️ Defense-in-Depth Summary

### Layer 1: Prevention
- Comprehensive logging (EC-001 detection)
- Input validation (EC-004, EC-005)
- Rate limiting (EC-007)
- Memory checks (EC-008)

### Layer 2: Detection
- Regression tests (EC-001, EC-002)
- Confidence scoring (all)
- Monitoring and alerts (EC-001)

### Layer 3: Recovery
- Fallback parsing strategies (EC-001, EC-002, EC-003)
- Graceful degradation (EC-004, EC-005)
- Retry logic (EC-004, EC-007)

### Layer 4: Learning
- Log all edge cases encountered
- Update test suite with real-world failures
- Quarterly review of edge case frequency

---

## 📅 Monitoring Checklist

### Weekly
- [ ] Run regression tests against latest SEC filings
- [ ] Check parsing success rate (target: >95%)
- [ ] Review logs for new warning patterns

### Monthly
- [ ] Update XBRL tag mapping table
- [ ] Review edge case test coverage
- [ ] Analyze which edge cases are most common

### Quarterly
- [ ] Full Pre-mortem review (update this document)
- [ ] Load test with batch processing
- [ ] Security audit of download/parsing pipeline

---

## 🔗 Related Documents

- **AGENTS.md**: Core development principles (#11: Plan Like Amundsen)
- **README.md**: Normal operation documentation
- **LESSONS_LEARNED.md**: Post-mortems of actual failures (to be created)

---

## 📝 Conclusion

**Key Takeaway:** SEC filing parsing is not a "set it and forget it" system. Format changes WILL happen. This document ensures we're prepared.

> "By failing to prepare, you are preparing to fail." — Benjamin Franklin

Following AGENTS.md Principle #11, we've planned for failure scenarios and built safeguards. The next step is to **implement the P0 and P1 mitigations** and **monitor for these edge cases in production**.

---

**Last Updated**: 2026-01-14
**Next Review**: 2026-04-14
**Maintained By**: Pascal Institute Team
