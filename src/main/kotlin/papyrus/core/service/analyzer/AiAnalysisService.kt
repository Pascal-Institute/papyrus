package papyrus.core.service.analyzer

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import papyrus.core.model.AiAnalysisResult
import papyrus.core.model.FinancialAnalysis
import papyrus.core.model.FinancialMetric
import papyrus.core.model.FinancialRatio
import papyrus.util.SettingsManager

/** OpenRouter AI 분석 서비스 무료 LLM API를 사용하여 재무 데이터 분석 */
object AiAnalysisService {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()

    // Explicitly specify the generic type for Json to avoid ambiguity if needed,
    // or just use the standard import.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    // OpenRouter API 설정
    private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

    // 무료 모델 옵션 - OpenRouter의 최신 무료 모델 사용
    // 참고: 무료 모델은 변경될 수 있으므로 https://openrouter.ai/docs 에서 확인
    private const val FREE_MODEL = "meta-llama/llama-3.2-3b-instruct:free"

    // 대체 모델 옵션 (fallback)
    private val FALLBACK_MODELS =
            listOf(
                    "meta-llama/llama-3.2-3b-instruct:free",
                    "meta-llama/llama-3.1-8b-instruct:free",
                    "google/gemma-2-9b-it:free",
                    "microsoft/phi-3-mini-128k-instruct:free"
            )

    // API 키 (SettingsManager에서 가져오기)
    private fun getApiKey(): String? = SettingsManager.getApiKey()

    fun isConfigured(): Boolean = !getApiKey().isNullOrBlank()

    fun getConfigurationHelp(): List<String> {
        return listOf(
                "🤖 OpenRouter AI 분석 설정",
                "",
                "OpenRouter는 여러 AI 모델에 접근할 수 있는 통합 API입니다.",
                "무료 모델(Llama 3.1 8B)을 사용하여 재무 데이터를 분석합니다.",
                "",
                "📝 설정 방법:",
                "1. https://openrouter.ai 에서 무료 계정 생성",
                "2. API Keys 페이지에서 키 발급 (무료)",
                "3. 환경변수 설정: OPENROUTER_API_KEY=your_key",
                "4. 또는 실행 시: -Dopenrouter.api.key=your_key",
                "",
                "💰 비용:",
                "• 무료 모델 사용 (제한 없음)",
                "• 크레딧 구매 시 더 강력한 모델 사용 가능",
                "",
                "✨ 기능:",
                "• 재무 데이터 종합 분석",
                "• 투자 권장사항 제공",
                "• 위험 요소 평가",
                "• 산업 비교 분석"
        )
    }

    /** 재무 데이터를 AI로 분석 */
    suspend fun analyzeFinancialData(
            companyName: String,
            metrics: List<FinancialMetric>,
            ratios: List<FinancialRatio>
    ): AiAnalysisResult =
            withContext(Dispatchers.IO) {
                if (!isConfigured()) {
                    return@withContext AiAnalysisResult(
                            success = false,
                            provider = "OpenRouter",
                            model = FREE_MODEL,
                            summary = "🔑 AI 분석을 사용하려면 OpenRouter API 키가 필요합니다.",
                            keyInsights = getConfigurationHelp(),
                            recommendations = emptyList(),
                            riskAssessment = "위 방법에 따라 API 키를 설정한 후 다시 시도해주세요.",
                            confidence = 0.0
                    )
                }

                return@withContext try {
                    val prompt = buildAnalysisPrompt(companyName, metrics, ratios)
                    val response = callOpenRouterApi(prompt)
                    parseAiResponse(response)
                            .copy(success = true, provider = "OpenRouter", model = FREE_MODEL)
                } catch (e: Exception) {
                    AiAnalysisResult(
                            success = false,
                            provider = "OpenRouter",
                            model = FREE_MODEL,
                            summary = "⚠️ AI 분석 중 오류 발생: ${e.message}",
                            keyInsights =
                                    listOf(
                                            "네트워크 연결을 확인해주세요.",
                                            "API 키가 유효한지 확인해주세요.",
                                            "잠시 후 다시 시도해주세요."
                                    ),
                            recommendations = emptyList(),
                            riskAssessment = "분석 실패 - 수동 검토 필요",
                            confidence = 0.0
                    )
                }
            }

