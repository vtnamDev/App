package com.example.core.ai

import com.example.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val groundingMetadata: GroundingMetadata? = null
)

@Serializable
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GroundingChunk>? = null
)

@Serializable
data class GroundingChunk(
    val web: WebSource? = null
)

@Serializable
data class WebSource(
    val uri: String? = null,
    val title: String? = null
)

enum class AiAdvisorMode(
    val title: String,
    val modelId: String,
    val subtitle: String,
    val usesHighThinking: Boolean,
    val usesGoogleSearch: Boolean
) {
    DEEP_KERNEL_THINKING(
        title = "High-Thinking Architect",
        modelId = "gemini-3.1-pro-preview",
        subtitle = "gemini-3.1-pro-preview • ThinkingLevel.HIGH • Deep SoC & thermal bottleneck analysis",
        usesHighThinking = true,
        usesGoogleSearch = false
    ),
    SEARCH_GROUNDED_INTEL(
        title = "Search-Grounded Intel",
        modelId = "gemini-3.5-flash",
        subtitle = "gemini-3.5-flash • Google Search Grounding • Live OEM patches & game frame-pacing data",
        usesHighThinking = false,
        usesGoogleSearch = true
    ),
    FAST_TELEMETRY_TRIAGE(
        title = "Fast Telemetry Triage",
        modelId = "gemini-3.1-flash-lite-preview",
        subtitle = "gemini-3.1-flash-lite-preview • Instant low-latency frame-time & thermal diagnosis",
        usesHighThinking = false,
        usesGoogleSearch = false
    )
}

@Serializable
data class ChatTurn(
    val id: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    val text: String,
    val modeBadge: String,
    val sources: List<Pair<String, String>> = emptyList()
)

interface GeminiRestApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiAdvisorService {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        private val SYSTEM_PROMPT = """
            You are the NEXUS BOOST Systems Performance & Kernel Engineering Advisor.
            Strict Engineering Rules:
            1. Never recommend fake sysfs properties (such as debug.sf.hw or debug.performance.tuning), arbitrary voltage modifications, or disabling thermal protection (such as stopping thermal-engine).
            2. Ground every recommendation in the user's actual DeviceCapabilityReport, Shizuku privilege state (NORMAL vs SHIZUKU_SHELL UID 2000 vs ROOT UID 0), and live frame-time percentile telemetry (P50/P90/P95/P99).
            3. Clearly distinguish between optimizations available right now on their current privilege tier and those requiring Shizuku or Root.
            4. Keep explanations concise, scannable, and technically rigorous.
        """.trimIndent()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiRestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiRestApi::class.java)
    }

    private fun resolveGeminiApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            field.isAccessible = true
            (field.get(null) as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    suspend fun sendMultiTurnMessage(
        history: List<ChatTurn>,
        userPrompt: String,
        hardwareContextSummary: String,
        mode: AiAdvisorMode
    ): ChatTurn = withContext(Dispatchers.IO) {
        val apiKey = resolveGeminiApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ChatTurn(
                isUser = false,
                text = "API Key Configuration Required: Please set your `GEMINI_API_KEY` in the **Secrets panel in AI Studio** to enable live ${mode.modelId} analysis.\n\n" +
                    "**Local Deterministic Hardware Summary:**\n$hardwareContextSummary",
                modeBadge = "${mode.title} (Offline Fallback)"
            )
        }

        val conversationContents = mutableListOf<Content>()
        // Include recent multi-turn context (up to 8 turns)
        history.takeLast(8).forEach { turn ->
            conversationContents.add(
                Content(
                    role = if (turn.isUser) "user" else "model",
                    parts = listOf(Part(text = turn.text))
                )
            )
        }
        // Append current user prompt enriched with live hardware & telemetry snapshot
        val enrichedPrompt = buildString {
            appendLine("[LIVE HARDWARE & TELEMETRY CONTEXT]")
            appendLine(hardwareContextSummary)
            appendLine()
            appendLine("[USER QUERY]")
            appendLine(userPrompt)
        }
        conversationContents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = enrichedPrompt))
            )
        )

        // Note: When using ThinkingLevel.HIGH with gemini-3.1-pro-preview, do NOT set maxOutputTokens.
        val generationConfig = if (mode.usesHighThinking) {
            GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
            )
        } else {
            GenerationConfig(
                temperature = 0.4f
            )
        }

        val tools = if (mode.usesGoogleSearch) {
            listOf(
                buildJsonObject {
                    putJsonObject("googleSearch") {}
                }
            )
        } else {
            null
        }

        val request = GenerateContentRequest(
            contents = conversationContents,
            generationConfig = generationConfig,
            tools = tools,
            systemInstruction = Content(
                parts = listOf(Part(text = SYSTEM_PROMPT))
            )
        )

        try {
            val response = api.generateContent(
                model = mode.modelId,
                apiKey = apiKey,
                request = request
            )
            val candidate = response.candidates?.firstOrNull()
            val textParts = candidate?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?.trim()
                .orEmpty()

            val citations = candidate?.groundingMetadata?.groundingChunks
                ?.mapNotNull { chunk ->
                    val uri = chunk.web?.uri
                    val title = chunk.web?.title ?: uri
                    if (!uri.isNullOrBlank() && !title.isNullOrBlank()) title to uri else null
                }
                ?.distinct()
                ?: emptyList()

            ChatTurn(
                isUser = false,
                text = textParts.ifBlank { "Completed analysis with no additional text output." },
                modeBadge = "${mode.title} (${mode.modelId})",
                sources = citations
            )
        } catch (t: Throwable) {
            ChatTurn(
                isUser = false,
                text = "Gemini API request failed (${t.javaClass.simpleName}): ${t.message}\n\n" +
                    "Verify your `GEMINI_API_KEY` in the AI Studio Secrets panel and network connectivity.",
                modeBadge = "${mode.title} (Error)"
            )
        }
    }
}
