package com.portfolio.ai_challenge.experiment

import com.portfolio.ai_challenge.agent.AgentResponse
import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.agent.Day8Agent
import com.portfolio.ai_challenge.models.MessageRole
import com.portfolio.ai_challenge.experiment.models.ConversationEntry
import com.portfolio.ai_challenge.experiment.models.StepResult
import com.portfolio.ai_challenge.experiment.models.TestStep
import com.portfolio.ai_challenge.experiment.models.content
import com.portfolio.ai_challenge.experiment.models.failureMode
import com.portfolio.ai_challenge.experiment.models.id
import com.portfolio.ai_challenge.experiment.models.type

class ExperimentSession(private val agent: Day8Agent, private val logger: ExperimentLogger) {

    val conversationHistory = mutableListOf<ApiMessageDto>()
    val stepResults = mutableListOf<StepResult>()
    val conversationLog = mutableListOf<ConversationEntry>()
    var peakTokens = 0
    var lastTotalTokens = 0

    suspend fun sendStep(userMessage: String, stepId: String, stepType: String, failureMode: String?): AgentResponse {
        conversationHistory.add(ApiMessageDto(role = MessageRole.USER, content = userMessage))
        conversationLog.add(
            ConversationEntry(
                role = MessageRole.USER, content = userMessage,
                stepId = stepId, stepType = stepType, usage = null, timestamp = System.currentTimeMillis(),
            )
        )
        val response = agent.chat(conversationHistory.toList())
        if (response.errorMessage == null) {
            conversationHistory.add(ApiMessageDto(role = MessageRole.ASSISTANT, content = response.content))
            conversationLog.add(
                ConversationEntry(
                    role = MessageRole.ASSISTANT, content = response.content,
                    stepId = stepId, stepType = stepType, usage = response.usage, timestamp = System.currentTimeMillis(),
                )
            )
        }
        response.usage?.totalTokens?.let {
            if (it > peakTokens) peakTokens = it
            lastTotalTokens = it
        }
        stepResults.add(
            StepResult(
                stepId = stepId, stepType = stepType, failureMode = failureMode,
                userMessage = userMessage, assistantResponse = response.content.takeIf { it.isNotEmpty() },
                usage = response.usage, httpStatus = response.httpStatus, errorMessage = response.errorMessage,
                conversationLength = conversationHistory.size, timestamp = System.currentTimeMillis(),
            )
        )
        return response
    }

    suspend fun runSteps(steps: List<TestStep>): Boolean {
        for (step in steps) {
            if (step is TestStep.Checkpoint) logger.logMilestone("CHECKPOINT ${step.id}: ${step.failureMode}")
            if (step is TestStep.Verification) logger.logMilestone("VERIFICATION ${step.id}: ${step.failureMode}")
            logger.logStep(step, conversationHistory.size)
            val response = sendStep(step.content, step.id, step.type, step.failureMode)
            logger.logResponse(response)
            if (response.errorMessage != null) {
                logger.logError("Aborting: ${response.errorMessage}")
                return false
            }
            kotlinx.coroutines.delay(1000)
        }
        return true
    }
}