    /** 간단한 요약 생성 */
    suspend fun generateQuickSummary(
            companyName: String,
            documentType: String,
            content: String
    ): String =
            withContext(Dispatchers.IO) {
                if (!isConfigured()) {
                    return@withContext "AI 요약 기능을 사용하려면 OpenRouter API 키를 설정해주세요.\n무료 계정: https://openrouter.ai"
                }

                return@withContext try {
                    val prompt =
                            """
                다음 ${companyName}의 ${documentType} 문서를 한국어로 5-7문장으로 요약해주세요.
                투자자 관점에서 가장 중요한 정보를 포함해주세요.
                
                문서 내용 (처음 3000자):
                ${content.take(3000)}
                
                요약 (한국어로 일반 텍스트만):
            """.trimIndent()

                    callOpenRouterApi(prompt)
                } catch (e: Exception) {
                    "⚠️ 요약 생성 실패: ${e.message}"
                }
            }

    /** 재무 데이터 산업 비교 분석 */
    suspend fun compareWithIndustry(companyName: String, ratios: List<FinancialRatio>): String =
            withContext(Dispatchers.IO) {
                if (!isConfigured()) {
                    return@withContext "산업 비교 분석을 사용하려면 API 키가 필요합니다."
                }

                return@withContext try {
                    val ratioSummary =
                            ratios.joinToString("\n") {
                                "- ${it.name}: ${it.formattedValue} (${it.interpretation})"
                            }

                    val prompt =
                            """
                ${companyName}의 다음 재무 비율을 일반적인 산업 표준과 비교하여 분석해주세요.
                
                재무 비율:
                $ratioSummary
                
                다음 형식으로 답변해주세요:
                
                💪 강점 (2-3개):
                - [구체적인 강점 분석]
                
                ⚠️ 약점 (2-3개):
                - [구체적인 약점 분석]
                
                📊 산업 대비 평가:
                [전반적인 평가 2-3문장]
                
                💡 개선 제안:
                - [실행 가능한 개선 방안]
                
                한국어로 답변해주세요.
            """.trimIndent()

                    callOpenRouterApi(prompt)
                } catch (e: Exception) {
                    "⚠️ 비교 분석 실패: ${e.message}"
                }
            }

    /** 투자 의사결정 지원 분석 */
    suspend fun generateInvestmentAdvice(companyName: String, analysis: FinancialAnalysis): String =
            withContext(Dispatchers.IO) {
                if (!isConfigured()) {
                    return@withContext "투자 조언 기능을 사용하려면 API 키가 필요합니다."
                }

                return@withContext try {
                    val healthScore = analysis.healthScore
                    val ratiosText =
                            analysis.ratios.take(8).joinToString("\n") {
                                "- ${it.name}: ${it.formattedValue}"
                            }

                    val prompt =
                            """
                ${companyName}의 재무 분석 결과를 바탕으로 투자 의사결정을 도와주세요.
                
                재무 건강 점수: ${healthScore?.overallScore}/100
                등급: ${healthScore?.grade}
                
                주요 비율:
                $ratiosText
                
                강점:
                ${healthScore?.strengths?.joinToString("\n") { "- $it" } ?: "없음"}
                
                약점:
                ${healthScore?.weaknesses?.joinToString("\n") { "- $it" } ?: "없음"}
                
                다음 관점에서 분석해주세요:
                
                📈 투자 매력도 평가:
                [점수 기반 종합 평가]
                
                🎯 투자 전략 제안:
                - 단기 투자자:
                - 장기 투자자:
                
                ⚖️ 리스크 vs 수익 분석:
                [균형잡힌 평가]
                
                🔍 주의할 점:
                [주요 위험 요소]
                
                한국어로 답변해주세요.
            """.trimIndent()

                    callOpenRouterApi(prompt)
                } catch (e: Exception) {
                    "⚠️ 투자 조언 생성 실패: ${e.message}"
                }
            }

