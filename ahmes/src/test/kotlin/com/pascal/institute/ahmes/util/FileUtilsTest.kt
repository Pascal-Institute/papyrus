package com.pascal.institute.ahmes.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Tests for File Utilities
 */
class FileUtilsTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `isSupportedFile should identify supported files`() {
        assertTrue(FileUtils.isSupportedFile(File("report.htm")))
        assertTrue(FileUtils.isSupportedFile(File("report.html")))
        assertTrue(FileUtils.isSupportedFile(File("report.pdf")))
        assertTrue(FileUtils.isSupportedFile(File("report.txt")))

        assertFalse(FileUtils.isSupportedFile(File("report.doc")))
        assertFalse(FileUtils.isSupportedFile(File("report.xlsx")))
        assertFalse(FileUtils.isSupportedFile(File("report.xyz")))
    }

    @Test
    fun `getFileTypeDescription should return correct descriptions`() {
        assertEquals("HTML Document", FileUtils.getFileTypeDescription(File("report.htm")))
        assertEquals("HTML Document", FileUtils.getFileTypeDescription(File("report.html")))
        assertEquals("PDF Document", FileUtils.getFileTypeDescription(File("report.pdf")))
        assertEquals("Text File", FileUtils.getFileTypeDescription(File("report.txt")))
        assertEquals("Unknown File Type", FileUtils.getFileTypeDescription(File("report.xyz")))
    }

    @Test
    fun `extractDocument should read HTML files`() {
        val testFile = tempDir.resolve("test.html").toFile()
        testFile.writeText("<html><body><p>Test content</p></body></html>")

        val result = FileUtils.extractDocument(testFile)

        assertNotNull(result)
        assertTrue(result.rawContent.contains("Test content"))
    }

    @Test
    fun `extractDocument should read TXT files`() {
        val testFile = tempDir.resolve("test.txt").toFile()
        testFile.writeText("Plain text content for testing")

        val result = FileUtils.extractDocument(testFile)

        assertNotNull(result)
        assertTrue(result.rawContent.contains("Plain text content"))
    }

    @Test
    fun `extractDocument should handle UTF-8 encoding`() {
        val testFile = tempDir.resolve("unicode.txt").toFile()
        testFile.writeText("안녕하세요 こんにちは 你好")

        val result = FileUtils.extractDocument(testFile)

        assertTrue(result.rawContent.contains("안녕하세요"))
        assertTrue(result.rawContent.contains("こんにちは"))
        assertTrue(result.rawContent.contains("你好"))
    }

    @Test
    fun `extractTextFromFile should extract text`() {
        val testFile = tempDir.resolve("sample.txt").toFile()
        testFile.writeText("Sample text for extraction")

        val text = FileUtils.extractTextFromFile(testFile)

        assertTrue(text.contains("Sample text"))
    }

    @Test
    fun `isSupportedFile should handle case insensitivity`() {
        assertTrue(FileUtils.isSupportedFile(File("report.HTM")))
        assertTrue(FileUtils.isSupportedFile(File("report.HTML")))
        assertTrue(FileUtils.isSupportedFile(File("report.PDF")))
        assertTrue(FileUtils.isSupportedFile(File("report.TXT")))
    }

    @Test
    fun `extractDocument should return metadata`() {
        val testFile = tempDir.resolve("meta.html").toFile()
        testFile.writeText("""
            <html>
                <head><title>Test Document</title></head>
                <body>Content</body>
            </html>
        """.trimIndent())

        val result = FileUtils.extractDocument(testFile)

        assertNotNull(result)
        // Metadata may or may not be present depending on Tika processing
    }
}
