package com.portfolio.ai_challenge.experiment

import com.portfolio.ai_challenge.agent.Day8Agent
import com.portfolio.ai_challenge.experiment.models.ExperimentResult
import com.portfolio.ai_challenge.experiment.models.TestCase
import com.portfolio.ai_challenge.experiment.models.TestStep
import kotlinx.coroutines.delay

class Day8ExperimentRunner(
    private val agent: Day8Agent,
    private val logger: ExperimentLogger,
) {
    suspend fun runExperiment(testCase: TestCase): ExperimentResult {
        val startedAt = System.currentTimeMillis()
        val session = ExperimentSession(agent, logger)
        val ok = session.runSteps(testCase.steps)

        if (ok && session.peakTokens < 120_000) {
            runOverflowCycles(session, testCase.steps.filterIsInstance<TestStep.Message>())
        }

        return buildResult(testCase.name, session, startedAt)
    }

    suspend fun runOverflowExperiment(case2: TestCase, case3: TestCase): ExperimentResult {
        val startedAt = System.currentTimeMillis()
        val session = ExperimentSession(agent, logger)

        logger.logMilestone("Phase 1: Loading Case 2 — Studies 1-10")
        val phase1Ok = session.runSteps(case2.steps.filter { it !is TestStep.Verification })
        logger.logMilestone("Phase 1 complete. Tokens: ${session.lastTotalTokens}")

        if (phase1Ok) {
            logger.logMilestone("Phase 2: Loading Case 3 — Studies 11-25")
            val phase2Ok = session.runSteps(case3.steps.filter { it !is TestStep.Verification })
            logger.logMilestone("Phase 2 complete. Tokens: ${session.lastTotalTokens}")

            if (phase2Ok) {
                val allMessages = (case2.steps + case3.steps).filterIsInstance<TestStep.Message>()
                runRepetitionRounds(session, allMessages)
            }
        }

        logger.logMilestone("Phase 4: Sending verifications at ${session.lastTotalTokens} tokens")
        val verifications = case2.steps.filterIsInstance<TestStep.Verification>() +
            case3.steps.filterIsInstance<TestStep.Verification>()
        session.runSteps(verifications)
        logger.logMilestone("Phase 4 complete. Final peak tokens: ${session.peakTokens}")

        return buildResult("case_3_overflow_fixed", session, startedAt)
    }

    private suspend fun runOverflowCycles(session: ExperimentSession, messages: List<TestStep.Message>) {
        for (cycle in 1..3) {
            if (session.peakTokens >= 120_000) break
            logger.logMilestone("Overflow repeat cycle $cycle/3 (peakTokens=${session.peakTokens})")
            for (msg in messages) {
                if (session.peakTokens >= 120_000) break
                val repeatId = "${msg.id}-R$cycle"
                logger.logStep(TestStep.Message(id = repeatId, content = "REPEAT: ${msg.content}"), session.conversationHistory.size)
                val response = session.sendStep("REPEAT: ${msg.content}", repeatId, "message", null)
                logger.logResponse(response)
                if (response.errorMessage != null) {
                    logger.logError("Aborting overflow: ${response.errorMessage}")
                    return
                }
                delay(1000)
            }
        }
    }

    private suspend fun runRepetitionRounds(session: ExperimentSession, messages: List<TestStep.Message>) {
        var round = 0
        while (session.lastTotalTokens < 120_000 && round < 10) {
            round++
            logger.logMilestone("Phase 3 — Repetition round R$round. Tokens: ${session.lastTotalTokens}")
            for (step in messages) {
                if (session.lastTotalTokens >= 125_000) break
                val repeatId = "${step.id}-R$round"
                val repeatContent = "REPEAT R$round: ${step.content}"
                logger.logStep(TestStep.Message(id = repeatId, content = repeatContent), session.conversationHistory.size)
                val response = session.sendStep(repeatContent, repeatId, "message", null)
                logger.logResponse(response)
                if (response.errorMessage != null) {
                    logger.logError("Error in R$round at $repeatId: ${response.errorMessage}")
                    return
                }
                delay(1000)
            }
        }
        logger.logMilestone("Phase 3 complete. Peak tokens: ${session.peakTokens}")
    }

    private fun buildResult(caseName: String, session: ExperimentSession, startedAt: Long) = ExperimentResult(
        caseName = caseName,
        steps = session.stepResults.toList(),
        conversationLog = session.conversationLog.toList(),
        totalMessages = session.conversationHistory.size,
        peakTokens = session.peakTokens,
        totalSteps = session.stepResults.size,
        failedSteps = session.stepResults.count { it.errorMessage != null },
        startedAt = startedAt,
        finishedAt = System.currentTimeMillis(),
    )
}
