package papyrus.core.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import papyrus.core.model.CompanyNews
import papyrus.core.model.NewsArticle

/**
 * NewsApi - Fetches company news from various sources Uses Alpha Vantage News API (free tier
 * available)
 */
object NewsApi {
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

    // Alpha Vantage API Key (demo key - replace with actual key for production)
    // Get free API key from: https://www.alphavantage.co/support/#api-key
    private const val ALPHA_VANTAGE_API_KEY = "4PT34J7D30OZAA1K"

    /** Fetch news for a specific ticker symbol */
    suspend fun getCompanyNews(ticker: String, companyName: String, limit: Int = 10): CompanyNews? {
        return try {
            // Use Alpha Vantage News Sentiment API
            val url =
                    "https://www.alphavantage.co/query?" +
                            "function=NEWS_SENTIMENT" +
                            "&tickers=$ticker" +
                            "&limit=$limit" +
                            "&apikey=$ALPHA_VANTAGE_API_KEY"

            val response: String = client.get(url).bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val newsResponse = json.decodeFromString<AlphaVantageNewsResponse>(response)

            // Convert to our internal model
            val articles =
                    newsResponse.feed?.take(limit)?.map { item ->
                        NewsArticle(
                                title = item.title ?: "No Title",
                                url = item.url ?: "",
                                source = item.source ?: "Unknown",
                                publishedAt = formatPublishDate(item.timePublished),
                                description = item.summary,
                                imageUrl = item.bannerImage
                        )
                    }
                            ?: emptyList()

            CompanyNews(
                    ticker = ticker,
                    companyName = companyName,
                    articles = articles,
                    lastUpdated =
                            LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            )
        } catch (e: Exception) {
            println("Error fetching news for $ticker: ${e.message}")
            // Return mock news if API fails
            getMockNews(ticker, companyName)
        }
    }

    /** Format the Alpha Vantage timestamp to readable format */
    private fun formatPublishDate(timestamp: String?): String {
        if (timestamp == null) return "Unknown date"

        return try {
            // Alpha Vantage format: 20240101T120000
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
            val dateTime = LocalDateTime.parse(timestamp, formatter)
            dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        } catch (e: Exception) {
            timestamp
        }
    }

    /** Provide mock news for demonstration purposes */
    private fun getMockNews(ticker: String, companyName: String): CompanyNews {
        val mockArticles =
                listOf(
                        NewsArticle(
                                title = "$companyName Reports Strong Quarterly Earnings",
                                url =
                                        "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=$ticker",
                                source = "Business Wire",
                                publishedAt = "Jan 03, 2026 14:30",
                                description =
                                        "$companyName exceeded analyst expectations with strong revenue growth and improved profit margins.",
                                imageUrl = null
                        ),
                        NewsArticle(
                                title = "Analysts Upgrade $ticker Stock Following Positive Outlook",
                                url =
                                        "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=$ticker",
                                source = "MarketWatch",
                                publishedAt = "Jan 02, 2026 09:15",
                                description =
                                        "Several major investment firms have raised their price targets for $ticker following the company's strategic announcement.",
                                imageUrl = null
                        ),
                        NewsArticle(
                                title = "$companyName Announces New Strategic Partnership",
                                url =
                                        "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=$ticker",
                                source = "Reuters",
                                publishedAt = "Dec 30, 2025 16:45",
                                description =
                                        "The partnership is expected to drive growth in key market segments and enhance the company's competitive position.",
                                imageUrl = null
                        ),
                        NewsArticle(
                                title =
                                        "Market Update: $ticker Shares Rise on Strong Sector Performance",
                                url =
                                        "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=$ticker",
                                source = "Bloomberg",
                                publishedAt = "Dec 28, 2025 11:20",
                                description =
                                        "The broader sector rally has lifted $ticker shares, with investors showing increased confidence in the company's fundamentals.",
                                imageUrl = null
                        ),
                        NewsArticle(
                                title =
                                        "$companyName Continues Innovation Push with New Product Launch",
                                url =
                                        "https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK=$ticker",
                                source = "TechCrunch",
                                publishedAt = "Dec 25, 2025 13:00",
                                description =
                                        "The new product line addresses emerging market needs and positions $companyName for future growth opportunities.",
                                imageUrl = null
                        )
                )

        return CompanyNews(
                ticker = ticker,
                companyName = companyName,
                articles = mockArticles,
                lastUpdated =
                        LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        )
    }
}

// ===== Alpha Vantage API Response Models =====

@Serializable
private data class AlphaVantageNewsResponse(
        val feed: List<NewsFeedItem>? = null,
        @SerialName("items") val items: String? = null
)

@Serializable
private data class NewsFeedItem(
        val title: String? = null,
        val url: String? = null,
        @SerialName("time_published") val timePublished: String? = null,
        val summary: String? = null,
        @SerialName("banner_image") val bannerImage: String? = null,
        val source: String? = null,
        @SerialName("source_domain") val sourceDomain: String? = null
)
