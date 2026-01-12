package papyrus.core.service

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object MarketDataService {

    private val json = Json { ignoreUnknownKeys = true }

    // Minimal models for Yahoo Chart response
    @Serializable data class YahooChartResponse(val chart: ChartData?)
    @Serializable data class ChartData(val result: List<ChartResult>?, val error: ChartError?)
    @Serializable data class ChartResult(val meta: ChartMeta)
    @Serializable
    data class ChartMeta(
            val regularMarketPrice: Double? = null,
            val currency: String? = null,
            val symbol: String? = null,
            val instrumentType: String? = null
    )
    @Serializable data class ChartError(val code: String, val description: String)

    data class StockQuote(val price: Double, val currency: String)

    suspend fun getStockPrice(ticker: String): StockQuote? =
            withContext(Dispatchers.IO) {
                try {
                    // Using Yahoo Finance Chart API which is often more open than Quote API
                    val urlString =
                            "https://query1.finance.yahoo.com/v8/finance/chart/$ticker?interval=1d&range=1d"
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    // User-Agent is required to avoid 403
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == 200) {
                        val content = connection.inputStream.bufferedReader().use { it.readText() }
                        // Basic cleanup if needed, but JSON parser should handle it
                        val response = json.decodeFromString<YahooChartResponse>(content)
                        val meta = response.chart?.result?.firstOrNull()?.meta

                        if (meta?.regularMarketPrice != null) {
                            StockQuote(meta.regularMarketPrice, meta.currency ?: "USD")
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Silently fail for market data fetch
                    null
                }
            }
}
