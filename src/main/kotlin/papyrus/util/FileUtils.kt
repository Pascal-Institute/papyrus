package papyrus.util

import java.io.File

object FileUtils {
    
    // Centralized file extension constants to avoid duplication
    private val SUPPORTED_EXTENSIONS = setOf("pdf", "html", "htm", "txt")
    private val HTML_EXTENSIONS = setOf("html", "htm")
    
    data class ExtractedDocument(
        val rawContent: String,
        val extractedText: String,
        val mimeType: String? = null,
        val metadata: Map<String, String> = emptyMap(),
    )

    /**
     * Extract both raw content and extracted text.
     * - For HTML/HTM/TXT we keep raw for parsers that need markup (e.g., XBRL).
     * - For PDF we keep rawContent empty and use Tika-extracted text.
     */
    fun extractDocument(file: File): ExtractedDocument {
        val ext = file.extension.lowercase()
        return when {
            ext in HTML_EXTENSIONS || ext == "txt" -> {
                val raw = file.readText(Charsets.UTF_8)
                val tika = runCatching { TikaExtractor.extract(file) }.getOrNull()
                ExtractedDocument(
                    rawContent = raw,
                    extractedText = tika?.extractedText ?: raw,
                    mimeType = tika?.mimeType,
                    metadata = tika?.metadata ?: emptyMap(),
                )
            }
            ext == "pdf" -> {
                val tika = TikaExtractor.extract(file)
                ExtractedDocument(
                    rawContent = "",
                    extractedText = tika.extractedText,
                    mimeType = tika.mimeType,
                    metadata = tika.metadata,
                )
            }
            else -> {
                val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrElse { "" }
                val tika = runCatching { TikaExtractor.extract(file) }.getOrNull()
                ExtractedDocument(
                    rawContent = raw,
                    extractedText = tika?.extractedText ?: raw,
                    mimeType = tika?.mimeType,
                    metadata = tika?.metadata ?: emptyMap(),
                )
            }
        }
    }

    /** Extract text content from a file based on its extension */
    fun extractTextFromFile(file: File): String {
        val extracted = extractDocument(file)
        return extracted.extractedText
    }

    /** Check if file type is supported */
    fun isSupportedFile(file: File): Boolean {
        return file.extension.lowercase() in SUPPORTED_EXTENSIONS
    }

    /** Get a user-friendly description of the file type */
    fun getFileTypeDescription(file: File): String {
        val ext = file.extension.lowercase()
        return when {
            ext == "pdf" -> "PDF Document"
            ext in HTML_EXTENSIONS -> "HTML Document"
            ext == "txt" -> "Text File"
            else -> "Unknown File Type"
        }
    }
}
