package papyrus

import java.io.File

object FileUtils {
    /** Extract text content from a file based on its extension */
    fun extractTextFromFile(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> {
                println("Extracting text from PDF: ${file.name}")
                PdfParser.extractText(file)
            }
            "html", "htm" -> {
                println("Reading HTML file: ${file.name}")
                file.readText(Charsets.UTF_8)
            }
            "txt" -> {
                println("Reading text file: ${file.name}")
                file.readText(Charsets.UTF_8)
            }
            else -> {
                // Try to read as text, might work for some formats
                println("Attempting to read file as text: ${file.name}")
                file.readText(Charsets.UTF_8)
            }
        }
    }

    /** Check if file type is supported */
    fun isSupportedFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("pdf", "html", "htm", "txt")
    }

    /** Get a user-friendly description of the file type */
    fun getFileTypeDescription(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "PDF Document"
            "html", "htm" -> "HTML Document"
            "txt" -> "Text File"
            else -> "Unknown File Type"
        }
    }
}
