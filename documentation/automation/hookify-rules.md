# Hookify Rules

## Що це?

Hookify — плагін Claude Code, який створює правила-хуки у форматі markdown. Правила автоматично перевіряються під час роботи Claude Code і блокують/попереджають при порушеннях.

## Навіщо?

Архітектурні правила в `.claude/rules/` — це документація. Claude їх читає, але може забути або проігнорувати під тиском складної задачі. Hookify **примусово enforcement'ить** ці правила через автоматичні перевірки.

## Активні правила

### 1. block-integration-tests (block)
**Файл:** `.claude/hookify.block-integration-tests.local.md`
**Event:** bash | **Action:** block

**Що робить:** Блокує запуск `./gradlew test` без `--tests` фільтра.

**Чому:** Інтеграційні тести (`*IntegrationTest.kt`) викликають реальний DeepSeek API і коштують грошей. Запуск без фільтра запустить ВСІ тести, включаючи інтеграційні.

**Зв'язок:** `testing.md` → "NEVER run `./gradlew test` without `--tests` filter"

---

### 2. save-plan-on-stop (block)
**Файл:** `.claude/hookify.save-plan-on-stop.local.md`
**Event:** stop | **Action:** block

**Що робить:** Блокує завершення сесії якщо Plan Mode використовувався, але план не збережено в `.claude/plans/`.

**Чому:** Плани мають цінність між сесіями. Якщо план не збережено — він втрачається.

**Зв'язок:** `CLAUDE.md` → "MANDATORY: When exiting Plan Mode, save plan"

---

### 3. warn-large-files (warn)
**Файл:** `.claude/hookify.warn-large-files.local.md`
**Event:** edit | **Action:** warn

**Що робить:** Попереджає коли .kt файл перевищує 150 рядків після редагування.

**Чому:** Великі файли порушують SRP, ускладнюють review і підтримку.

**Зв'язок:** `architecture.md` → "Files: < 150 lines. If more — split."

---

### 4. warn-inline-prompts (warn)
**Файл:** `.claude/hookify.warn-inline-prompts.local.md`
**Event:** edit | **Action:** warn

**Що робить:** Попереджає коли в .kt файлі з'являються inline prompt strings (великі multiline strings або патерни типу "You are", "Act as").

**Чому:** Промпти — це конфігурація, не код. Вони мають жити в `resources/prompts/` для зручності редагування, тестування і version control.

**Зв'язок:** `prompts.md` → "All prompt text in resources, never inline in .kt"

---

### 5. warn-hardcoded-types (warn)
**Файл:** `.claude/hookify.warn-hardcoded-types.local.md`
**Event:** edit | **Action:** warn

**Що робить:** Попереджає коли в `when` блоках з'являються string literals що відповідають відомим sealed type значенням ("assessment", "greeting", "crisis" тощо).

**Чому:** Hardcoded strings замість sealed types — це type-unsafe код що не перевіряється компілятором.

**Зв'язок:** `architecture.md` → "Zero Hardcoded Strings for Types"

---

### 6. remind-run-tests (notify)
**Файл:** `.claude/hookify.remind-run-tests.local.md`
**Event:** edit | **Action:** notify

**Що робить:** Нагадує запустити unit тести коли серверний .kt код змінено, але тести ще не запускались в цій сесії.

**Чому:** Зміни без тестів — це бомба уповільненої дії.

**Зв'язок:** `testing.md` → "Every data mutation gets a test"

---

## Як додати нове правило

1. Створи файл `.claude/hookify.<name>.local.md`
2. Додай frontmatter:
```yaml
---
name: rule-name
enabled: true
event: bash|edit|stop
action: block|warn|notify
pattern: regex-pattern         # для event: bash
conditions:                    # для event: edit/stop
  - field: file_path|diff|transcript|file_line_count
    operator: matches|contains|not_contains|greater_than
    pattern: "value"
---
```
3. Напиши зрозуміле пояснення (українською) чому це правило існує
4. Додай посилання на відповідне правило з `.claude/rules/`

## Рівні severity

| Action | Коли використовувати |
|--------|---------------------|
| `block` | Критичне порушення: коштує грошей, втрата даних, безпека |
| `warn` | Архітектурне порушення: порушення конвенцій, якість коду |
| `notify` | Нагадування: корисні підказки, non-critical best practices |
