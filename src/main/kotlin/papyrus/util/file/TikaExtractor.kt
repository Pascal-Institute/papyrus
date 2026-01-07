package papyrus.util.file

import java.io.File
import java.io.InputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler

/**
 * Apache Tika-based text extractor.
 *
 * Provides a single extraction path for PDF/HTML/TXT and other common document types.
 */
object TikaExtractor {

    data class ExtractionResult(
            val extractedText: String,
            val mimeType: String? = null,
            val metadata: Map<String, String> = emptyMap(),
    )

    fun extract(file: File): ExtractionResult {
        file.inputStream().use { input ->
            return extract(input, file.name)
        }
    }

    fun extract(input: InputStream, resourceName: String? = null): ExtractionResult {
        val parser = AutoDetectParser()
        val handler = BodyContentHandler(-1)
        val metadata =
                Metadata().apply {
                    if (!resourceName.isNullOrBlank()) {
                        set(Metadata.RESOURCE_NAME_KEY, resourceName)
                    }
                }

        parser.parse(input, handler, metadata)

        val mimeType = metadata.get(Metadata.CONTENT_TYPE)
        val metaMap = metadata.names().associateWith { name -> metadata.get(name).orEmpty() }

        return ExtractionResult(
                extractedText = handler.toString().trim(),
                mimeType = mimeType,
                metadata = metaMap,
        )
    }
}
