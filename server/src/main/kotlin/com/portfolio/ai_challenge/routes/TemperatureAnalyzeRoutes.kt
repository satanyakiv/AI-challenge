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

fun Route.temperatureAnalyzeRoutes(httpClient: HttpClient, apiKey: String) {

    post("/api/temperature/analyze") {
        val request = call.receive<AnalyzeRequest>()
        val resultsText = request.results.joinToString("\n\n") { result ->
            "=== Temperature ${result.temperature} ===\n${result.content}"
        }
        val analyzePrompt = Prompts.Day3.TEMPERATURE_ANALYZE
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
            val analyzeResponse = analyzeJson.decodeFromString<AnalyzeResponse>(rawContent)
            call.respond(analyzeResponse)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                AnalyzeResponse(
                    comparison = "Analysis failed: ${e.message}",
                    recommendations = emptyList(),
                ),
            )
        }
    }
}
