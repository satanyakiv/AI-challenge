package com.portfolio.ai_challenge.agent.psy_agent.statemachine

sealed interface TaskPhase {
    val displayName: String
    val order: Int

    data object Assessment : TaskPhase {
        override val displayName = "assessment"
        override val order = 0
    }

    data class PlanProposed(val plan: String = "") : TaskPhase {
        override val displayName = "plan_proposed"
        override val order = 1
    }

    data class Executing(val plan: String = "") : TaskPhase {
        override val displayName = "executing"
        override val order = 2
    }

    data object Validating : TaskPhase {
        override val displayName = "validating"
        override val order = 3
    }

    data object Completed : TaskPhase {
        override val displayName = "completed"
        override val order = 4
    }

    companion object {
        fun fromStorageString(s: String): TaskPhase = when {
            s == "assessment" -> Assessment
            s.startsWith("plan_proposed:") -> PlanProposed(plan = s.removePrefix("plan_proposed:"))
            s.startsWith("executing:") -> Executing(plan = s.removePrefix("executing:"))
            s == "validating" -> Validating
            s == "completed" -> Completed
            else -> Assessment
        }
    }
}

fun TaskPhase.toStorageString(): String = when (this) {
    is TaskPhase.Assessment -> "assessment"
    is TaskPhase.PlanProposed -> "plan_proposed:$plan"
    is TaskPhase.Executing -> "executing:$plan"
    is TaskPhase.Validating -> "validating"
    is TaskPhase.Completed -> "completed"
}