    private fun buildAnalysisPrompt(
            companyName: String,
            metrics: List<FinancialMetric>,
            ratios: List<FinancialRatio>
    ): String {
        val metricsText = metrics.take(12).joinToString("\n") { "- ${it.name}: ${it.value}" }

        val ratiosText =
                ratios.take(10).joinToString("\n") {
                    "- ${it.name}: ${it.formattedValue} (상태: ${it.healthStatus})"
                }

        return """
            당신은 전문 재무 분석가입니다. ${companyName}의 재무 데이터를 종합적으로 분석하고 평가해주세요.
            
            주요 재무 지표:
            $metricsText
            
            재무 비율:
            $ratiosText
            
            다음 항목을 JSON 형식으로 정확하게 답변해주세요:
            {
                "summary": "전체 재무 상태 요약 (3-4문장, 구체적 수치 포함, 한국어)",
                "keyInsights": [
                    "핵심 인사이트 1 (구체적 수치와 의미 포함)",
                    "핵심 인사이트 2 (비교 분석 포함)",
                    "핵심 인사이트 3 (트렌드 분석)"
                ],
                "recommendations": [
                    "투자자를 위한 실행 가능한 권장사항 1",
                    "투자자를 위한 실행 가능한 권장사항 2",
                    "투자자를 위한 실행 가능한 권장사항 3"
                ],
                "riskAssessment": "주요 위험 요소와 주의사항 (2-3문장, 구체적)",
                "confidence": 0.85
            }
            
            반드시 한국어로 답변하고, JSON 형식만 출력하세요 (다른 텍스트나 마크다운 없이).
        """.trimIndent()
    }

    private fun callOpenRouterApi(prompt: String): String {
        val currentApiKey = getApiKey() ?: throw Exception("API 키가 설정되지 않았습니다")

        // 먼저 기본 모델 시도, 실패 시 대체 모델들 순차 시도
        var lastError: Exception? = null

        for (model in FALLBACK_MODELS) {
            try {
                return callOpenRouterApiWithModel(currentApiKey, prompt, model)
            } catch (e: Exception) {
                lastError = e
                // 404 또는 모델 찾을 수 없음 오류인 경우 다음 모델 시도
                if (e.message?.contains("404") == true ||
                                e.message?.contains("no endpoints") == true ||
                                e.message?.contains("not found") == true
                ) {
                    continue
                }
                // 다른 오류는 바로 throw
                throw e
            }
        }

        // 모든 모델 실패 시
        throw lastError ?: Exception("사용 가능한 AI 모델을 찾을 수 없습니다")
    }

    private fun callOpenRouterApiWithModel(apiKey: String, prompt: String, model: String): String {
        // Need to explicitly specify serializer for string due to prompt being a string
        val contentJson = json.encodeToString(prompt)

        val requestBody =
                """
            {
                "model": "$model",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a professional financial analyst. Provide accurate, data-driven analysis in Korean."
                    },
                    {
                        "role": "user",
                        "content": $contentJson
                    }
                ],
                "temperature": 0.3,
                "max_tokens": 2000,
                "top_p": 1.0
            }
        """.trimIndent()

        val request =
                HttpRequest.newBuilder()
                        .uri(URI.create(OPENROUTER_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer $apiKey")
                        .header("HTTP-Referer", "https://github.com/Pascal-Institute/papyrus")
                        .header("X-Title", "Papyrus SEC Financial Analyzer")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofMinutes(2))
                        .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw Exception("OpenRouter API 오류 (${response.statusCode()}): ${response.body()}")
        }

        val jsonResponse = json.decodeFromString<OpenRouterResponse>(response.body())
        return jsonResponse.choices.firstOrNull()?.message?.content
                ?: throw Exception("응답에서 내용을 찾을 수 없습니다")
    }

