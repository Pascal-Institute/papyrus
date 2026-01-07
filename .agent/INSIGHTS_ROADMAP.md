# Insights Extraction Roadmap

## Mission
To derive **actionable investment insights** from SEC Filings (10-K, 10-Q).

## Key Components

### 1. Data Extraction (The Foundation)
- **Location**: `src/main/kotlin/papyrus/core/service/EnhancedFinancialParser.kt`
- **Responsibility**: Parse messy HTML/PDF into structured `FinancialMetric` objects.
- **Critical Success Factor**: Handling various formatting styles of SEC tables.
- **Validation**: `src/test/kotlin/papyrus/HtmlParserTest.kt`

### 2. Financial Analysis (The Logic)
- **Location**: `src/main/kotlin/papyrus/core/service/FinancialAnalyzer.kt`
- **Responsibility**: Convert raw metrics into **Ratios** and **Health Scores**.
- **Key Metrics to Track**:
    - **profitability**: Net Margin, Operating Margin, ROE.
    - **Solvency**: Current Ratio, Debt-to-Equity.
    - **Efficiency**: Asset Turnover.
    - **Growth**: YoY Revenue, YoY Net Income.

### 3. AI Insights (The Intelligence)
- **Location**: `src/main/kotlin/papyrus/core/service/AiAnalysisService.kt`
- **Responsibility**: Use LLMs to read MD&A (Management Discussion and Analysis) and Risk Factors.
- **Goal**: Identify "Red Flags" and "hidden gems" that numbers miss.

## Workflow for Adding New Insights
1. **Identify**: What metric do investors care about? (e.g., Free Cash Flow).
2. **Extract**: Update `EnhancedFinancialParser` to find the raw line items (Operating Cash Flow, CapEx).
3. **Compute**: Add logic to `FinancialAnalyzer` to calculate the derived metric.
4. **Display**: Update `QuickAnalyzeView` to show the new insight.
5. **Verify**: Add a unit test case with a sample excerpt.

## Precision Standard
All financial calculations must use `BigDecimal`. See `FINANCIAL_PRECISION.md` for rules.
