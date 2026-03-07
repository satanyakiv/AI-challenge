package com.portfolio.ai_challenge.routes

import com.portfolio.ai_challenge.models.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun Route.temperatureRoutes(httpClient: HttpClient, apiKey: String) {

    post("/api/temperature/stream") {
        val request = call.receive<TemperatureRequest>()

        call.response.header(HttpHeaders.ContentType, "text/event-stream")
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.response.header(HttpHeaders.Connection, "keep-alive")

        call.respondBytesWriter {
            try {
                httpClient.preparePost(DEEPSEEK_API_URL) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(apiKey)
                    setBody(json.encodeToString(DeepSeekRequest.serializer(),
                        DeepSeekRequest(
                            messages = listOf(DeepSeekMessage(role = MessageRole.USER, content = request.prompt)),
                            temperature = request.temperature,
                            stream = true,
                        )
                    ))
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        val errorBody = response.bodyAsText()
                        writeStringUtf8("data: {\"error\":\"DeepSeek API error: ${response.status} - $errorBody\"}\n\n")
                        flush()
                        return@execute
                    }

                    val channel = response.bodyAsChannel()
                    val buffer = StringBuilder()

                    while (!channel.isClosedForRead) {
                        val line = try {
                            channel.readLine()
                        } catch (e: Exception) {
                            null
                        } ?: break

                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                writeStringUtf8("data: [DONE]\n\n")
                                flush()
                                break
                            }
                            try {
                                val chunk = json.decodeFromString<DeepSeekStreamChunk>(data)
                                val content = chunk.choices.firstOrNull()?.delta?.content
                                if (content != null) {
                                    val escaped = content
                                        .replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                    writeStringUtf8("data: {\"content\":\"$escaped\"}\n\n")
                                    flush()
                                }
                            } catch (_: Exception) {
                                // skip unparseable chunks
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = (e.message ?: "Unknown error")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                writeStringUtf8("data: {\"error\":\"$errorMsg\"}\n\n")
                flush()
            }
        }
    }

    post("/api/temperature") {
        val request = call.receive<TemperatureRequest>()
        try {
            val response = httpClient.post(DEEPSEEK_API_URL) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(json.encodeToString(DeepSeekRequest.serializer(),
                    DeepSeekRequest(
                        messages = listOf(DeepSeekMessage(role = MessageRole.USER, content = request.prompt)),
                        temperature = request.temperature,
                    )
                ))
            }
            val rawBody = response.bodyAsText()
            val body = json.decodeFromString<DeepSeekResponse>(rawBody)
            val content = body.choices.firstOrNull()?.message?.content ?: "No response"
            call.respond(TemperatureResponse(temperature = request.temperature, content = content))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                TemperatureResponse(temperature = request.temperature, content = "", error = e.message),
            )
        }
    }

}
