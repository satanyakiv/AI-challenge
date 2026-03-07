package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskPhase

class EnforceTaskPhaseUseCase {

    sealed interface PhaseCheck {
        data object Allowed : PhaseCheck
        data class Blocked(val reason: String, val requiredPhase: TaskPhase) : PhaseCheck
    }

    fun execute(taskPhase: TaskPhase, sessionEvent: SessionEvent): PhaseCheck {
        return when (sessionEvent) {
            is SessionEvent.TechniqueProposed -> requireAtLeast(taskPhase, TaskPhase.PlanProposed())
            is SessionEvent.TechniqueAccepted -> requireAtLeast(taskPhase, TaskPhase.Executing())
            is SessionEvent.SessionEndRequested -> requireAtLeast(taskPhase, TaskPhase.Validating)
            else -> PhaseCheck.Allowed
        }
    }

    private fun requireAtLeast(current: TaskPhase, required: TaskPhase): PhaseCheck {
        return if (current.order >= required.order) {
            PhaseCheck.Allowed
        } else {
            PhaseCheck.Blocked(
                reason = "Task is in '${current.displayName}' phase. " +
                    "Required phase: '${required.displayName}' or later.",
                requiredPhase = required,
            )
        }
    }
}
