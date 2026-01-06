package papyrus.core.service.parser

/**
 * Parser Factory
 *
 * Factory class to create appropriate parser based on file extension or content type. Provides
 * centralized parser selection logic.
 */
object ParserFactory {

    private val parsers: List<DocumentParser> = listOf(HtmlParser(), PdfParser(), TxtParser())

    /**
     * Get parser by file extension
     *
     * @param extension File extension (pdf, html, htm, txt)
     * @return Appropriate parser for the extension
     * @throws IllegalArgumentException if extension is not supported
     */
    fun getParserByExtension(extension: String): DocumentParser {
        val normalizedExt = extension.lowercase().trim().removePrefix(".")

        return when (normalizedExt) {
            "pdf" -> PdfParser()
            "html", "htm" -> HtmlParser()
            "txt" -> TxtParser()
            else ->
                    throw IllegalArgumentException(
                            "Unsupported file extension: $extension. Supported: pdf, html, htm, txt"
                    )
        }
    }

    /**
     * Get parser by analyzing content
     *
     * Automatically detects the content type and returns appropriate parser
     *
     * @param content Document content
     * @return Best matching parser for the content
     */
    fun getParserByContent(content: String): DocumentParser {
        // Try each parser's canParse method
        for (parser in parsers) {
            if (parser.canParse(content)) {
                return parser
            }
        }

        // Default to HTML parser if no match (most common SEC format)
        println("Warning: Could not detect content type, defaulting to HTML parser")
        return HtmlParser()
    }

    /**
     * Parse document with automatic parser selection
     *
     * @param content Document content
     * @param documentName Name of the document
     * @param extension Optional file extension hint
     * @return Parse result
     */
    fun parseDocument(
            content: String,
            documentName: String,
            extension: String? = null
    ): ParseResult {
        val parser =
                if (extension != null) {
                    try {
                        getParserByExtension(extension)
                    } catch (e: IllegalArgumentException) {
                        println("Invalid extension '$extension', auto-detecting content type")
                        getParserByContent(content)
                    }
                } else {
                    getParserByContent(content)
                }

        println("Using ${parser.javaClass.simpleName} to parse $documentName")
        return parser.parse(content, documentName)
    }

    /** Get all supported extensions */
    fun getSupportedExtensions(): List<String> {
        return parsers.map { it.getSupportedExtension() }
    }

    /** Check if extension is supported */
    fun isExtensionSupported(extension: String): Boolean {
        val normalized = extension.lowercase().trim().removePrefix(".")
        return try {
            getParserByExtension(normalized)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
