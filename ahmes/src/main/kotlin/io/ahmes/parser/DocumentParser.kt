package io.ahmes.parser

import io.ahmes.model.FinancialMetric

/**
 * Document Parser Interface
 *
 * Base interface for all document format parsers. Each file format (PDF, HTML, TXT) implements this
 * interface to provide format-specific parsing logic.
 */
interface DocumentParser {

    /**
     * Parse raw document content into structured financial metrics
     *
     * @param content Raw document content (text, HTML, etc.)
     * @param documentName Name of the document for tracking
     * @return List of extracted financial metrics
     */
    fun parse(content: String, documentName: String): ParseResult

    /**
     * Check if this parser can handle the given content
     *
     * @param content Document content
     * @return true if this parser supports the content format
     */
    fun canParse(content: String): Boolean

    /** Get the supported file extension */
    fun getSupportedExtension(): String
}

/**
 * Parse Result
 *
 * Container for parsed financial data with metadata
 */
data class ParseResult(
    val metrics: List<FinancialMetric>,
    val documentName: String,
    val parserType: String,
    val rawContent: String,
    val cleanedContent: String,
    val metadata: Map<String, String> = emptyMap()
)
