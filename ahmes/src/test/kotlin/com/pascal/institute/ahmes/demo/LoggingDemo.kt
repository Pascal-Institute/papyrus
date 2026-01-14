package com.pascal.institute.ahmes.demo

import com.pascal.institute.ahmes.parser.EnhancedFinancialParser
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Demo application to showcase logging functionality Demonstrates AGENTS.md Principle #7 (Radical
 * Truth) and #11 (Plan Like Amundsen)
 */
fun main() {
    logger.info { "========================================" }
    logger.info { "Ahmes Parser Logging Demo - START" }
    logger.info { "========================================" }

    val sampleFinancialData =
            """
        CONSOLIDATED STATEMENTS OF OPERATIONS
        (In millions, except per share data)

                                                2023        2022         2021
        Total Revenue                          ${'$'}394,328    ${'$'}365,817    ${'$'}365,825
        Cost of Revenue                         214,137     223,546      212,981
        ------------------------------------------------------------------
        Gross Profit                            180,191     142,271      152,844

        Operating Expenses:
            Research and Development             29,915      26,251       21,914
            Selling, General and Administrative  24,932      25,094       21,973
        ------------------------------------------------------------------
        Operating Income                         114,301      98,392      108,949

        Net Income                               96,995      94,321       94,680

        Earnings Per Share:
            Basic                                ${'$'}6.16      ${'$'}6.15       ${'$'}5.67
            Diluted                              ${'$'}6.13      ${'$'}6.11       ${'$'}5.61

        CONSOLIDATED BALANCE SHEETS
        (In millions)

                                                2023        2022
        Assets:
        Cash and Cash Equivalents              ${'$'}30,737    ${'$'}23,646
        Accounts Receivable                      29,508      28,184
        Inventories                               6,331       4,946
        Total Current Assets                    143,566     135,405
        Total Assets                            352,755     352,583

        Liabilities:
        Accounts Payable                         62,611      64,115
        Total Current Liabilities               153,982     153,982
        Long-term Debt                           98,959     111,109
        Total Liabilities                       290,437     302,083

        Stockholders' Equity:
        Total Equity                             62,146      50,672
    """.trimIndent()

    try {
        logger.info { "Starting financial metrics parsing demo..." }

        // Parse financial metrics
        val metrics = EnhancedFinancialParser.parseFinancialMetrics(sampleFinancialData)
        logger.info { "Successfully parsed ${metrics.size} financial metrics" }

        // Show some metrics
        metrics.take(5).forEach { metric ->
            logger.info {
                "Metric: ${metric.name} = ${metric.value} (Category: ${metric.category}, Confidence: ${metric.confidence})"
            }
        }

        // Calculate ratios
        logger.info { "Calculating financial ratios..." }
        val ratios = EnhancedFinancialParser.calculateRatios(metrics)
        logger.info { "Successfully calculated ${ratios.size} financial ratios" }

        // Show ratios
        ratios.forEach { ratio ->
            logger.info {
                "Ratio: ${ratio.name} = ${ratio.formattedValue} (Status: ${ratio.healthStatus}, Category: ${ratio.category})"
            }
        }

        // Parse risk factors
        val riskContent =
                """
            ITEM 1A. RISK FACTORS

            Market Competition - We face intense competition in the technology sector which may impact our market share.

            Economic Conditions - Global economic uncertainties could adversely affect our business operations.

            Cybersecurity Threats - We face increasing cybersecurity risks that could compromise customer data.
        """.trimIndent()

        logger.info { "Parsing risk factors..." }
        val risks = EnhancedFinancialParser.parseRiskFactors(riskContent)
        logger.info { "Successfully parsed ${risks.size} risk factors" }

        risks.forEach { risk ->
            logger.info {
                "Risk: ${risk.title} (Category: ${risk.category}, Severity: ${risk.severity})"
            }
        }
    } catch (e: Exception) {
        logger.error(e) { "Demo failed with exception" }
    }

    logger.info { "========================================" }
    logger.info { "Ahmes Parser Logging Demo - COMPLETE" }
    logger.info { "========================================" }
}
