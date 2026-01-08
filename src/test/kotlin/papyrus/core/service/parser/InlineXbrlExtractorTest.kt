package papyrus.core.service.parser

import com.pascal.institute.ahmes.format.HtmlParser
import com.pascal.institute.ahmes.parser.InlineXbrlExtractor
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class InlineXbrlExtractorTest {

    @Test
    @DisplayName("Should extract scaled numeric iXBRL facts with context")
    fun extractScaledFacts() {
        val html =
            """
            <html xmlns:ix="http://www.xbrl.org/2013/inlineXBRL" xmlns:xbrli="http://www.xbrl.org/2003/instance">
              <body>
                <xbrli:context id="C_2024">
                  <xbrli:period>
                    <xbrli:instant>2024-12-31</xbrli:instant>
                  </xbrli:period>
                </xbrli:context>

                <ix:nonFraction name="us-gaap:Revenues" contextRef="C_2024" unitRef="USD" scale="6">383,285</ix:nonFraction>
                <ix:nonFraction name="us-gaap:NetIncomeLoss" contextRef="C_2024" unitRef="USD">96,995</ix:nonFraction>
              </body>
            </html>
            """.trimIndent()

        val doc = Jsoup.parse(html)
        val metrics = InlineXbrlExtractor.extractMetrics(doc)

        val revenue = metrics.firstOrNull { it.name.equals("Revenue", ignoreCase = true) }
        assertNotNull(revenue)
        // 383,285 with scale=6 => 383,285,000,000
        val expectedRevenue = java.math.BigDecimal("383285000000")
        assertEquals(0, expectedRevenue.compareTo(revenue!!.getRawValueBigDecimal()),
            "Expected $expectedRevenue but got ${revenue.getRawValueBigDecimal()}")
        assertTrue(revenue.context.contains("period=2024-12-31"))

        val netIncome = metrics.firstOrNull { it.name.equals("Net Income", ignoreCase = true) }
        assertNotNull(netIncome)
        val expectedNetIncome = java.math.BigDecimal("96995")
        assertEquals(0, expectedNetIncome.compareTo(netIncome!!.getRawValueBigDecimal()),
            "Expected $expectedNetIncome but got ${netIncome.getRawValueBigDecimal()}")
    }

    @Test
    @DisplayName("HtmlParser should include iXBRL metrics in results")
    fun htmlParserIncludesXbrlMetrics() {
        val html =
            """
            <html xmlns:ix="http://www.xbrl.org/2013/inlineXBRL" xmlns:xbrli="http://www.xbrl.org/2003/instance">
              <body>
                <xbrli:context id="C_2024">
                  <xbrli:period>
                    <xbrli:instant>2024-12-31</xbrli:instant>
                  </xbrli:period>
                </xbrli:context>
                <ix:nonFraction name="us-gaap:Assets" contextRef="C_2024" unitRef="USD">352,583</ix:nonFraction>
              </body>
            </html>
            """.trimIndent()

        val parser = HtmlParser()
        val result = parser.parse(html, "ixbrl.html")

        assertTrue(result.metadata["xbrlMetricCount"]?.toIntOrNull() ?: 0 >= 1)
        assertTrue(result.metrics.any { it.name.equals("Total Assets", ignoreCase = true) })
    }
}
