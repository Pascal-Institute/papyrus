package papyrus

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

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

    private var tickers: List<TickerEntry> = emptyList()
    private val mutex = Mutex()
    // User Agent MUST include contact info per SEC Fair Access Policy
    // For production, use your real email: "CompanyName/1.0 (contact@yourdomain.com)"
    private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    suspend fun loadTickers() {
        if (tickers.isNotEmpty()) return

        mutex.withLock {
            if (tickers.isNotEmpty()) return@withLock
            try {
                // company_tickers.json returns a Map<String, TickerEntry> where keys are "0", "1",
                // etc.
                val response: Map<String, TickerEntry> =
                        client
                                .get("https://www.sec.gov/files/company_tickers.json") {
                                    header("User-Agent", USER_AGENT)
                                    header("Accept", "application/json, */*")
                                    header("Accept-Encoding", "gzip, deflate")
                                    header("Connection", "keep-alive")
                                }
                                .body()
                tickers = response.values.toList()
                println("Loaded ${tickers.size} tickers")
            } catch (e: Exception) {
                System.err.println("Failed to load tickers: ${e.message}")
                e.printStackTrace()
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
            client
                    .get(url) {
                        header("User-Agent", USER_AGENT)
                        header("Accept-Encoding", "gzip, deflate")
                    }
                    .body()
        } catch (e: Exception) {
            System.err.println("Failed to fetch submissions: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun getDocumentUrl(cik: String, accessionNumber: String, primaryDocument: String): String {
        val acc = accessionNumber.replace("-", "")
        // Use safe casting or cleaning
        val cikInt = cik.trimStart('0')
        return "https://www.sec.gov/Archives/edgar/data/$cikInt/$acc/$primaryDocument"
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
            client.get(url) { header("User-Agent", USER_AGENT) }.body()
        } catch (e: Exception) {
            "Error loading content: ${e.message}"
        }
    }
}
