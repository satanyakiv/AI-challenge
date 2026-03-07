package com.portfolio.ai_challenge.experiment

import com.portfolio.ai_challenge.agent.Day8Agent
import com.portfolio.ai_challenge.experiment.models.ExperimentResult
import com.portfolio.ai_challenge.experiment.models.FullExperimentResult
import io.ktor.client.HttpClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val outputJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

suspend fun runFullExperiment(
    httpClient: HttpClient,
    apiKey: String,
    logger: ExperimentLogger,
    outputPath: String,
    loadTestResource: (String) -> String,
): FullExperimentResult {
    val results = mutableListOf<ExperimentResult>()

    val case1 = parseTestData("case_1_short", loadTestResource("case_1_short.txt"))
    logger.logMilestone("Starting experiment: case_1_short")
    val case1Result = Day8ExperimentRunner(
        agent = Day8Agent(httpClient, apiKey, case1.systemPrompt),
        logger = logger,
    ).runExperiment(case1)
    results.add(case1Result)
    logger.logMilestone("Finished case_1_short — peakTokens=${case1Result.peakTokens}, failed=${case1Result.failedSteps}")

    val case2 = parseTestData("case_2_long", loadTestResource("case_2_long.txt"))
    logger.logMilestone("Starting experiment: case_2_long")
    val case2Result = Day8ExperimentRunner(
        agent = Day8Agent(httpClient, apiKey, case2.systemPrompt),
        logger = logger,
    ).runExperiment(case2)
    results.add(case2Result)
    logger.logMilestone("Finished case_2_long — peakTokens=${case2Result.peakTokens}, failed=${case2Result.failedSteps}")

    val case3 = parseTestData("case_3_overflow", loadTestResource("case_3_overflow.txt"))
    logger.logMilestone("Starting experiment: case_3_overflow_fixed (continuation of case_2)")
    val case3Result = Day8ExperimentRunner(
        agent = Day8Agent(httpClient, apiKey, case2.systemPrompt),
        logger = logger,
    ).runOverflowExperiment(case2 = case2, case3 = case3)
    results.add(case3Result)
    logger.logMilestone("Finished case_3_overflow_fixed — peakTokens=${case3Result.peakTokens}, failed=${case3Result.failedSteps}")

    val fullResult = FullExperimentResult(cases = results, generatedAt = System.currentTimeMillis())
    saveResults(fullResult, outputPath)
    logger.logMilestone("Results saved to $outputPath")

    return fullResult
}

fun saveResults(result: FullExperimentResult, outputPath: String) {
    val file = File(outputPath)
    file.parentFile?.mkdirs()
    file.writeText(outputJson.encodeToString(result))
}
