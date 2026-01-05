package papyrus.core.service.parser

/**
 * Common text normalization utilities used across SEC parsers.
 *
 * Keep behavior stable: changes here affect many parsers.
 */
object SecTextNormalization {

    fun cleanHtmlToText(html: String): String {
        var cleaned = html

        cleaned =
            cleaned.replace(
                Regex(
                    "<(SCRIPT|script)[^>]*>.*?</(SCRIPT|script)>",
                    RegexOption.DOT_MATCHES_ALL
                ),
                ""
            )
        cleaned =
            cleaned.replace(
                Regex(
                    "<(STYLE|style)[^>]*>.*?</(STYLE|style)>",
                    RegexOption.DOT_MATCHES_ALL
                ),
                ""
            )

        // Remove XBRL tags but keep content
        cleaned = cleaned.replace(Regex("</?ix:[^>]+>"), "")
        cleaned = cleaned.replace(Regex("</?xbrli:[^>]+>"), "")

        // Remove HTML tags
        cleaned = cleaned.replace(Regex("<[^>]+>"), " ")

        cleaned = decodeBasicEntities(cleaned)
        return normalizeWhitespace(cleaned)
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
