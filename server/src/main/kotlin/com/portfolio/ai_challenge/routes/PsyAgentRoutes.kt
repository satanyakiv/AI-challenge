package com.portfolio.ai_challenge.routes

import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyAgent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyResponseMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class PsyStartRequest(val userId: String)

@Serializable
data class PsyStartResponse(val sessionId: String)

@Serializable
data class PsyChatRequest(val sessionId: String, val message: String)

fun Route.psyAgentRoutes(psyAgent: PsyAgent, responseMapper: PsyResponseMapper) {
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
    }
}
