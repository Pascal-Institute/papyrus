package com.pascal.institute.ahmes.network

import com.pascal.institute.ahmes.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Configuration for SEC API client.
 *
 * @param userAgent User agent string (SEC requires descriptive user agent with contact info)
 * @param contactEmail Contact email for SEC API
 * @param rateLimitDelayMs Delay between requests to respect SEC rate limits (default 100ms)
 */
data class SecApiConfig(
        val userAgent: String,
        val contactEmail: String,
        val rateLimitDelayMs: Long = 100L
)

/**
 * Generic SEC API client for data.sec.gov and www.sec.gov APIs.
 *
 * Provides access to:
 * - Company tickers
 * - Submissions and filings
 * - Company facts (XBRL data)
 * - Document content
 *
 * This client is designed to be reusable across different applications.
 *
 * @param config Configuration for the client
 */
class SecApiClient(private val config: SecApiConfig) {

    private val client =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            }
                    )
                }
            }

    private var tickers: List<TickerEntry> = emptyList()
    private val tickerMutex = Mutex()

    /** Configure request with SEC API headers. */
    private fun HttpRequestBuilder.applySecHeaders(hostOverride: String? = null) {
        header("User-Agent", "${config.userAgent} (${config.contactEmail})")
        header("Accept", "*/*")
        hostOverride?.let { header("Host", it) }
    }

    /** Load all company tickers from SEC. Results are cached after first load. */
    suspend fun loadTickers() {
        if (tickers.isNotEmpty()) return

        tickerMutex.withLock {
            if (tickers.isNotEmpty()) return@withLock
            try {
                kotlinx.coroutines.delay(config.rateLimitDelayMs)

                // company_tickers.json returns a Map<String, TickerEntry> where keys are "0", "1",
                // etc.
                val response: Map<String, TickerEntry> =
                        client
                                .get("https://www.sec.gov/files/company_tickers.json") {
                                    applySecHeaders()
                                }
                                .body()
                tickers = response.values.toList()
            } catch (e: Exception) {
                System.err.println("Failed to load tickers: ${e.message}")
                e.printStackTrace()
                tickers = emptyList()
            }
        }
    }

    /**
     * Search for tickers by query string. Searches both ticker symbol and company title.
     *
     * @param query Search query (case-insensitive)
     * @param limit Maximum number of results to return (default 20)
     * @return List of matching ticker entries
     */
    fun searchTicker(query: String, limit: Int = 20): List<TickerEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.uppercase()
        return tickers
                .filter { it.ticker.startsWith(q) || it.title.uppercase().contains(q) }
                .sortedBy { it.ticker.length }
                .take(limit)
    }

    /**
     * Get submissions for a company by CIK.
     *
     * @param cik Central Index Key (CIK) number
     * @return Submissions data or null if not found/error
     */
    suspend fun getSubmissions(cik: Int): SubmissionsRoot? {
        val cikStr = cik.toString().padStart(10, '0')
        val url = "https://data.sec.gov/submissions/CIK$cikStr.json"
        return try {
            kotlinx.coroutines.delay(config.rateLimitDelayMs)
            client.get(url) { applySecHeaders() }.body()
        } catch (e: Exception) {
            System.err.println("Failed to fetch submissions: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch SEC XBRL company facts.
     *
     * @param cik Central Index Key (CIK) number
     * @return Company facts data or null if not found/error
     */
    suspend fun getCompanyFacts(cik: Int): CompanyFacts? {
        val cikStr = cik.toString().padStart(10, '0')
        val url = "https://data.sec.gov/api/xbrl/companyfacts/CIK$cikStr.json"
        return try {
            kotlinx.coroutines.delay(config.rateLimitDelayMs)
            client.get(url) { applySecHeaders() }.body()
        } catch (e: Exception) {
            System.err.println("Failed to fetch company facts: ${e.message}")
            null
        }
    }

    /**
     * Generate document URL for SEC EDGAR archives.
     *
     * @param cik Central Index Key as string
     * @param accessionNumber Filing accession number
     * @param primaryDocument Primary document filename
     * @return Full URL to the document
     */
    fun getDocumentUrl(cik: String, accessionNumber: String, primaryDocument: String): String {
        val acc = accessionNumber.replace("-", "")
        val cikInt = cik.trimStart('0')
        return "https://www.sec.gov/Archives/edgar/data/$cikInt/$acc/$primaryDocument"
    }

    /**
     * Generate document URL with specific file format.
     *
     * SEC filings are typically available in multiple formats:
     * - HTML: primary document (e.g., 0001628280-23-019764-index.htm)
     * - TXT: Complete submission text file (e.g., 0001628280-23-019764.txt)
     *
     * @param cik Central Index Key as string
     * @param accessionNumber Filing accession number
     * @param primaryDocument Primary document filename
     * @param fileExtension Desired file extension (txt, html, htm)
     * @return Full URL to the document
     */
    fun getDocumentUrlWithFormat(
            cik: String,
            accessionNumber: String,
            primaryDocument: String,
            fileExtension: String
    ): String {
        val acc = accessionNumber.replace("-", "")
        val cikInt = cik.trimStart('0')
        val baseUrl = "https://www.sec.gov/Archives/edgar/data/$cikInt/$acc"

        return when (fileExtension.lowercase()) {
            "txt" -> "$baseUrl/$accessionNumber.txt"
            "html", "htm" -> "$baseUrl/$primaryDocument"
            else -> "$baseUrl/$primaryDocument"
        }
    }

    /**
     * Transform recent filings from parallel lists to objects.
     *
     * @param recent Recent filings data
     * @return List of filing items
     */
    fun transformFilings(recent: RecentFilings): List<FilingItem> {
        val list = mutableListOf<FilingItem>()
        val size = recent.accessionNumber.size
        for (i in 0 until size) {
            list.add(
                    FilingItem(
                            accessionNumber = recent.accessionNumber.getOrElse(i) { "" },
                            filingDate = recent.filingDate.getOrElse(i) { "" },
                            form = recent.form.getOrElse(i) { "" },
                            primaryDocument = recent.primaryDocument.getOrElse(i) { "" },
                            description = recent.primaryDocumentDescription?.getOrElse(i) { "" }
                                            ?: ""
                    )
            )
        }
        return list
    }

    /**
     * Fetch document content from URL.
     *
     * @param url Full URL to the document
     * @return Document content as string, or error message if fetch fails
     */
    suspend fun fetchDocumentContent(url: String): String {
        return try {
            if (url.isBlank()) {
                return "Error: Invalid URL (empty)"
            }

            if (url.contains("cgi-bin/viewer")) {
                return "Error: SEC's cgi-bin/viewer service is no longer available. Please use HTML or TXT format instead."
            }

            kotlinx.coroutines.delay(config.rateLimitDelayMs)
            val response = client.get(url) { applySecHeaders() }

            if (response.status.value in 200..299) {
                response.body()
            } else {
                val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                System.err.println("Failed to fetch document: $errorMsg")
                "Error loading content: $errorMsg\n\nURL: $url\n\nPlease try a different format (HTML or TXT)."
            }
        } catch (e: Exception) {
            System.err.println("Exception fetching document: ${e.message}")
            e.printStackTrace()
            "Error loading content: ${e.message}\n\nURL: $url\n\nThis may be due to:\n- Invalid document URL\n- Network connectivity issues\n- SEC server temporarily unavailable"
        }
    }

    /** Close the HTTP client and release resources. */
    fun close() {
        client.close()
    }
}
