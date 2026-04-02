---
name: warn-hardcoded-types
enabled: true
event: edit
action: warn
conditions:
  - field: file_path
    operator: matches
    pattern: \.kt$
  - field: diff
    operator: matches
    pattern: when\s*\(.*\)\s*\{[\s\S]*?\"(assessment|active_listening|greeting|closing|intervention|crisis|plan_proposed|executing|validating|completed)\"
---

**Знайдено hardcoded string замість sealed type/enum!**

Правило з `architecture.md`: **Zero Hardcoded Strings** — якщо існує enum або sealed interface, використовуй тип напряму.

**Правильний підхід:**
```kotlin
// BAD — string comparison
when (state) { "greeting" -> ... }

// GOOD — type-safe
when (state) { is SessionState.Greeting -> ... }
```

Якщо enum/sealed ще не існує — створи його першим.