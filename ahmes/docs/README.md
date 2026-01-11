# 📚 Ahmes Library Documentation

## Overview

Comprehensive documentation for the Ahmes SEC document parsing library.

---

## 📖 Guides

### Getting Started
- [Basic Examples](../examples/BasicExamples.kt) - Common use cases and quick start
- [Advanced Examples](../examples/AdvancedExamples.kt) - AI, parallel processing, and optimization
- [API Documentation](#api-documentation) - Detailed KDoc reference

### Best Practices
- [Error Handling Guide](ERROR_HANDLING_GUIDE.md) - Exception types, recovery patterns, and error scenarios
- [Performance Guide](PERFORMANCE_GUIDE.md) - Optimization strategies, benchmarks, and production config
- [Testing Guide](../src/test/README.md) - Writing tests and coverage reports

---

## 🚀 Quick Start

### Basic Document Parsing

```kotlin
import com.pascal.institute.ahmes.form.Form10KParser
import com.pascal.institute.ahmes.model.SecReportMetadata

// 1. Create metadata
val metadata = SecReportMetadata(
    formType = "10-K",
    filingDate = "2023-11-03",
    reportDate = "2023-09-30",
    fiscalYearEnd = "0930",
    companyName = "Apple Inc.",
    ticker = "AAPL",
    cik = "0000320193",
    accessionNumber = "0000320193-23-000077",
    primaryDocument = "aapl-20230930.htm"
)

// 2. Parse document
val parser = Form10KParser()
val result = parser.parseHtml(htmlContent, metadata)

// 3. Access data
println("Business: ${result.businessDescription}")
println("Risk Factors: ${result.riskFactors.size}")
```

### Financial Metric Extraction

```kotlin
import com.pascal.institute.ahmes.parser.EnhancedFinancialParser

val content = File("10-K.html").readText()
val metrics = EnhancedFinancialParser.parseFinancialMetrics(content)

metrics.forEach { metric ->
    println("${metric.name}: ${metric.value}")
    println("  Category: ${metric.category}")
    println("  Period: ${metric.period}")
}
```

### AI-Powered Analysis

```kotlin
import com.pascal.institute.ahmes.ai.DjlModelManager

if (DjlModelManager.isAvailable()) {
    val predictor = DjlModelManager.getPredictor(
        DjlModelManager.ModelType.SENTIMENT
    )

    val result = predictor.predict(mdaText)
    println("Sentiment: ${result.classifications.first().className}")
}
```

---

## 📚 Core Concepts

### Form Parsers

Specialized parsers for different SEC form types:

| Form Type | Parser Class | Purpose |
|-----------|--------------|---------|
| **10-K** | `Form10KParser` | Annual financial reports |
| **10-Q** | `Form10QParser` | Quarterly financial reports |
| **8-K** | `Form8KParser` | Material event disclosures |
| **S-1** | `FormS1Parser` | IPO registration statements |
| **DEF 14A** | `FormDEF14AParser` | Proxy statements |
| **20-F** | `Form20FParser` | Foreign company annual reports |

### Data Models

Key data structures:

- **`SecReportMetadata`** - Filing metadata (CIK, ticker, dates)
- **`ExtendedFinancialMetric`** - Individual financial metrics with BigDecimal precision
- **`FinancialStatement`** - Structured financial statements (income, balance, cash flow)
- **`FinancialRatio`** - Calculated ratios with health status
- **`RiskFactor`** - Risk factors with categorization and severity

### Format Support

- **HTML** - Primary format (most common for EDGAR filings)
- **PDF** - Via Apache PDFBox
- **TXT** - Plain text documents
- **XBRL** - Inline XBRL tag extraction

---

## 📊 Features

### Financial Analysis
✅ Metric extraction with automatic unit detection
✅ BigDecimal precision for all monetary values
✅ Financial ratio calculation (20+ ratios)
✅ Trend analysis and period comparison
✅ Segment-level data extraction

### Document Processing
✅ Multi-format support (HTML, PDF, TXT, XBRL)
✅ Table structure recognition
✅ Section extraction (Items, Parts)
✅ Metadata parsing and validation
✅ Performance-optimized for large files

### AI & Machine Learning
✅ Sentiment analysis (FinBERT)
✅ Named Entity Recognition (NER)
✅ Question answering
✅ Document summarization
✅ GPU acceleration support (CUDA 12.4+)

### Quality & Reliability
✅ 125+ unit tests (100% pass rate)
✅ BigDecimal for financial precision (AGENTS.md Principle 4)
✅ Comprehensive error handling
✅ Traceable data sources
✅ Full documentation

---

## 🎯 Use Cases

### Investment Research
- Extract key financial metrics from 10-K/10-Q filings
- Calculate and track financial ratios over time
- Analyze MD&A sentiment trends
- Identify material risks and changes

### Risk Management
- Parse and categorize risk factors
- Track risk severity across filings
- Monitor risk factor changes quarter-over-quarter
- Automated risk assessment scoring

### M&A Due Diligence
- Comprehensive financial statement analysis
- Historical trend analysis
- Competitive positioning from business descriptions
- Legal proceeding and litigation tracking

### Regulatory Compliance
- Automated filing validation
- Data quality assurance
- Audit trail with source attribution
- Standardized data extraction

---

## 🔧 Configuration

### JVM Settings (Production)

```bash
java -Xms2g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar ahmes-app.jar
```

### AI Model Configuration

```kotlin
// Check GPU availability
val deviceInfo = DjlModelManager.getDeviceInfo()
println("GPU available: ${deviceInfo["isGpuAvailable"]}")

// Use specific device
System.setProperty("ai.djl.default_engine", "PyTorch")
```

### Performance Tuning

```kotlin
import kotlinx.coroutines.*

// Parallel processing
val cpuCores = Runtime.getRuntime().availableProcessors()
val dispatcher = Dispatchers.Default.limitedParallelism(cpuCores)

// Caching
val cache = Caffeine.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(Duration.ofHours(1))
    .build<String, ParseResult>()
```

---

## 📝 API Documentation

### Core Parsers

#### EnhancedFinancialParser
Comprehensive financial data extraction with BigDecimal precision.

**Key Methods:**
- `parseFinancialMetrics(content: String): List<ExtendedFinancialMetric>`
- `parseFinancialStatements(content: String): List<FinancialStatement>`
- `calculateRatios(metrics: List<ExtendedFinancialMetric>): List<FinancialRatio>`
- `parseRiskFactors(content: String): List<RiskFactor>`

**See:** [EnhancedFinancialParser.kt](../src/main/kotlin/com/pascal/institute/ahmes/parser/EnhancedFinancialParser.kt)

#### Form Parsers

**Form10KParser** - Annual Reports (10-K)
- `parseHtml(htmlContent: String, metadata: SecReportMetadata): Form10KParseResult`
- `parseText(textContent: String, metadata: SecReportMetadata): Form10KParseResult`

**Form10QParser** - Quarterly Reports (10-Q)
- Automatic quarter detection (Q1-Q4)
- Period-over-period comparison support

**Form8KParser** - Event Reports (8-K)
- Event item extraction
- Material event categorization

**FormS1Parser** - IPO Registrations
- Offering price extraction
- Shares offered calculation
- Underwriting information

### AI Models

#### DjlModelManager
Manages Deep Learning models for text analysis.

**Available Models:**
- `SENTIMENT` - Financial sentiment analysis (FinBERT)
- `NER` - Named entity recognition
- `QUESTION_ANSWERING` - High-precision Q&A
- `SUMMARIZATION` - Document summarization
- `TEXT_CLASSIFICATION` - Zero-shot classification

**Key Methods:**
- `getPredictor(modelType: ModelType): Predictor`
- `isAvailable(): Boolean`
- `getDeviceInfo(): Map<String, Any>`

---

## 🧪 Testing

### Running Tests

```bash
# All tests
./gradlew :ahmes:test

# Specific test class
./gradlew :ahmes:test --tests "Form10KParserTest"

# Pattern matching
./gradlew :ahmes:test --tests "*ParserTest"
```

### Test Coverage

Current: 125 tests, 100% pass rate, ~45% code coverage

See: [Test Coverage Report](../.agent/TEST_COVERAGE_FINAL_REPORT.md)

---

## 🚨 Troubleshooting

### Common Issues

**Issue:** `OutOfMemoryError` when parsing large files
**Solution:** Increase heap size or use streaming:
```bash
java -Xmx8g -jar app.jar
```

**Issue:** AI models not loading
**Solution:** Verify CUDA installation and classpath:
```kotlin
println(DjlModelManager.getDeviceInfo())
```

**Issue:** Incorrect financial values
**Solution:** Check unit detection and BigDecimal usage:
```kotlin
val value = metric.getRawValueBigDecimal() // Correct
// val value = metric.value.toDouble() // Wrong!
```

### Getting Help

- 📖 Check [Error Handling Guide](ERROR_HANDLING_GUIDE.md)
- 🔍 Search [existing tests](../src/test/) for examples
- 💬 Review [examples](../examples/) directory
- 🐛 Report issues with minimal reproduction case

---

## 📈 Performance Benchmarks

| Operation | Size | Time | Memory |
|-----------|------|------|--------|
| Parse 10-K (HTML) | 2MB | ~800ms | ~150MB |
| Extract Metrics | 2MB | ~200ms | ~50MB |
| Calculate Ratios | 50 metrics | ~10ms | ~5MB |
| AI Sentiment (GPU) | 1000 chars | ~50ms | ~200MB |
| AI Sentiment (CPU) | 1000 chars | ~500ms | ~200MB |

See: [Performance Guide](PERFORMANCE_GUIDE.md) for optimization strategies

---

## 🗺️ Roadmap

### Version 2.0 (Planned)

- [ ] Custom exception hierarchy
- [ ] Streaming API for very large files (>100MB)
- [ ] XBRL taxonomy support
- [ ] Real-time SEC filing integration
- [ ] Enhanced table parsing with ML
- [ ] Multi-language support

### Version 1.1 (In Progress)

- [x] Form parser test suite
- [x] Error handling documentation
- [x] Performance optimization guide
- [ ] FormDEF14AParser implementation
- [ ] Form20FParser implementation
- [ ] CI/CD integration

---

## 📚 Additional Resources

### External Links
- [SEC EDGAR System](https://www.sec.gov/edgar/searchedgar/companysearch.html)
- [XBRL US](https://xbrl.us/)
- [DJL (Deep Java Library)](https://djl.ai/)
- [Apache PDFBox](https://pdfbox.apache.org/)

### Related Projects
- [Papyrus Desktop App](../../README.md) - Desktop UI for SEC filing analysis
- [Pascal Institute](https://github.com/Pascal-Institute) - Research tools

---

*Last Updated: 2026-01-11*
*Version: 1.0.1*
*Maintained by: Pascal Institute*
