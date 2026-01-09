package papyrus.util.data

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import papyrus.core.model.*

/** Cache for storing and retrieving financial analysis results to avoid redundant calculations */
object AnalysisCache {
    private val cacheDir = File(System.getProperty("user.home"), ".papyrus/cache/v3")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        cacheDir.mkdirs()
    }

    /** Generate cache key from document content */
    private fun getCacheKey(documentContent: String): String {
        // Use content hash as cache key
        return documentContent.hashCode().toString()
    }

    /** Get cached file for document */
    private fun getCacheFile(documentContent: String): File {
        val key = getCacheKey(documentContent)
        return File(cacheDir, "$key.json")
    }

    /** Save analysis result to cache */
    fun saveAnalysis(documentContent: String, analysis: FinancialAnalysis) {
        try {
            val cacheFile = getCacheFile(documentContent)
            val cacheData =
                    CachedAnalysis(
                            documentHash = getCacheKey(documentContent),
                            timestamp = System.currentTimeMillis(),
                            analysis = analysis
                    )
            val jsonString = json.encodeToString(cacheData)
            cacheFile.writeText(jsonString)
            println("Analysis cached successfully")
        } catch (e: Exception) {
            println("Failed to cache analysis: ${e.message}")
            e.printStackTrace()
        }
    }

    /** Load analysis result from cache */
    fun loadAnalysis(documentContent: String): FinancialAnalysis? {
        return try {
            val cacheFile = getCacheFile(documentContent)
            if (!cacheFile.exists()) {
                return null
            }

            // Check if cache is too old (7 days)
            val cacheAge = System.currentTimeMillis() - cacheFile.lastModified()
            if (cacheAge > 7 * 24 * 60 * 60 * 1000) {
                cacheFile.delete()
                return null
            }

            val jsonString = cacheFile.readText()
            val cacheData = json.decodeFromString<CachedAnalysis>(jsonString)
            println("Analysis loaded from cache")
            cacheData.analysis
        } catch (e: Exception) {
            println("Failed to load cached analysis: ${e.message}")
            null
        }
    }

    /** Check if analysis exists in cache */
    fun hasAnalysis(documentContent: String): Boolean {
        val cacheFile = getCacheFile(documentContent)
        return cacheFile.exists()
    }

    /** Clear all cached analyses */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            println("Cache cleared successfully")
        } catch (e: Exception) {
            println("Failed to clear cache: ${e.message}")
        }
    }

    /** Clear old cache entries (older than 7 days) */
    fun clearOldCache() {
        try {
            val now = System.currentTimeMillis()
            val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days

            cacheDir.listFiles()?.forEach { file ->
                val age = now - file.lastModified()
                if (age > maxAge) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            println("Failed to clear old cache: ${e.message}")
        }
    }
}

@Serializable
private data class CachedAnalysis(
        val documentHash: String,
        val timestamp: Long,
        val analysis: FinancialAnalysis
)
