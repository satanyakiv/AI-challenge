# Testing Rules

> Read before writing any tests.

## Every Feature Needs Both

- **Unit tests**: individual components in isolation (PromptBuilder, InvariantChecker, StateMachine)
- **Integration tests**: full HTTP flow (Ktor test server → endpoint → verify response)

## File Locations

| Module | Path |
|--------|------|
| Server | `server/src/test/kotlin/com/portfolio/ai_challenge/` |
| Shared | `shared/src/commonTest/` |
| ComposeApp | `composeApp/src/commonTest/` |

## Naming

File: `XxxTest.kt` matching the class under test.

Methods use `test[What]_[condition]_[expected]` pattern:

```kotlin
fun testNoDiagnosis_responseContainsDiagnosis_returnsHardBlock() { ... }
fun testCrisisMode_userRequestsEnd_transitionBlocked() { ... }
fun testProfileBuilder_messageWithAnxiety_extractsConcern() { ... }
```

## Unit Test Structure

```kotlin
class InvariantCheckerTest {
    // Setup once
    private val checker = InvariantChecker(listOf(
        NoDiagnosisInvariant(),
        NoMedicationInvariant(),
    ))

    @Test
    fun testCheckAll_cleanResponse_noViolations() {
        val result = checker.checkAll("I understand how difficult this must be")
        assertEquals(0, result.size)
    }

    @Test
    fun testCheckAll_diagnosisInResponse_returnsHardBlock() {
        val result = checker.checkAll("You clearly have depression")
        assertTrue(result.any { it.severity == Severity.HARD_BLOCK })
    }
}
```

## Integration Test Structure

Follow existing `Day10IntegrationTest.kt` pattern:

```kotlin
class Day11IntegrationTest {
    @Test
    fun testPsyChat_validSession_returnsResponseWithMemoryLayers() = testApplication {
        // 1. Setup
        application { module() }

        // 2. Act
        val startResp = client.post("/api/agent/psy/start") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId": "test-user"}""")
        }
        val sessionId = /* extract from startResp */

        val chatResp = client.post("/api/agent/psy/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"sessionId": "$sessionId", "message": "Hello"}""")
        }

        // 3. Assert
        assertEquals(HttpStatusCode.OK, chatResp.status)
        val body = chatResp.bodyAsText()
        assertTrue(body.contains("response"))
        assertTrue(body.contains("memoryLayers"))
    }
}
```

## What to Test Per Component

| Component | Test for |
|-----------|----------|
| StateMachine | Valid transitions, invalid transitions rejected, guards, history |
| InvariantChecker | Each invariant passes/blocks correctly, multiple violations |
| PromptBuilder | Output contains profile, state prompt, context, avoidTopics |
| ContextStore | Session CRUD, profile persistence, isolation, assembleContext |
| Pipeline | Happy path, retry on violation, fallback after max retries |
| Routes | Valid request → 200, invalid session → error, response shape |

## Test Dependencies

- MockK for mocking (already in `libs.versions.toml`)
- `ktor-server-test-host` for integration tests
- `kotlinx-coroutines-test` for suspend functions
- `ktor-client-mock` for mocking HTTP calls in unit tests