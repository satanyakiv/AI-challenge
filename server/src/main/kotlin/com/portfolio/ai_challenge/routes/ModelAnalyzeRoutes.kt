package com.portfolio.ai_challenge.routes

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.models.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
private val analyzeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun Route.modelAnalyzeRoutes(httpClient: HttpClient, apiKey: String) {

    post("/api/models/analyze") {
        val request = call.receive<ModelCompareAnalyzeRequest>()
        val resultsText = request.results.joinToString("\n\n") { r ->
            "=== ${r.modelLabel} ===\nResponse time: ${r.responseTimeMs}ms\nTokens: ${r.totalTokens}\nCost: $${String.format("%.6f", r.estimatedCost)}\nContent: ${r.content}"
        }
        val analyzePrompt = Prompts.Day5.MODEL_ANALYZE
            .replace("{{results}}", resultsText)

        try {
            val response = httpClient.post(DEEPSEEK_API_URL) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(analyzeJson.encodeToString(DeepSeekRequest.serializer(),
                    DeepSeekRequest(
                        messages = listOf(DeepSeekMessage(role = MessageRole.USER, content = analyzePrompt)),
                        temperature = 0.0,
                    )
                ))
            }
            val rawBody = response.bodyAsText()
            val deepSeekResponse = analyzeJson.decodeFromString<DeepSeekResponse>(rawBody)
            val rawContent = deepSeekResponse.choices.firstOrNull()?.message?.content ?: ""
            val analysis = analyzeJson.decodeFromString<ModelCompareAnalysis>(rawContent)
            call.respond(analysis)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ModelCompareAnalysis(comparison = "Analysis failed: ${e.message}"),
            )
        }
    }
}
