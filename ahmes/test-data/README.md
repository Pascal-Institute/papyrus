# Test Data & Benchmarks

## Overview

This directory contains test datasets and ground truth data for validating and benchmarking the Ahmes SEC parsing library.

---

## Directory Structure

```
test-data/
├── ground-truth/          # Ground truth JSON files
│   ├── apple-10k-2023.json
│   ├── microsoft-10q-2024.json
│   └── ...
├── documents/             # Sample SEC documents (HTML)
│   ├── apple-10k-2023.html
│   └── ...
├── results/               # Benchmark results
│   └── README.md
└── README.md             # This file
```

---

## Ground Truth Format

Each ground truth file contains:

### Document Info
```json
{
  "document": {
    "company": "Apple Inc.",
    "ticker": "AAPL",
    "cik": "0000320193",
    "formType": "10-K",
    "filingDate": "2023-11-03",
    "fiscalPeriod": "2023-09-30",
    "fileName": "aapl-20230930-10k.html",
    "fileSize": 2500000,
    "accessionNumber": "0000320193-23-000077"
  }
}
```

### Expected Metrics
Financial metrics that should be extracted:
```json
{
  "expectedMetrics": [
    {
      "name": "Total Revenue",
      "category": "REVENUE",
      "value": "$383.3B",
      "rawValue": "383285000000",
      "unit": "DOLLARS",
      "period": "FY 2023"
    }
  ]
}
```

### Expected Sections
Document sections that should be identified:
```json
{
  "expectedSections": {
    "Item 1": {
      "title": "Business",
      "type": "Item",
      "minLength": 1000,
      "mustContain": ["Apple", "products", "services"]
    }
  }
}
```

### Expected Risk Factors
Risk factors that should be extracted:
```json
{
  "expectedRiskFactors": [
    {
      "title": "Competition",
      "category": "OPERATIONAL",
      "severity": "HIGH",
      "summary": "We face intense competition...",
      "mustContainKeywords": ["competition", "competitive"]
    }
  ]
}
```

### Performance Benchmarks
Performance targets:
```json
{
  "benchmarks": {
    "maxParseTimeMs": 5000,
    "maxMemoryMB": 500,
    "minMetricAccuracy": 0.95,
    "minSectionAccuracy": 0.90,
    "minRiskFactorAccuracy": 0.85
  }
}
```

---

## Usage

### Running Benchmarks

```kotlin
import com.pascal.institute.ahmes.benchmark.BenchmarkRunner
import java.io.File

fun main() {
    val runner = BenchmarkRunner()

    // Run benchmark
    val result = runner.runBenchmark(
        documentFile = File("test-data/documents/apple-10k-2023.html"),
        groundTruthFile = File("test-data/ground-truth/apple-10k-2023.json")
    )

    // Check results
    println("Overall Accuracy: ${result.overallAccuracy}")
    println("Passed: ${result.passed}")
    println("Section Accuracy: ${result.sectionResults.accuracy}")
    println("Metric Accuracy: ${result.metricResults.accuracy}")
    println("Parse Time: ${result.performanceResults.parseTimeMs}ms")

    // Save results
    runner.saveResult(result, File("test-data/results/apple-10k-2023-result.json"))
}
```

### Creating Ground Truth

1. **Extract Actual Data**: Parse a document manually or with the parser
2. **Verify Accuracy**: Manually verify all extracted data
3. **Create JSON**: Use the ground truth format
4. **Set Benchmarks**: Define acceptable performance targets

Example template:
```json
{
  "document": {},
  "expectedMetrics": [],
  "expectedRiskFactors": [],
  "expectedSections": {},
  "benchmarks": {}
}
```

---

## Validation Metrics

### Accuracy Calculation

**Overall Accuracy**:
```
Overall = (SectionAccuracy × 0.5) +
          (MetricAccuracy × 0.3) +
          (RiskFactorAccuracy × 0.2)
```

