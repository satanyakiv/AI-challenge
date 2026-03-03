# Koog Architecture Skill

**Trigger**: Use this skill when designing or implementing AI agent features with Koog.

**vs koog-reference.md**: `koog-reference.md` is a quick API lookup (syntax, imports, snippets).
This skill provides **architecture decisions, patterns, anti-patterns, and testing** — use it first when starting a new Koog feature.

---

## Architecture Decision Tree

```
What do you need?
│
├── Simple Q&A / chat completion
│   └── AIAgent with chatAgentStrategy() or default
│
├── Agent needs external tools (search, DB, API calls)
│   ├── JVM only → annotation-based ToolSet (@Tool)
│   └── Multiplatform → class-based SimpleTool<Args>
│   └── Strategy: chatAgentStrategy()
│
├── Multi-step reasoning (think before acting)
│   └── reActStrategy(reasoningInterval = 1)
│
├── Custom flow / retries / conditional branches
│   └── functionalStrategy { ... } (lambda-based)
│
├── Complex branching with named nodes
│   └── Custom strategy graph: strategy<I,O>("name") { ... }
│
├── Typed structured response (not free-form text)
│   └── requestLLMStructured<T>() + StructureFixingParser
│
├── Long-running agent (many turns, memory matters)
│   └── Add nodeLLMCompressHistory node after tool steps
│
├── Real-time output / streaming UX
│   └── Streaming API → collect Flow<StreamFrame>
│
├── External services via MCP protocol
│   └── McpToolRegistryProvider.fromTransport()
│
└── Multiple cooperating agents
    ├── Agents-as-tools (simpler) → wrap agent in @Tool
    └── A2A protocol (full) → koog-a2a artifact
```

---

## Code Examples

See `.claude/koog-reference.md` for Testing, Streaming, MCP Integration, Multi-Agent, and Strategy Visualization snippets.

---

## Anti-Patterns / Common Mistakes

| Anti-Pattern | Why it's Wrong | Fix |
|---|---|---|
| Hardcoding API key in source | Security risk, can't rotate | `System.getenv("DEEPSEEK_API_KEY")` |
| Missing `@LLMDescription` on params | LLM doesn't know what to pass | Add `@LLMDescription` to every param |
| No `maxIterations` set | Agent can loop forever | Always set `maxIterations = N` |
| Using class-based tools on JVM | More verbose than needed | Use annotation-based `@Tool` on JVM |
| Raw JSON parsing of LLM output | Fragile, breaks on format variation | Use `requestLLMStructured` + `StructureFixingParser` |
| Parsing LLM text to extract data | String parsing is brittle | Define `@Serializable` response type |
| No history compression in long agents | Token limit exceeded, cost explosion | Add `nodeLLMCompressHistory` or `RetrieveFactsFromHistory` |
| Single giant system prompt | Hard to maintain, poor modularity | Split concerns: base prompt + dynamic context |
| Ignoring tool execution errors | Silent failures, bad UX | Return error strings; let LLM retry or escalate |

---

## Module / Dependency Guide

| Need | Artifact | Notes |
|---|---|---|
| Core agents + tools | `ai.koog:koog-agents` | Always required |
| Ktor server integration | `ai.koog:koog-ktor` | For web endpoints |
| Spring Boot integration | `ai.koog:koog-spring-boot-starter` | Auto-configuration |
| A2A multi-agent protocol | `ai.koog:koog-a2a` | Agent-to-agent comms |
| Mock testing utilities | `ai.koog:agents-test` | `testImplementation` only |
| MCP tool servers | `ai.koog:agent-mcp` | MCP client/server |

All versions from `gradle/libs.versions.toml` — check `koog` version alias.

---

## Quick Checklist Before Implementing

- [ ] Picked the right strategy for the use case (see decision tree above)
- [ ] All `@Tool` methods and params have `@LLMDescription`
- [ ] `maxIterations` is set
- [ ] Structured output uses `StructureFixingParser(retries = 3)`
- [ ] Long-running agents have history compression
- [ ] API key from env var, not hardcoded
- [ ] Tests use `getMockExecutor` from `agents-test`
- [ ] Tool return type is `String`