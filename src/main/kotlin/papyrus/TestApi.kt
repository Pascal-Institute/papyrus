package papyrus

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val client =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                                prettyPrint = true
                            }
                    )
                }
            }

    try {
        println("Testing SEC API...")
        val response: HttpResponse =
                client.get("https://www.sec.gov/files/company_tickers.json") {
                    header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    )
                    header("Accept", "application/json")
                }

        println("Status: ${response.status}")
        println("Content-Type: ${response.headers["Content-Type"]}")

        val text: String = response.body()
        println("Response (first 500 chars):")
        println(text.take(500))

        // Try to parse as Map
        try {
            val parsed: Map<String, TickerEntry> =
                    Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            }
                            .decodeFromString(text)
            println("\nSuccessfully parsed ${parsed.size} entries")
            println("First entry: ${parsed.values.first()}")
        } catch (e: Exception) {
            println("\nFailed to parse as Map: ${e.message}")
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
