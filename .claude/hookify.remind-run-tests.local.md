---
name: remind-run-tests
enabled: true
event: edit
action: notify
conditions:
  - field: file_path
    operator: matches
    pattern: server/src/main/kotlin/.*\.kt$
  - field: transcript
    operator: not_contains
    pattern: gradlew.*--tests
---

**Нагадування: запусти unit тести!**

Ти змінив серверний код, але ще не запускав тести в цій сесії.

```bash
./gradlew :server:test --tests "*.НазваТестуTest"
```

Правило з `testing.md`: кожна мутація даних потребує мінімум 3 тести (happy path, no-op, persistence).