package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

// Day 12: same orchestration as PsyAgent, adds two things:
//  1. PsyPromptBuilder now injects PersonalizeResponseUseCase → system prompt is personalized
//  2. profileUpdateStrings — what was extracted this turn is returned to the client,
//     so the UI can show "Detected: name: Alice, concern: anxiety" chips
class Day12PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateProfileUseCase,
) {

    fun startSession(userId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): PsyChatResult {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        // UpdateProfileUseCase: extracts name/concerns/triggers from the message AND saves them to profile
        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val detectedEmotion = profileUpdate.newConcerns.firstOrNull()
        val plan = if (detectedEmotion != null) "validate_and_explore" else "active_listening"
        val turnContext = TurnContext(attemptCount = attemptCount, detectedEmotion = detectedEmotion, plan = plan)
        if (profileUpdate.newConcerns.isNotEmpty()) {
            contextStore.updateSessionEmotions(sessionId, profileUpdate.newConcerns)
        }

        // PersonalizeResponseUseCase (inside PsyPromptBuilder) builds a system prompt
        // that reflects formality, response length, avoid-topics and user summary
        val context = contextStore.assembleContext(sessionId, "active")
        val messages = promptBuilder.buildMessages(context)
        val response = llmClient.complete(messages, maxTokens = 300)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = response))
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(context.userId)

        // Day 12 addition: turn the extracted ProfileUpdate into human-readable strings
        // and return them so the client can display "what the agent learned this turn"
        val profileUpdateStrings = buildList {
            profileUpdate.preferredName?.let { add("name: $it") }
            profileUpdate.newConcerns.forEach { add("concern: $it") }
            profileUpdate.newTriggers.forEach { add("trigger: $it") }
        }
        return PsyChatResult(response, "active", updatedSession, profile, turnContext, profileUpdateStrings)
    }
}
