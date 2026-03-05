# Architecture Rules

> Read before writing any server feature.

## Layered Architecture

Every server feature MUST have these layers in separate files:

```
Routes (HTTP)  →  Agent (orchestration)  →  Components (logic)  →  Store (data)
```

**Routes** (`routes/XxxRoutes.kt`) — HTTP only: request/response DTOs, parsing, status codes. No business logic.

**Agent** (`agent/xxx/XxxAgent.kt`) — Orchestration only. Reads like a recipe:

```kotlin
suspend fun chat(sessionId: String, message: String): ChatResult {
  val context = contextStore.assembleContext(sessionId)
  val prompt = promptBuilder.build(context, message)
  val response = llmClient.complete(prompt)
  contextStore.save(sessionId, message, response)
  return responseMapper.toResult(context, response)
}
```

Never put in Agent: HTTP calls, prompt strings, JSON serialization, DTOs.

**Components** — separate file per responsibility:

| File | Does |
|------|------|
| `XxxPromptBuilder.kt` | Builds prompts from loaded templates + runtime data |
| `XxxResponseMapper.kt` | Internal models → API response DTOs |
| `LlmClient.kt` | DeepSeek HTTP wrapper (shared across agents) |
| `XxxValidator.kt` | Validates responses (invariants, safety) |

**Store** (`memory/`, `database/`) — Data access only. Interface first, implementation second.

## Dependency Injection

Never create dependencies inside a class. Inject via constructor, register in Koin.

```kotlin
// BAD
class PsyAgent { private val store = InMemoryContextStore() }

// GOOD
class PsyAgent(private val contextStore: ContextStore, private val llmClient: LlmClient)
```

## Sealed Types Over Strings

```kotlin
// BAD
val state: String = "active_listening"

// GOOD
sealed interface SessionState {
  data object Greeting : SessionState
  data class ActiveListening(val turnCount: Int) : SessionState
}
```

## Size Limits

- Files: < 150 lines. If more — split.
- Functions: < 20 lines. If more — extract helpers.
- Agent class: < 80 lines. If more — extract to components.

## File Organization

```
server/src/main/kotlin/com/portfolio/ai_challenge/
  agent/
    psy/
      PsyAgent.kt                # orchestration only
      PsyPromptBuilder.kt        # prompt composition
      PsyResponseMapper.kt       # internal → DTO mapping
      prompts/
        PsyPrompts.kt            # loads .txt prompt files
      model/
        PsySessionContext.kt     # one class per file
        PsyUserProfile.kt
      memory/
        ContextStore.kt          # interface
        InMemoryContextStore.kt  # implementation
      statemachine/
        SessionState.kt
        SessionStateMachine.kt
      invariants/
        Invariant.kt
        InvariantChecker.kt
        impl/
          NoDiagnosisInvariant.kt
  models/
    LlmClient.kt                # shared DeepSeek wrapper
    DeepSeekModels.kt
  routes/
    PsyAgentRoutes.kt           # HTTP layer + DTOs

server/src/main/resources/
  prompts/psy/
    system.txt
    state-greeting.txt
    state-active-listening.txt
    state-intervention.txt
    state-crisis.txt
    state-closing.txt
```