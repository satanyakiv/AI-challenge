package com.portfolio.ai_challenge.routes

import com.portfolio.ai_challenge.agent.day_11_psy_agent.Day12PsyAgent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyResponseMapper
import com.portfolio.ai_challenge.agent.day_11_psy_agent.UpdatePreferencesUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.CommunicationPreferences
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.Formality
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ResponseLength
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class PsyStartRequest(val userId: String)

@Serializable
data class PsyStartResponse(val sessionId: String)

@Serializable
data class PsyChatRequest(val sessionId: String, val message: String)

@Serializable
data class PsyPreferencesRequest(
    val userId: String,
    val language: String = "en",
    val formality: String = "INFORMAL",
    val responseLength: String = "MEDIUM",
    val avoidTopics: List<String> = emptyList(),
)

fun Route.psyAgentRoutes(
    psyAgent: Day12PsyAgent,
    responseMapper: PsyResponseMapper,
    updatePreferences: UpdatePreferencesUseCase,
    contextStore: ContextStore,
) {
    route("/api/agent/psy") {
        post("/start") {
            val request = call.receive<PsyStartRequest>()
            if (request.userId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId cannot be empty"))
                return@post
            }
            val sessionId = psyAgent.startSession(request.userId)
            call.respond(PsyStartResponse(sessionId = sessionId))
        }

        post("/chat") {
            val request = call.receive<PsyChatRequest>()
            if (request.sessionId.isBlank() || request.message.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "sessionId and message cannot be empty"))
                return@post
            }
            try {
                val result = psyAgent.chat(request.sessionId, request.message)
                call.respond(responseMapper.toChatResponse(result))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Session not found")))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Agent error")),
                )
            }
        }

        get("/profile") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@get
            }
            call.respond(contextStore.loadProfile(userId))
        }

        post("/profile/preferences") {
            val request = call.receive<PsyPreferencesRequest>()
            val prefs = CommunicationPreferences(
                language = request.language,
                formality = runCatching { Formality.valueOf(request.formality) }.getOrDefault(Formality.INFORMAL),
                responseLength = runCatching { ResponseLength.valueOf(request.responseLength) }.getOrDefault(ResponseLength.MEDIUM),
                avoidTopics = request.avoidTopics,
            )
            updatePreferences.execute(request.userId, prefs)
            call.respond(contextStore.loadProfile(request.userId))
        }
    }
}
