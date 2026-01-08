package papyrus.core.service.parser

import com.pascal.institute.ahmes.format.HtmlParser
import com.pascal.institute.ahmes.format.ParserFactory
import com.pascal.institute.ahmes.format.PdfFormatParser
import com.pascal.institute.ahmes.format.TxtParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Integration tests for ParserFactory
 *
 * Tests the automatic parser selection and document parsing workflow
 */
class ParserFactoryTest {

    @Test
    @DisplayName("Should select HtmlParser for HTML content")
    fun testHtmlParserSelection() {
        val htmlContent = "<html><body><h1>Test</h1></body></html>"
        val parser = ParserFactory.getParserByContent(htmlContent)

        assertTrue(parser is HtmlParser)
    }

    @Test
    @DisplayName("Should select PdfParser for PDF content")
    fun testPdfParserSelection() {
        val pdfMagicBytes = "%PDF-1.4\n"
        val parser = ParserFactory.getParserByContent(pdfMagicBytes)

        assertTrue(parser is PdfFormatParser)
    }

    @Test
    @DisplayName("Should select TxtParser for plain text")
    fun testTxtParserSelection() {
        val plainText =
                """
            This is plain text without any markup.
            It contains multiple lines and enough content to be detected as plain text.
            We need sufficient text to ensure proper parser detection.
        """.trimIndent()
        val parser = ParserFactory.getParserByContent(plainText)

        assertTrue(parser is TxtParser)
    }

    @Test
    @DisplayName("Should get parser by extension - html")
    fun testGetParserByExtensionHtml() {
        val parser = ParserFactory.getParserByExtension("html")
        assertTrue(parser is HtmlParser)

        val parser2 = ParserFactory.getParserByExtension("HTM")
        assertTrue(parser2 is HtmlParser)

        val parser3 = ParserFactory.getParserByExtension(".html")
        assertTrue(parser3 is HtmlParser)
    }

    @Test
    @DisplayName("Should get parser by extension - pdf")
    fun testGetParserByExtensionPdf() {
        val parser = ParserFactory.getParserByExtension("pdf")
        assertTrue(parser is PdfFormatParser)

        val parser2 = ParserFactory.getParserByExtension("PDF")
        assertTrue(parser2 is PdfFormatParser)
    }

    @Test
    @DisplayName("Should get parser by extension - txt")
    fun testGetParserByExtensionTxt() {
        val parser = ParserFactory.getParserByExtension("txt")
        assertTrue(parser is TxtParser)
    }

    @Test
    @DisplayName("Should throw exception for unsupported extension")
    fun testUnsupportedExtension() {
        assertThrows(IllegalArgumentException::class.java) {
            ParserFactory.getParserByExtension("doc")
        }

        assertThrows(IllegalArgumentException::class.java) {
            ParserFactory.getParserByExtension("xlsx")
        }
    }

    @Test
    @DisplayName("Should check extension support correctly")
    fun testIsExtensionSupported() {
        assertTrue(ParserFactory.isExtensionSupported("html"))
        assertTrue(ParserFactory.isExtensionSupported("htm"))
        assertTrue(ParserFactory.isExtensionSupported("pdf"))
        assertTrue(ParserFactory.isExtensionSupported("txt"))

        assertFalse(ParserFactory.isExtensionSupported("doc"))
        assertFalse(ParserFactory.isExtensionSupported("xlsx"))
        assertFalse(ParserFactory.isExtensionSupported("unknown"))
    }

    @Test
    @DisplayName("Should return list of supported extensions")
    fun testGetSupportedExtensions() {
        val extensions = ParserFactory.getSupportedExtensions()

        assertEquals(3, extensions.size)
        assertTrue(extensions.contains("html"))
        assertTrue(extensions.contains("pdf"))
        assertTrue(extensions.contains("txt"))
    }

    @Test
    @DisplayName("Should parse document with HTML extension hint")
    fun testParseDocumentWithExtension() {
        val content = "<html><body><p>Revenue: $1000</p></body></html>"
        val result = ParserFactory.parseDocument(content, "test.html", "html")

        assertNotNull(result)
        assertEquals("test.html", result.documentName)
        assertTrue(result.parserType.contains("HTML"))
    }

    @Test
    @DisplayName("Should parse document without extension hint")
    fun testParseDocumentWithoutExtension() {
        val content = "<html><body><p>Revenue: $1000</p></body></html>"
        val result = ParserFactory.parseDocument(content, "test-doc")

        assertNotNull(result)
        assertEquals("test-doc", result.documentName)
    }

    @Test
    @DisplayName("Should auto-detect HTML content when extension is wrong")
    fun testAutoDetectWhenExtensionWrong() {
        val htmlContent = "<html><body>Test</body></html>"

        // Even with wrong extension hint, should auto-detect HTML
        val result = ParserFactory.parseDocument(htmlContent, "test.doc", "doc")

        assertNotNull(result)
        // Should have used HTML parser despite wrong extension
        assertTrue(result.parserType.contains("HTML"))
    }

    @Test
    @DisplayName("Should handle DOCTYPE variations")
    fun testDoctypeVariations() {
        val html1 = "<!DOCTYPE html><html><body>Test</body></html>"
        val parser1 = ParserFactory.getParserByContent(html1)
        assertTrue(parser1 is HtmlParser)

        val html2 =
                "<!doctype HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"><html><body>Test</body></html>"
        val parser2 = ParserFactory.getParserByContent(html2)
        assertTrue(parser2 is HtmlParser)
    }

    @Test
    @DisplayName("Should default to HtmlParser for ambiguous content")
    fun testDefaultToHtmlParser() {
        val ambiguousContent = "Some content that could be anything"
        val parser = ParserFactory.getParserByContent(ambiguousContent)

        // Should default to HtmlParser when no parser can clearly identify the content
        assertTrue(parser is HtmlParser)
    }
}