**Section Accuracy**:
```
SectionAccuracy = CorrectSections / ExpectedSections
```

**Metric Accuracy**:
```
MetricAccuracy = CorrectMetrics / ExpectedMetrics
```

**Risk Factor Accuracy**:
```
RiskFactorAccuracy = MatchedRiskFactors / ExpectedRiskFactors
```

### Performance Score

```
PerformanceScore = (TimeScore + MemoryScore) / 2

where:
  TimeScore = min(1.0, MaxTime / ActualTime)
  MemoryScore = min(1.0, MaxMemory / ActualMemory)
```

---

## Sample Datasets

### Currently Available

| Document | Form | Company | Fiscal Period | Size | Status |
|----------|------|---------|---------------|------|--------|
| apple-10k-2023 | 10-K | Apple Inc. | 2023-09-30 | ~2.5MB | ✅ Ground truth |

### Planned Datasets

- [ ] Microsoft 10-Q (Q2 2024)
- [ ] Tesla 8-K (Earnings)
- [ ] Alphabet S-1 (IPO)
- [ ] Meta DEF 14A (Proxy)
- [ ] Toyota 20-F (Foreign)

---

## Adding New Datasets

### Step 1: Obtain Document

Download from SEC EDGAR:
```bash
# Example: Apple 10-K
https://www.sec.gov/cgi-bin/viewer?action=view&cik=320193&accession_number=0000320193-23-000077&xbrl_type=v
```

### Step 2: Create Ground Truth

1. Parse the document
2. Manually verify:
   - All key metrics
   - Section boundaries
   - Risk factors
3. Create JSON file

### Step 3: Set Benchmarks

Run initial benchmark:
```kotlin
val result = runner.runBenchmark(document, groundTruth)
println("Parse Time: ${result.performanceResults.parseTimeMs}ms")
println("Memory: ${result.performanceResults.memoryUsedMB}MB")
```

Set targets based on results (e.g., 150% of current performance).

### Step 4: Document

Update this README with:
- Dataset information
- Any special considerations
- Known issues

---

## Best Practices

### Ground Truth Quality

✅ **Do:**
- Manually verify all expected values
- Include representative samples from each section
- Set realistic performance benchmarks
- Document edge cases

❌ **Don't:**
- Copy parser output directly
- Set overly strict benchmarks
- Include PII or sensitive data
- Use copyrighted content beyond fair use

### Benchmark Design

- **Accuracy > Speed**: Prioritize correctness over performance
- **Real Documents**: Use actual SEC filings, not synthetic data
- **Diverse Samples**: Cover different companies, industries, periods
- **Edge Cases**: Include challenging documents

---

## Performance Targets

| Form Type | File Size | Parse Time | Memory | Section Accuracy | Metric Accuracy |
|-----------|-----------|------------|--------|------------------|-----------------|
| 10-K | < 5MB | < 5s | < 500MB | > 90% | > 95% |
| 10-Q | < 2MB | < 2s | < 200MB | > 90% | > 95% |
| 8-K | < 500KB | < 1s | < 100MB | > 85% | > 90% |
| S-1 | < 3MB | < 3s | < 300MB | > 85% | > 90% |

---

## Continuous Integration

### Automated Benchmarking

Run benchmarks on every commit:
```bash
./gradlew :ahmes:benchmark
```

### Regression Detection

Alert if:
- Accuracy drops > 2%
- Parse time increases > 20%
- Memory usage increases > 20%

---

## Contributing

To contribute new datasets:

1. Fork the repository
2. Add ground truth JSON to `test-data/ground-truth/`
3. Add document to `test-data/documents/` (if permissible)
4. Run benchmarks and verify results
5. Submit pull request with:
   - Ground truth file
   - Benchmark results
   - Updated README

---

## License

Ground truth data is derived from public SEC filings and is used for testing purposes under fair use.

---

*Last Updated: 2026-01-11*
