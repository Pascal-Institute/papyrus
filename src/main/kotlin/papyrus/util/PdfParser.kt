package papyrus.util

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
