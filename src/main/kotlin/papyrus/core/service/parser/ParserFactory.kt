package papyrus.core.service.parser

@Deprecated(
    message = "Use com.pascal.institute.ahmes.format.ParserFactory directly",
    level = DeprecationLevel.ERROR
)
object ParserFactory {

    private val delegate = com.pascal.institute.ahmes.format.ParserFactory

    fun getParserByExtension(extension: String): DocumentParser = delegate.getParserByExtension(extension)

    fun getParserByContent(content: String): DocumentParser = delegate.getParserByContent(content)

    fun parseDocument(content: String, documentName: String, extension: String? = null): ParseResult {
        return delegate.parseDocument(content, documentName, extension)
    }

    fun getSupportedExtensions(): List<String> = delegate.getSupportedExtensions()

    fun isExtensionSupported(extension: String): Boolean = delegate.isExtensionSupported(extension)
}
