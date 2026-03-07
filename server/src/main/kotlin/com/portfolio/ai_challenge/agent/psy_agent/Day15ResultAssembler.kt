package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.psy_agent.model.SessionIntent
import com.portfolio.ai_challenge.agent.psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskStateMachine

class Day15ResultAssembler(
    private val contextStore: ContextStore,
    private val enforcePhase: EnforceTaskPhaseUseCase,
) {

    fun checkPhaseEnforcement(
        taskMachine: TaskStateMachine,
        userMessage: String,
        sessionMachine: SessionStateMachine,
    ): PsyChatResult? {
        val sessionEvent = inferSessionEvent(userMessage, sessionMachine)
        if (sessionEvent != null) {
            val check = enforcePhase.execute(taskMachine.phase, sessionEvent)
            if (check is EnforceTaskPhaseUseCase.PhaseCheck.Blocked) {
                return buildBlockedResult(check, taskMachine)
            }
        }
        return null
    }

    private fun inferSessionEvent(userMessage: String, machine: SessionStateMachine): SessionEvent? {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("end the session") || lower.contains("goodbye") -> SessionEvent.SessionEndRequested
            machine.state is SessionState.Greeting && lower.contains("start the") ->
                SessionEvent.TechniqueProposed("", 3)
            else -> null
        }
    }

    private fun buildBlockedResult(
        check: EnforceTaskPhaseUseCase.PhaseCheck.Blocked,
        taskMachine: TaskStateMachine,
    ): PsyChatResult {
        val response = Prompts.Psy.TASK_BLOCKED
            .replace("{{phase}}", taskMachine.phase.displayName)
            .replace("{{required}}", check.requiredPhase.displayName)
            .replace("{{reason}}", check.reason)
        val emptySession = contextStore.loadSession("")
        val emptyProfile = contextStore.loadProfile("")
        return PsyChatResult(
            response = response,
            state = "blocked",
            session = emptySession ?: PsySessionContext("", ""),
            profile = emptyProfile,
            turnContext = TurnContext(plan = "blocked"),
            intent = SessionIntent.Welcome,
            taskPhase = taskMachine.phase.displayName,
            allowedTransitions = taskMachine.allowedEvents(),
        )
    }

    fun buildTurnContext(
        sessionId: String,
        profileUpdate: ProfileUpdate,
        intent: SessionIntent,
        attemptCount: Int,
    ): TurnContext {
        val detectedEmotion = profileUpdate.newConcerns.firstOrNull()
        if (profileUpdate.newConcerns.isNotEmpty()) {
            contextStore.updateSessionEmotions(sessionId, profileUpdate.newConcerns)
        }
        val plan = if (detectedEmotion != null) "validate_and_explore" else intent.apiName
        return TurnContext(attemptCount = attemptCount, detectedEmotion = detectedEmotion, plan = plan)
    }

    fun assembleResult(
        sessionId: String,
        machine: SessionStateMachine,
        taskMachine: TaskStateMachine,
        validation: ValidateAndRetryUseCase.ValidationResult,
        intent: SessionIntent,
        turnContext: TurnContext,
        profileUpdate: ProfileUpdate,
    ): PsyChatResult {
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(updatedSession.userId)
        val profileUpdateStrings = buildList {
            profileUpdate.preferredName?.let { add("name: $it") }
            profileUpdate.newConcerns.forEach { add("concern: $it") }
            profileUpdate.newTriggers.forEach { add("trigger: $it") }
        }
        return PsyChatResult(
            response = validation.response,
            state = machine.state.displayName,
            session = updatedSession,
            profile = profile,
            turnContext = turnContext,
            profileUpdates = profileUpdateStrings,
            intent = intent,
            transitions = machine.history,
            violations = validation.violations,
            taskPhase = taskMachine.phase.displayName,
            allowedTransitions = taskMachine.allowedEvents(),
        )
    }
}