    private fun parseAiResponse(response: String): AiAnalysisResult {
        return try {
            // JSON 부분만 추출 (마크다운 코드 블록 제거)
            var jsonString = response.trim()
            if (jsonString.startsWith("```")) {
                val lines = jsonString.lines()
                jsonString = lines.drop(1).dropLast(1).joinToString("\n")
            }

            val jsonStart = jsonString.indexOf("{")
            val jsonEnd = jsonString.lastIndexOf("}") + 1

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonString = jsonString.substring(jsonStart, jsonEnd)
            }

            json.decodeFromString<AiAnalysisResult>(jsonString)
                    .copy(success = true, provider = "OpenRouter", model = FREE_MODEL)
        } catch (e: Exception) {
            // JSON 파싱 실패 시 텍스트로 처리
            AiAnalysisResult(
                    success = true,
                    provider = "OpenRouter",
                    model = FREE_MODEL,
                    summary = response.take(500).trim(),
                    keyInsights =
                            listOf("AI 분석이 완료되었으나 구조화하지 못했습니다.", "응답 내용: ${response.take(200)}"),
                    recommendations = listOf("상세 요약을 참고해주세요."),
                    riskAssessment = "상세 내용을 수동으로 검토해주세요.",
                    confidence = 0.6
            )
        }
    }

    /** API 키 테스트 - 간단한 요청으로 키의 유효성 검사 */
    suspend fun testApiKey(testKey: String): Pair<Boolean, String?> =
            withContext(Dispatchers.IO) {
                // 여러 모델 시도
                for (model in FALLBACK_MODELS) {
                    try {
                        val requestBody =
                                """
                    {
                        "model": "$model",
                        "messages": [
                            {
                                "role": "user",
                                "content": "Say 'OK' if you can understand this message."
                            }
                        ],
                        "max_tokens": 10
                    }
                """.trimIndent()

                        val request =
                                HttpRequest.newBuilder()
                                        .uri(URI.create(OPENROUTER_API_URL))
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", "Bearer $testKey")
                                        .header(
                                                "HTTP-Referer",
                                                "https://github.com/Pascal-Institute/papyrus"
                                        )
                                        .header("X-Title", "Papyrus SEC Financial Analyzer")
                                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                        .timeout(Duration.ofSeconds(30))
                                        .build()

                        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                        if (response.statusCode() == 200) {
                            return@withContext Pair(
                                    true,
                                    "API 키가 정상적으로 작동합니다! (모델: ${model.split("/").last()})"
                            )
                        } else if (response.statusCode() == 404) {
                            // 모델을 찾을 수 없으면 다음 모델 시도
                            continue
                        } else {
                            val errorMsg =
                                    when (response.statusCode()) {
                                        401 -> "유효하지 않은 API 키입니다."
                                        402 -> "크레딧이 부족합니다. OpenRouter에서 크레딧을 충전해주세요."
                                        429 -> "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."
                                        else ->
                                                "API 오류 (${response.statusCode()}): ${response.body().take(100)}"
                                    }
                            return@withContext Pair(false, errorMsg)
                        }
                    } catch (e: Exception) {
                        // 404 오류면 다음 모델 시도
                        if (e.message?.contains("404") == true) {
                            continue
                        }
                        return@withContext Pair(false, "연결 실패: ${e.message}")
                    }
                }

                // 모든 모델 실패
                return@withContext Pair(
                        false,
                        "사용 가능한 AI 모델을 찾을 수 없습니다. OpenRouter 무료 모델 상태를 확인해주세요."
                )
            }
}

// Private DTOs for OpenRouter API
@kotlinx.serialization.Serializable
private data class OpenRouterResponse(val choices: List<OpenRouterChoice>)

@kotlinx.serialization.Serializable
private data class OpenRouterChoice(val message: OpenRouterMessage)

@kotlinx.serialization.Serializable private data class OpenRouterMessage(val content: String)
