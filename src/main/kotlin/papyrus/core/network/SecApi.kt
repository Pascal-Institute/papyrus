package papyrus.core.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import papyrus.core.model.*

object SecApi {
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

    private val mapper =
            jacksonObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private var tickers: List<TickerEntry> = emptyList()
    private val mutex = Mutex()
    // SEC requires a descriptive User-Agent with contact info
    // Using a generic browser User-Agent for better compatibility
    private const val USER_AGENT = "PapyrusApp/1.0"
    private const val CONTACT_EMAIL = "admin@example.com" // Replace with your actual email

    suspend fun loadTickers() {
        if (tickers.isNotEmpty()) return

        mutex.withLock {
            if (tickers.isNotEmpty()) return@withLock
            try {
                // Add a small delay to respect SEC rate limits
                kotlinx.coroutines.delay(100)

                // company_tickers.json returns a Map<String, TickerEntry> where keys are "0", "1",
                // etc.
                val response: Map<String, TickerEntry> =
                        client
                                .get("https://www.sec.gov/files/company_tickers.json") {
                                    header("User-Agent", "$USER_AGENT ($CONTACT_EMAIL)")
                                    header("Accept", "*/*")
                                    header("Host", "www.sec.gov")
                                }
                                .body()
                tickers = response.values.toList()
                println("Loaded ${tickers.size} tickers")
            } catch (e: Exception) {
                System.err.println("Failed to load tickers: ${e.message}")
                e.printStackTrace()
                // Set empty list so app can continue
                tickers = emptyList()
            }
        }
    }

    fun searchTicker(query: String): List<TickerEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.uppercase()
        return tickers
                .filter { it.ticker.startsWith(q) || it.title.uppercase().contains(q) }
                .sortedBy { it.ticker.length }
                .take(20)
    }

    suspend fun getSubmissions(cik: Int): SubmissionsRoot? {
        // CIK must be 10 digits, zero padded
        val cikStr = cik.toString().padStart(10, '0')
        val url = "https://data.sec.gov/submissions/CIK$cikStr.json"
        println("Fetching submissions from: $url")
        return try {
            kotlinx.coroutines.delay(100) // Respect rate limits
            client
                    .get(url) {
                        header("User-Agent", "$USER_AGENT ($CONTACT_EMAIL)")
                        header("Accept", "*/*")
                        header("Host", "data.sec.gov")
                    }
                    .body()
        } catch (e: Exception) {
            System.err.println("Failed to fetch submissions: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch SEC XBRL company facts (companyfacts JSON).
     * Docs: https://data.sec.gov/api/xbrl/companyfacts/CIK##########.json
     */
    suspend fun getCompanyFacts(cik: Int): CompanyFacts? {
        val cikStr = cik.toString().padStart(10, '0')
        val url = "https://data.sec.gov/api/xbrl/companyfacts/CIK$cikStr.json"
        return try {
            kotlinx.coroutines.delay(100) // Respect rate limits
            val jsonText: String =
                    client
                            .get(url) {
                                header("User-Agent", "$USER_AGENT ($CONTACT_EMAIL)")
                                header("Accept", "*/*")
                                header("Host", "data.sec.gov")
                            }
                            .bodyAsText()

            mapper.readValue<CompanyFacts>(jsonText)
        } catch (e: Exception) {
            System.err.println("Failed to fetch company facts: ${e.message}")
            null
        }
    }

    fun getDocumentUrl(cik: String, accessionNumber: String, primaryDocument: String): String {
        val acc = accessionNumber.replace("-", "")
        // Use safe casting or cleaning
        val cikInt = cik.trimStart('0')
        return "https://www.sec.gov/Archives/edgar/data/$cikInt/$acc/$primaryDocument"
    }

    /**
     * Get document URL with specific file format SEC filings are typically available in multiple
     * formats:
     * - HTML: primary document (e.g., 0001628280-23-019764-index.htm)
     * - TXT: Complete submission text file (e.g., 0001628280-23-019764.txt)
     * - PDF: Not always available directly, but can be rendered
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
            "txt" -> {
                // The complete submission text file uses the accession number
                "$baseUrl/$accessionNumber.txt"
            }
            "pdf" -> {
                // PDF rendering service (may not always work)
                // Alternative: Use SEC's rendering service
                "https://www.sec.gov/cgi-bin/viewer?action=view&cik=$cikInt&accession_number=$accessionNumber&xbrl_type=v"
            }
            "html", "htm" -> {
                // Use the primary document (usually HTML)
                "$baseUrl/$primaryDocument"
            }
            else -> {
                // Default to primary document
                "$baseUrl/$primaryDocument"
            }
        }
    }

    // Helper to transform parallel lists into objects
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

    suspend fun fetchDocumentContent(url: String): String {
        return try {
            kotlinx.coroutines.delay(100) // Respect rate limits
            client
                    .get(url) {
                        header("User-Agent", "$USER_AGENT ($CONTACT_EMAIL)")
                        header("Accept", "*/*")
                    }
                    .body()
        } catch (e: Exception) {
            "Error loading content: ${e.message}"
        }
    }
}
