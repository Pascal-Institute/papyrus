package papyrus.core.network

import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.network.SecApiClient
import com.pascal.institute.ahmes.network.SecApiConfig

/**
 * Papyrus wrapper for SEC API client.
 *
 * This object provides a convenient singleton instance of SecApiClient configured specifically for
 * the Papyrus application.
 */
object SecApi {
    private val client =
            SecApiClient(
                    SecApiConfig(
                            userAgent = "PapyrusApp/1.0",
                            contactEmail = "admin@example.com", // Replace with your actual email
                            rateLimitDelayMs = 100L
                    )
            )

    suspend fun loadTickers() = client.loadTickers()

    fun searchTicker(query: String): List<TickerEntry> = client.searchTicker(query)

    suspend fun getSubmissions(cik: Int): SubmissionsRoot? = client.getSubmissions(cik)

    suspend fun getCompanyFacts(cik: Int): CompanyFacts? = client.getCompanyFacts(cik)

    fun getDocumentUrl(cik: String, accessionNumber: String, primaryDocument: String): String =
            client.getDocumentUrl(cik, accessionNumber, primaryDocument)

    fun getDocumentUrlWithFormat(
            cik: String,
            accessionNumber: String,
            primaryDocument: String,
            fileExtension: String
    ): String =
            client.getDocumentUrlWithFormat(cik, accessionNumber, primaryDocument, fileExtension)

    fun transformFilings(recent: RecentFilings): List<FilingItem> = client.transformFilings(recent)

    suspend fun fetchDocumentContent(url: String): String = client.fetchDocumentContent(url)
}
