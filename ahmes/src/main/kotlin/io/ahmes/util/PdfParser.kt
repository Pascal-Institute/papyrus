package io.ahmes.util

import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

object PdfParser {
    /** Extract text content from a PDF file */
    fun extractText(file: File): String {
        return try {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } catch (e: Exception) {
            throw Exception("Failed to extract text from PDF: ${e.message}", e)
        }
    }

    /** Extract text with page information */
    fun extractTextWithPages(file: File): List<Pair<Int, String>> {
        return try {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper()
                val pages = mutableListOf<Pair<Int, String>>()

                for (pageNum in 1..document.numberOfPages) {
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    val pageText = stripper.getText(document)
                    pages.add(Pair(pageNum, pageText))
                }

                pages
            }
        } catch (e: Exception) {
            throw Exception("Failed to extract pages from PDF: ${e.message}", e)
        }
    }

    /**
     * SEC document-specific text extraction - preserves table structure
     */
    fun extractTextForSec(file: File): SecDocumentText {
        return try {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    addMoreFormatting = true
                }

                val fullText = stripper.getText(document)
                val sections = mutableListOf<SecSection>()
                val pageTexts = mutableListOf<String>()

                // Extract text page by page
                for (pageNum in 1..document.numberOfPages) {
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    pageTexts.add(stripper.getText(document))
                }

                // Identify SEC sections
                val sectionMarkers = listOf(
                    SectionMarker("CONSOLIDATED STATEMENTS OF OPERATIONS", SecSectionType.INCOME_STATEMENT),
                    SectionMarker("CONSOLIDATED STATEMENTS OF INCOME", SecSectionType.INCOME_STATEMENT),
                    SectionMarker("CONSOLIDATED STATEMENTS OF EARNINGS", SecSectionType.INCOME_STATEMENT),
                    SectionMarker("STATEMENTS OF OPERATIONS", SecSectionType.INCOME_STATEMENT),
                    SectionMarker("CONSOLIDATED BALANCE SHEETS", SecSectionType.BALANCE_SHEET),
                    SectionMarker("CONSOLIDATED BALANCE SHEET", SecSectionType.BALANCE_SHEET),
                    SectionMarker("BALANCE SHEETS", SecSectionType.BALANCE_SHEET),
                    SectionMarker("CONSOLIDATED STATEMENTS OF CASH FLOWS", SecSectionType.CASH_FLOW),
                    SectionMarker("STATEMENTS OF CASH FLOWS", SecSectionType.CASH_FLOW),
                    SectionMarker("CONSOLIDATED STATEMENTS OF COMPREHENSIVE INCOME", SecSectionType.COMPREHENSIVE_INCOME),
                    SectionMarker("CONSOLIDATED STATEMENTS OF SHAREHOLDERS", SecSectionType.EQUITY_STATEMENT),
                    SectionMarker("CONSOLIDATED STATEMENTS OF STOCKHOLDERS", SecSectionType.EQUITY_STATEMENT),
                    SectionMarker("NOTES TO CONSOLIDATED FINANCIAL STATEMENTS", SecSectionType.NOTES),
                    SectionMarker("MANAGEMENT'S DISCUSSION AND ANALYSIS", SecSectionType.MDA),
                    SectionMarker("RISK FACTORS", SecSectionType.RISK_FACTORS)
                )

                for (marker in sectionMarkers) {
                    val startIdx = fullText.uppercase().indexOf(marker.pattern.uppercase())
                    if (startIdx != -1) {
                        val endIdx = findNextSectionEnd(fullText, startIdx + marker.pattern.length, sectionMarkers)
                        val sectionText = fullText.substring(startIdx, minOf(endIdx, startIdx + 30000))
                        sections.add(SecSection(marker.type, sectionText, startIdx))
                    }
                }

                val documentType = detectDocumentType(fullText)
                val fiscalYear = extractFiscalYear(fullText)
                val companyName = extractCompanyName(fullText, document.documentInformation.title ?: "")

                SecDocumentText(
                    fullText = fullText,
                    pageTexts = pageTexts,
                    sections = sections,
                    documentType = documentType,
                    fiscalYear = fiscalYear,
                    companyName = companyName,
                    pageCount = document.numberOfPages
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to extract SEC document text: ${e.message}", e)
        }
    }

    /** Extract financial statement tables */
    fun extractFinancialTables(file: File): List<ExtractedTable> {
        return try {
            PDDocument.load(file).use { document ->
                val stripper = PDFTextStripper().apply { sortByPosition = true }

                val tables = mutableListOf<ExtractedTable>()

                for (pageNum in 1..document.numberOfPages) {
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    val pageText = stripper.getText(document)

                    if (isFinancialStatementPage(pageText)) {
                        val tableRows = parseTableFromText(pageText)
                        if (tableRows.isNotEmpty()) {
                            val tableType = detectTableType(pageText)
                            tables.add(
                                ExtractedTable(
                                    type = tableType,
                                    rows = tableRows,
                                    pageNumber = pageNum,
                                    unit = detectUnit(pageText)
                                )
                            )
                        }
                    }
                }

                tables
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun findNextSectionEnd(text: String, startIdx: Int, markers: List<SectionMarker>): Int {
        var minEnd = text.length
        val upperText = text.uppercase()

        for (marker in markers) {
            val idx = upperText.indexOf(marker.pattern.uppercase(), startIdx + 100)
            if (idx != -1 && idx < minEnd) {
                minEnd = idx
            }
        }

        val notesIdx = upperText.indexOf("NOTES TO", startIdx + 100)
        if (notesIdx != -1 && notesIdx < minEnd) minEnd = notesIdx

        val itemPattern = Regex("""ITEM\s+\d""", RegexOption.IGNORE_CASE)
        val itemMatch = itemPattern.find(text, startIdx + 100)
        if (itemMatch != null && itemMatch.range.first < minEnd) {
            minEnd = itemMatch.range.first
        }

        return minEnd
    }

    private fun detectDocumentType(text: String): SecDocumentType {
        val upperText = text.uppercase().take(5000)
        return when {
            upperText.contains("FORM 10-K") || upperText.contains("ANNUAL REPORT") -> SecDocumentType.FORM_10K
            upperText.contains("FORM 10-Q") || upperText.contains("QUARTERLY REPORT") -> SecDocumentType.FORM_10Q
            upperText.contains("FORM 8-K") -> SecDocumentType.FORM_8K
            upperText.contains("FORM 20-F") -> SecDocumentType.FORM_20F
            upperText.contains("PROXY STATEMENT") || upperText.contains("DEF 14A") -> SecDocumentType.PROXY
            else -> SecDocumentType.OTHER
        }
    }

    private fun extractFiscalYear(text: String): String? {
        val patterns = listOf(
            Regex("""fiscal\s+year\s+ended?\s+([A-Za-z]+\s+\d{1,2},?\s+\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""for\s+the\s+year\s+ended?\s+([A-Za-z]+\s+\d{1,2},?\s+\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:fiscal|year)\s+(?:year\s+)?(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:FY|FYE)\s*['"]?(\d{2,4})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text.take(10000))
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractCompanyName(text: String, title: String): String {
        if (title.isNotBlank() && !title.contains("10-K", ignoreCase = true)) {
            return title.trim()
        }

        val patterns = listOf(
            Regex("""([A-Z][A-Za-z\s,\.]+(?:Inc\.|Corp\.|Corporation|Company|Ltd\.?|LLC))"""),
            Regex("""UNITED STATES.*?\n\n\s*([A-Z][A-Za-z\s,\.]+)\n""", setOf(RegexOption.DOT_MATCHES_ALL))
        )

        for (pattern in patterns) {
            val match = pattern.find(text.take(3000))
            if (match != null && match.groupValues[1].length > 3) {
                return match.groupValues[1].trim()
            }
        }

        return "Unknown Company"
    }

    private fun isFinancialStatementPage(text: String): Boolean {
        val upperText = text.uppercase()
        val keywords = listOf(
            "CONSOLIDATED", "STATEMENTS OF", "BALANCE SHEET", "INCOME",
            "OPERATIONS", "CASH FLOWS", "ASSETS", "LIABILITIES",
            "EQUITY", "REVENUE", "NET SALES"
        )

        val matchCount = keywords.count { upperText.contains(it) }
        val hasNumbers = Regex("""\$?\s*[\d,]+(?:\.\d+)?""").containsMatchIn(text)

        return matchCount >= 3 && hasNumbers
    }

    private fun detectTableType(text: String): TableType {
        val upperText = text.uppercase().take(1000)
        return when {
            upperText.contains("OPERATIONS") ||
                    upperText.contains("INCOME") && !upperText.contains("COMPREHENSIVE") -> TableType.INCOME_STATEMENT
            upperText.contains("BALANCE") ||
                    upperText.contains("ASSETS") && upperText.contains("LIABILITIES") -> TableType.BALANCE_SHEET
            upperText.contains("CASH FLOWS") -> TableType.CASH_FLOW
            upperText.contains("COMPREHENSIVE") -> TableType.COMPREHENSIVE_INCOME
            upperText.contains("EQUITY") || upperText.contains("SHAREHOLDERS") -> TableType.EQUITY
            else -> TableType.OTHER
        }
    }

    private fun detectUnit(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("in billions") || lowerText.contains("(billions)") -> "billions"
            lowerText.contains("in millions") ||
                    lowerText.contains("(in millions)") ||
                    lowerText.contains("$ in millions") -> "millions"
            lowerText.contains("in thousands") || lowerText.contains("(in thousands)") -> "thousands"
            else -> "millions"
        }
    }

    private fun parseTableFromText(text: String): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        val lines = text.split("\n")

        val numberPattern = Regex("""\(?\$?\s*[\d,]+(?:\.\d+)?\)?""")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.length < 5) continue

            val numbers = numberPattern.findAll(trimmedLine).toList()
            if (numbers.isEmpty()) continue

            val firstNumberStart = numbers.first().range.first
            val label = trimmedLine.substring(0, firstNumberStart).trim()

            if (label.length < 3) continue
            if (label.all { it.isDigit() || it.isWhitespace() || it == ',' || it == '.' }) continue
            if (label.contains("Page ") || label.matches(Regex("""F-\d+"""))) continue

            val values = numbers.map { match ->
                val valueStr = match.value.trim()
                parseNumericValue(valueStr)
            }

            val lowerLabel = label.lowercase()
            val isTotal = lowerLabel.startsWith("total") || lowerLabel.contains("total ")
            val isSubtotal = lowerLabel.contains("subtotal")

            val category = inferCategory(label)

            rows.add(
                TableRow(
                    label = label,
                    values = values,
                    isTotal = isTotal,
                    isSubtotal = isSubtotal,
                    category = category
                )
            )
        }

        return rows
    }

    private fun parseNumericValue(valueStr: String): Double? {
        val cleaned = valueStr.replace("$", "").replace(",", "").replace(" ", "").trim()

        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "—") return null

        val isNegative = cleaned.startsWith("(") && cleaned.endsWith(")")
        val numberStr = cleaned.replace("(", "").replace(")", "")

        return try {
            val value = numberStr.toDouble()
            if (isNegative) -value else value
        } catch (e: Exception) {
            null
        }
    }

    private fun inferCategory(label: String): String {
        val lowerLabel = label.lowercase().trim()

        return when {
            lowerLabel.contains("revenue") || lowerLabel.contains("net sales") || lowerLabel == "sales" -> "REVENUE"
            lowerLabel.contains("cost of") && (lowerLabel.contains("revenue") || lowerLabel.contains("sales") || lowerLabel.contains("goods")) -> "COST_OF_REVENUE"
            lowerLabel == "gross profit" || lowerLabel == "gross margin" -> "GROSS_PROFIT"
            lowerLabel.contains("operating income") || lowerLabel.contains("income from operations") -> "OPERATING_INCOME"
            lowerLabel == "net income" || lowerLabel.contains("net income (loss)") || lowerLabel == "net earnings" -> "NET_INCOME"
            lowerLabel == "total assets" -> "TOTAL_ASSETS"
            lowerLabel.contains("current assets") -> "CURRENT_ASSETS"
            lowerLabel.contains("cash and cash equivalents") || lowerLabel == "cash" -> "CASH"
            lowerLabel.contains("accounts receivable") -> "ACCOUNTS_RECEIVABLE"
            lowerLabel.contains("inventor") -> "INVENTORY"
            lowerLabel == "total liabilities" -> "TOTAL_LIABILITIES"
            lowerLabel.contains("current liabilities") -> "CURRENT_LIABILITIES"
            lowerLabel.contains("long-term debt") || lowerLabel.contains("long term debt") -> "LONG_TERM_DEBT"
            lowerLabel.contains("accounts payable") -> "ACCOUNTS_PAYABLE"
            lowerLabel.contains("equity") || lowerLabel.contains("shareholders") || lowerLabel.contains("stockholders") -> "EQUITY"
            lowerLabel.contains("retained earnings") || lowerLabel.contains("accumulated deficit") -> "RETAINED_EARNINGS"
            lowerLabel.contains("operating activities") || lowerLabel.contains("cash from operations") -> "OPERATING_CASH_FLOW"
            lowerLabel.contains("investing activities") -> "INVESTING_CASH_FLOW"
            lowerLabel.contains("financing activities") -> "FINANCING_CASH_FLOW"
            lowerLabel.contains("research and development") || lowerLabel.contains("r&d") -> "RD_EXPENSE"
            lowerLabel.contains("selling") && lowerLabel.contains("administrative") -> "SGA_EXPENSE"
            lowerLabel.contains("interest expense") -> "INTEREST_EXPENSE"
            lowerLabel.contains("depreciation") -> "DEPRECIATION"
            lowerLabel.contains("earnings per share") || lowerLabel.contains("eps") -> "EPS"
            else -> "OTHER"
        }
    }

    /** Get PDF metadata */
    fun getPdfInfo(file: File): Map<String, String> {
        return try {
            PDDocument.load(file).use { document ->
                val info = document.documentInformation
                mapOf(
                    "title" to (info.title ?: ""),
                    "author" to (info.author ?: ""),
                    "subject" to (info.subject ?: ""),
                    "keywords" to (info.keywords ?: ""),
                    "creator" to (info.creator ?: ""),
                    "producer" to (info.producer ?: ""),
                    "pageCount" to document.numberOfPages.toString()
                )
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

// SEC document-related data classes
data class SecDocumentText(
    val fullText: String,
    val pageTexts: List<String>,
    val sections: List<SecSection>,
    val documentType: SecDocumentType,
    val fiscalYear: String?,
    val companyName: String,
    val pageCount: Int
)

data class SecSection(
    val type: SecSectionType,
    val content: String,
    val startIndex: Int
)

data class SectionMarker(
    val pattern: String,
    val type: SecSectionType
)

enum class SecSectionType {
    INCOME_STATEMENT,
    BALANCE_SHEET,
    CASH_FLOW,
    COMPREHENSIVE_INCOME,
    EQUITY_STATEMENT,
    NOTES,
    MDA,
    RISK_FACTORS,
    OTHER
}

enum class SecDocumentType {
    FORM_10K,
    FORM_10Q,
    FORM_8K,
    FORM_20F,
    PROXY,
    OTHER
}

data class ExtractedTable(
    val type: TableType,
    val rows: List<TableRow>,
    val pageNumber: Int,
    val unit: String
)

data class TableRow(
    val label: String,
    val values: List<Double?>,
    val isTotal: Boolean = false,
    val isSubtotal: Boolean = false,
    val category: String = "OTHER"
)

enum class TableType {
    INCOME_STATEMENT,
    BALANCE_SHEET,
    CASH_FLOW,
    COMPREHENSIVE_INCOME,
    EQUITY,
    OTHER
}
