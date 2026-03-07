package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskLifecycleEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskPhase
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskStateMachine

class SyncTaskPhaseUseCase {

    fun execute(taskMachine: TaskStateMachine, sessionMachine: SessionStateMachine, msg: String) {
        val lower = msg.lowercase()
        val phase = taskMachine.phase
        when {
            phase is TaskPhase.Assessment && shouldProposePlan(sessionMachine) ->
                taskMachine.transition(TaskLifecycleEvent.AssessmentComplete)
            phase is TaskPhase.PlanProposed && isApproval(lower) ->
                taskMachine.transition(TaskLifecycleEvent.PlanApproved)
            phase is TaskPhase.PlanProposed && isRejection(lower) ->
                taskMachine.transition(TaskLifecycleEvent.PlanRejected)
            phase is TaskPhase.Executing && isCompletionSignal(lower) ->
                taskMachine.transition(TaskLifecycleEvent.ExecutionComplete)
            phase is TaskPhase.Validating && isPositiveValidation(lower) ->
                taskMachine.transition(TaskLifecycleEvent.ValidationPassed)
            phase is TaskPhase.Validating && isNegativeValidation(lower) ->
                taskMachine.transition(TaskLifecycleEvent.ValidationFailed)
        }
    }

    private fun shouldProposePlan(machine: SessionStateMachine): Boolean {
        val state = machine.state
        return state is SessionState.ActiveListening && state.turnCount >= 3
    }

    private fun isApproval(msg: String): Boolean =
        msg.contains("yes") || msg.contains("let's try") || msg.contains("okay") || msg.contains("sure")

    private fun isRejection(msg: String): Boolean =
        msg.contains("no") && (msg.contains("don't want") || msg.contains("not"))

    private fun isCompletionSignal(msg: String): Boolean =
        msg.contains("completed") || msg.contains("done") || msg.contains("finished the exercise")

    private fun isPositiveValidation(msg: String): Boolean =
        msg.contains("helpful") || msg.contains("better") || msg.contains("thank")

    private fun isNegativeValidation(msg: String): Boolean =
        msg.contains("didn't help") || msg.contains("try again") || msg.contains("not helpful")
}
