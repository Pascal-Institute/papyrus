package com.pascal.institute.ahmes.examples

import com.pascal.institute.ahmes.form.*
import com.pascal.institute.ahmes.model.*
import java.io.File

/**
 * Basic Examples - Getting Started with Ahmes
 *
 * This file demonstrates the most common use cases for parsing SEC documents.
 */
fun main() {
    println("=== Ahmes Library - Basic Examples ===\n")

    // Example 1: Parse a 10-K Annual Report
    example1_Parse10K()

    // Example 2: Parse a 10-Q Quarterly Report
    example2_Parse10Q()

    // Example 3: Extract Financial Metrics
    example3_ExtractMetrics()

    // Example 4: Parse Risk Factors
    example4_ParseRiskFactors()
}

/**
 * Example 1: Parse a 10-K Annual Report
 *
 * Demonstrates how to parse a 10-K filing and extract key sections.
 */
fun example1_Parse10K() {
    println("--- Example 1: Parse 10-K Annual Report ---")

    // 1. Create metadata for the document
    val metadata =
            SecReportMetadata(
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

    // 2. Load the HTML content (or use File)
    val htmlContent =
            """
        <html>
        <body>
            <h2>ITEM 1. BUSINESS</h2>
            <p>Apple Inc. designs, manufactures, and markets smartphones,
            personal computers, tablets, wearables, and accessories worldwide.</p>

            <h2>ITEM 1A. RISK FACTORS</h2>
            <p>We face intense competition in all aspects of our business.</p>

            <h2>ITEM 7. MANAGEMENT'S DISCUSSION AND ANALYSIS</h2>
            <p>Revenue for fiscal 2023 increased 10% to $383 billion.</p>
        </body>
        </html>
    """.trimIndent()

    // 3. Parse the document
    val parser = Form10KParser()
    val result = parser.parseHtml(htmlContent, metadata)

    // 4. Access extracted data
    println("Company: ${result.metadata.companyName}")
    println("Fiscal Year: ${result.metadata.reportDate}")
    println("\nBusiness Description:")
    println(result.businessDescription?.take(200))

    println("\nRisk Factors: ${result.riskFactors.size}")
    result.riskFactors.take(2).forEach { risk -> println("  - ${risk.title}: ${risk.category}") }

    println("\nSections extracted: ${result.sections.keys.joinToString()}")
    println()
}

/**
 * Example 2: Parse a 10-Q Quarterly Report
 *
 * Shows how to extract quarterly information and period detection.
 */
fun example2_Parse10Q() {
    println("--- Example 2: Parse 10-Q Quarterly Report ---")

    val metadata =
            SecReportMetadata(
                    formType = "10-Q",
                    filingDate = "2024-05-03",
                    reportDate = "2024-03-31",
                    fiscalYearEnd = "0930",
                    companyName = "Apple Inc.",
                    ticker = "AAPL",
                    cik = "0000320193",
                    accessionNumber = "0000320193-24-000045",
                    primaryDocument = "aapl-20240331.htm"
            )

    val htmlContent =
            """
        <html>
        <body>
            <h1>FORM 10-Q</h1>
            <p>For the quarterly period ended March 31, 2024</p>

            <h2>PART I - FINANCIAL INFORMATION</h2>
            <h3>Item 1. Financial Statements</h3>
            <p>Condensed consolidated balance sheets (unaudited)</p>

            <h3>Item 2. Management's Discussion and Analysis</h3>
            <p>Net sales for Q2 2024 increased 8% to $90.8 billion.</p>
        </body>
        </html>
    """.trimIndent()

    val parser = Form10QParser()
    val result = parser.parseHtml(htmlContent, metadata)

    println("Company: ${result.metadata.companyName}")
    println("Quarter: ${result.quarter}") // Automatically extracted: "Q1"
    println("Fiscal Year: ${result.fiscalYear}") // "2024"
    println("Sections: ${result.sections.size}")

    if (result.mdAndA != null) {
        println("\nMD&A Key Points:")
        result.mdAndA!!.keyBusinessDrivers.take(2).forEach { driver ->
            println("  - ${driver.take(80)}...")
        }
    }

    println()
}

/**
 * Example 3: Extract Financial Metrics
 *
 * Demonstrates financial data extraction with BigDecimal precision.
 */
fun example3_ExtractMetrics() {
    println("--- Example 3: Extract Financial Metrics ---")

    val documentContent =
            """
        CONSOLIDATED STATEMENTS OF OPERATIONS
        (In millions, except per share amounts)

        Year Ended September 30, 2023:

        Net sales:
          Products                    $298,085
          Services                     $85,200
        Total net sales               $383,285

        Cost of sales:
          Products                    $189,282
          Services                     $24,855
        Total cost of sales           $214,137

        Gross margin                  $169,148

        Operating expenses:
          Research and development     $29,915
          Selling, general and admin   $24,932
        Total operating expenses       $54,847

        Operating income              $114,301
        Net income                    $96,995

        Earnings per share:
          Basic                        $6.16
          Diluted                      $6.13
    """.trimIndent()

    val metadata =
            SecReportMetadata(
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

    val parser = Form10KParser()
    val result = parser.parseText(documentContent, metadata)

    println("Company: ${result.metadata.companyName}")

    if (result.financialStatements != null) {
        val financials = result.financialStatements!!

        println("\nFinancial Data:")
        println("Report Type: ${financials.reportType}")
        println("Fiscal Year: ${financials.fiscalYear}")
        println("Data Quality: ${financials.dataQuality}")

        financials.incomeStatement?.let { income ->
            println("\nIncome Statement:")
            income.totalRevenue?.let { revenue -> println("  Total Revenue: ${revenue.formatted}") }
            income.grossProfit?.let { profit -> println("  Gross Profit: ${profit.formatted}") }
            income.operatingIncome?.let { opIncome ->
                println("  Operating Income: ${opIncome.formatted}")
            }
            income.netIncome?.let { netIncome -> println("  Net Income: ${netIncome.formatted}") }
            income.dilutedEPS?.let { eps ->
                println("  Diluted EPS: $${String.format("%.2f", eps)}")
            }
        }
    } else {
        println("(Financial statements would be extracted from real 10-K document)")
    }

    println()
}

/**
 * Example 4: Parse Risk Factors
 *
 * Shows how to extract and categorize risk factors from SEC filings.
 */
fun example4_ParseRiskFactors() {
    println("--- Example 4: Parse Risk Factors ---")

    val documentContent =
            """
        ITEM 1A. RISK FACTORS

        Investment in our securities involves risk. You should carefully consider
        the following risks:

        We face intense competition in all aspects of our business.

        The markets for our products and services are highly competitive. We compete
        based on factors including price, product features, relative price/performance,
        product quality and reliability, design innovation, availability of products
        and services, and marketing and distribution capability.

        Our business requires substantial investment in research and development.

        To remain competitive and stimulate customer demand, we must successfully
        manage frequent introductions and transitions of products and services.
        This requires significant investment in research and development.

        Global and regional economic conditions could materially adversely affect us.

        Our operations and performance depend significantly on global and regional
        economic conditions. Adverse macroeconomic conditions, including slow growth
        or recession, high inflation, or decreased consumer confidence can reduce
        demand for our products.

        We are exposed to credit risk and fluctuations in foreign currency exchange rates.

        Our sales and operating results are subject to fluctuations in foreign
        currency exchange rates. Our primary exposures are to the Euro, Japanese
        Yen, British Pound, Chinese Yuan, and other Asian and European currencies.
    """.trimIndent()

    val metadata =
            SecReportMetadata(
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

    val parser = Form10KParser()
    val result = parser.parseText(documentContent, metadata)

    println("Company: ${result.metadata.companyName}")
    println("Total Risk Factors: ${result.riskFactors.size}")

    if (result.riskFactors.isNotEmpty()) {
        println("\nRisk Factors by Category:")

        val byCategory = result.riskFactors.groupBy { it.category }
        byCategory.forEach { (category, risks) ->
            println("\n${category} (${risks.size}):")
            risks.forEach { risk ->
                println("  • ${risk.title}")
                println("    Severity: ${risk.severity}")
                println("    ${risk.summary.take(100)}...")
            }
        }

        // Show severity distribution
        println("\nSeverity Distribution:")
        result.riskFactors.groupBy { it.severity }.forEach { (severity, risks) ->
            println("  $severity: ${risks.size}")
        }
    }

    println()
}

/** Example 5: Using ParserFactory for Auto-Detection */
fun example5_AutoDetectFormat() {
    println("--- Example 5: Auto-Detect File Format ---")

    // ParserFactory automatically detects format from file extension
    val file = File("sample-10k.html")

    val metadata =
            SecReportMetadata(
                    formType = "10-K",
                    filingDate = "2023-11-03",
                    reportDate = "2023-09-30",
                    fiscalYearEnd = "0930",
                    companyName = "Sample Corp",
                    ticker = "SMPL",
                    cik = "0000000001",
                    accessionNumber = "0000000001-23-000001",
                    primaryDocument = "sample-10k.html"
            )

    // Auto-detect and parse
    // val result = ParserFactory.parse(file, metadata)

    println("ParserFactory will automatically use:")
    println("  - HtmlParser for .html/.htm files")
    println("  - PdfFormatParser for .pdf files")
    println("  - TxtParser for .txt files")

    println()
}
