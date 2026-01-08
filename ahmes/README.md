# Ahmes - SEC Filing Parser Library

**Ahmes** is a Kotlin library for parsing SEC (Securities and Exchange Commission) filings. It extracts structured financial data from 10-K, 10-Q, 8-K, S-1, DEF 14A, 20-F and other SEC report types.

Named after the ancient Egyptian scribe Ahmes who authored the Rhind Mathematical Papyrus.

## Maven Coordinates

```kotlin
// Gradle Kotlin DSL
implementation("com.pascal.institute:ahmes:1.0.0")
```

```xml
<!-- Maven -->
<dependency>
    <groupId>com.pascal.institute</groupId>
    <artifactId>ahmes</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Features

-   **Multi-format Support**: Parse HTML, TXT, and PDF SEC filings
-   **XBRL Extraction**: Extract inline XBRL (iXBRL) data from SEC filings
-   **Specialized Form Parsers**: Dedicated parsers for 10-K, 10-Q, 8-K, S-1, DEF 14A, 20-F forms
-   **Financial Metrics**: Extract revenue, income, assets, liabilities, and other key metrics
-   **Financial Ratios**: Calculate profitability, liquidity, and solvency ratios
-   **Risk Factor Analysis**: Extract and categorize risk factors
-   **BigDecimal Precision**: All financial calculations use BigDecimal for accuracy

## Quick Start

### Parse HTML SEC Filing

```kotlin
import io.ahmes.format.ParserFactory
import io.ahmes.format.HtmlParser

// Automatic format detection
val result = ParserFactory.parseDocument(htmlContent, "apple-10k.htm")
println("Metrics found: ${result.metrics.size}")
result.metrics.forEach { metric ->
    println("${metric.name}: ${metric.value}")
}

// Or use specific parser
val htmlParser = HtmlParser()
val htmlResult = htmlParser.parse(htmlContent, "annual-report.htm")
```

### Parse by SEC Form Type

```kotlin
import io.ahmes.parser.SecReportParserFactory
import io.ahmes.model.SecReportType

// Get parser for 10-K form
val parser = SecReportParserFactory.getParser(SecReportType.FORM_10K)
val result = parser.parse(htmlContent, metadata)

// Access 10-K specific data
when (result) {
    is Form10KParseResult -> {
        println("Business: ${result.businessDescription}")
        println("Risk Factors: ${result.riskFactors.size}")
        result.financialStatements?.let { fs ->
            println("Revenue: ${fs.incomeStatement.revenue}")
        }
    }
}
```

### Extract Financial Metrics

```kotlin
import io.ahmes.parser.EnhancedFinancialParser

val metrics = EnhancedFinancialParser.parsePdfTextTable(textContent)
metrics.forEach { metric ->
    println("${metric.name}: ${metric.value} (${metric.category})")
    metric.getRawValueBigDecimal()?.let { bd ->
        println("  Raw value: $bd")
    }
}
```

### Extract Inline XBRL Data

```kotlin
import io.ahmes.parser.InlineXbrlExtractor
import org.jsoup.Jsoup

val document = Jsoup.parse(htmlContent)
val xbrlMetrics = InlineXbrlExtractor.extractMetrics(document)
xbrlMetrics.forEach { metric ->
    println("${metric.name}: ${metric.value} (confidence: ${metric.confidence})")
}
```

### Calculate Financial Ratios

```kotlin
import io.ahmes.parser.EnhancedFinancialParser

val ratios = EnhancedFinancialParser.calculateRatios(metrics)
ratios.forEach { ratio ->
    println("${ratio.name}: ${ratio.formattedValue}")
    println("  ${ratio.description}")
    println("  Status: ${ratio.healthStatus}")
}
```

## Package Structure

```
io.ahmes
├── model               # Data models
│   ├── FinancialModels.kt      # FinancialMetric, FinancialRatio, FinancialAnalysis
│   ├── ParserModels.kt         # ExtendedFinancialMetric, MetricCategory
│   └── SecReportModels.kt      # Form10KParseResult, SecReportType, etc.
├── parser              # Core parsers
│   ├── EnhancedFinancialParser.kt   # Main financial metric extraction
│   ├── InlineXbrlExtractor.kt       # iXBRL extraction
│   ├── SecTableParser.kt            # Table parsing
│   ├── SecReportParserFactory.kt    # Factory for form parsers
│   └── XbrlCompanyFactsExtractor.kt # XBRL company facts API
├── format              # Document format parsers
│   ├── HtmlParser.kt         # HTML/HTM parser
│   ├── TxtParser.kt          # Plain text parser
│   ├── PdfFormatParser.kt    # PDF parser
│   └── ParserFactory.kt      # Format parser factory
├── form                # SEC form-specific parsers
│   ├── Form10KParser.kt      # 10-K annual report
│   ├── Form10QParser.kt      # 10-Q quarterly report
│   ├── Form8KParser.kt       # 8-K current report
│   ├── FormS1Parser.kt       # S-1 IPO registration
│   ├── FormDEF14AParser.kt   # DEF 14A proxy statement
│   └── Form20FParser.kt      # 20-F foreign annual report
└── util                # Utilities
    ├── FinancialPrecision.kt # BigDecimal operations
    ├── FileUtils.kt          # File type detection
    ├── PdfParser.kt          # PDF binary parsing
    └── TikaExtractor.kt      # Apache Tika extraction
```

## Supported SEC Forms

| Form    | Description                      | Parser             |
| ------- | -------------------------------- | ------------------ |
| 10-K    | Annual Report                    | `Form10KParser`    |
| 10-Q    | Quarterly Report                 | `Form10QParser`    |
| 8-K     | Current Report (material events) | `Form8KParser`     |
| S-1     | IPO Registration                 | `FormS1Parser`     |
| DEF 14A | Proxy Statement                  | `FormDEF14AParser` |
| 20-F    | Foreign Company Annual Report    | `Form20FParser`    |

## Financial Metrics Extracted

### Income Statement

-   Revenue, Net Income, Gross Profit
-   Operating Expenses, R&D Expenses
-   EBITDA, Operating Income
-   EPS (Basic and Diluted)

### Balance Sheet

-   Total Assets, Total Liabilities
-   Cash and Equivalents
-   Accounts Receivable, Inventory
-   Stockholders' Equity

### Cash Flow Statement

-   Operating Cash Flow
-   Investing Cash Flow
-   Financing Cash Flow
-   Free Cash Flow

### Financial Ratios

-   Gross Margin, Operating Margin, Net Margin
-   Current Ratio, Quick Ratio
-   Debt-to-Equity, Interest Coverage
-   ROE, ROA

## Dependencies

-   Kotlin 2.1.0
-   kotlinx-serialization-json 1.7.3
-   Jsoup 1.18.3 (HTML parsing)
-   Saxon-HE 12.4 (XPath for XBRL)
-   Apache PDFBox 2.0.32 (PDF parsing)
-   Apache Tika 1.28.5 (content extraction)
-   JavaMoney Moneta 1.4.2 (financial precision)

## License

Apache License 2.0

## Related

This library is extracted from the [Papyrus](https://github.com/pascal-institute/papyrus) SEC filing analysis application.
