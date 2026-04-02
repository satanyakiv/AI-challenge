# Plan: Команди /ask та /pipeline

## Context

Потрібні дві slash-команди для структурованої розробки фіч:
- `/ask` — глибоке інтерв'ю по SPEC.md, результат у окремий файл
- `/pipeline` — повний цикл: spec → interview → code → review → test → fix → review → PR

## Files to Create

1. `.claude/commands/ask.md`
2. `.claude/commands/pipeline.md`

---

## `/ask` Command

**Файл:** `.claude/commands/ask.md`
**Input:** `$ARGUMENTS` = шлях до SPEC.md (наприклад `documentation/day-16/SPEC.md`)
**Output:** окремий файл `SPEC-refined.md` поруч з оригіналом

**Process:**

1. **READ** — прочитати файл з `$ARGUMENTS`, зрозуміти скоуп фічі
2. **ANALYZE** — виявити прогалини, неоднозначності, технічні ризики, неспецифіковані edge cases
3. **INTERVIEW** — серія AskUserQuestion (по 2-3 питання за раз), категорії:
   - Technical: архітектура, data flow, edge cases, error handling, API contracts
   - UI/UX: стани компонентів, transitions, empty/error/loading states, accessibility
   - Tradeoffs: performance vs readability, complexity vs flexibility, scope vs deadline
   - Integration: з існуючим кодом, breaking changes, міграції, backwards compat
   - Concerns: що може піти не так, масштабування, maintenance burden
   - Правило: питання мають бути НЕ очевидні — не "який колір кнопки", а "що станеться коли юзер відкриє 3 сесії одночасно"
   - Продовжувати доки всі прогалини не закриті
4. **WRITE** — створити `SPEC-refined.md` поруч з оригіналом, структуровано з усіма відповідями

---

## `/pipeline` Command

**Файл:** `.claude/commands/pipeline.md`
**Input:** `$ARGUMENTS` = шлях до SPEC.md
**Checkpoints:** go тільки після interview (перед кодингом). Решта — автоматично.

**Stages:**

1. **SPEC** — прочитати `$ARGUMENTS`
2. **INTERVIEW** — той самий процес що в /ask (вбудований). Питання по 2-3, не очевидні. Записати SPEC-refined.md
3. **--- CHECKPOINT: AskUserQuestion "go" перед кодингом ---**
4. **PLAN** — EnterPlanMode, створити план імплементації з архітектурним ревʼю
5. **CODE** — імплементація за планом. Тести теж тут (test-first де можливо)
6. **REVIEW** — архітектурний чеклист (файли <150, функції <20, agent <80, sealed types, DI, prompts in resources)
7. **TEST** — запустити тести з `--tests` фільтром
8. **FIX** — виправити проблеми з review + failing tests
9. **REVIEW 2** — повторний чеклист після фіксів
10. **PR** — `gh pr create` з summary з SPEC-refined.md

Між review→test→fix→review2→PR все автоматично, без зупинок.

---

## Verification

- Перевірити що `/ask documentation/day-16/SPEC.md` працює
- Перевірити що `/pipeline documentation/day-16/SPEC.md` працює
- Перевірити що SPEC-refined.md створюється коректно
