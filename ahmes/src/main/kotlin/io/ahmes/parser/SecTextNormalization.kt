package io.ahmes.parser

import org.jsoup.Jsoup

/**
 * Common text normalization utilities used across SEC parsers.
 *
 * Keep behavior stable: changes here affect many parsers.
 */
object SecTextNormalization {

    fun cleanHtmlToText(html: String): String {
        val doc = Jsoup.parse(html)

        // Remove scripts/styles early to avoid polluting extracted text.
        doc.select("script, style").remove()

        // Convert namespaced tags (XBRL / iXBRL) to a neutral tag while preserving content.
        doc.select("*").forEach { element ->
            if (element.tagName().contains(":")) {
                element.tagName("span")
            }
        }

        // Jsoup already strips tags when calling text(). We keep basic entity decoding for
        // consistency with previous behavior.
        val text = decodeBasicEntities(doc.text())
        return normalizeWhitespace(text)
    }

    fun decodeBasicEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Collapse spaces/tabs while preserving newlines.
     * Also caps consecutive blank lines to at most 2.
     */
    fun normalizeWhitespacePreserveNewlines(text: String): String {
        var cleaned = text
        cleaned = cleaned.replace(Regex("[ \\t]+"), " ")
        cleaned = cleaned.replace(Regex("\\n\\s*\\n"), "\n\n")
        return cleaned.trim()
    }
}
