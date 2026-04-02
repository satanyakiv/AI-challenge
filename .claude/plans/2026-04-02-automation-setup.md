# Рекомендації по максимальній автоматизації розробки

## Context

Superpowers plugin (v5.0.7) щойно встановлено. Проєкт вже має розвинену інфраструктуру автоматизації:
- 3 глобальні хуки (auto-stage, save-plan, open-pdf)
- 2 hookify правила (блок інтеграційних тестів, блок незбережених планів)
- 3 команди (/day, /fix, /restart)
- 8 плагінів, 6 кастомних скілів
- Архітектурні правила в `.claude/rules/`

Мета — знайти gap'и та максимально автоматизувати workflow розробки.

---

## 1. Superpowers — що дає і як інтегрується

Superpowers додає **14 скілів** структурованого workflow:

| Скіл | Що робить | Як доповнює поточний setup |
|------|-----------|---------------------------|
| `brainstorming` | Socratic design перед кодом | Замінює ручний Plan Mode для нових фіч |
| `writing-plans` | Деталізовані плани з exact code | Доповнює існуючий /day — більш структуровано |
| `subagent-driven-development` | Виконання плану через subagents + 2-stage review | **НОВЕ** — автономна імплементація |
| `test-driven-development` | RED-GREEN-REFACTOR enforcement | Доповнює testing.md правила |
| `systematic-debugging` | 4-фазний root cause analysis | Доповнює /fix — більш методично |
| `using-git-worktrees` | Ізольовані worktrees | **НОВЕ** — ізоляція фіч |
| `finishing-a-development-branch` | Завершення гілки + merge/PR | **НОВЕ** — автоматичний фініш |
| `dispatching-parallel-agents` | Паралельні subagents | **НОВЕ** — швидше виконання |
| `requesting/receiving-code-review` | Структурований code review | **НОВЕ** |
| `verification-before-completion` | Фінальна перевірка | **НОВЕ** |

**Дія:** Superpowers вже встановлено і працює. Скіли активуються автоматично при старті сесії.

---

## 2. Нові Hookify правила (автоматичне enforcement)

Зараз тільки 2 правила. Рекомендую додати:

### 2a. Блок файлів > 150 рядків
- **Event:** Edit/Write
- **Action:** warn
- **Логіка:** Якщо файл після редагування > 150 рядків — попередження "Файл перевищує ліміт 150 рядків, розбий на менші"
- **Чому:** Правило з architecture.md зараз не enforcement'иться автоматично

### 2b. Блок inline prompt strings
- **Event:** Edit/Write
- **Action:** warn
- **Pattern:** Якщо .kt файл (не в resources/) містить багаторядковий string literal з LLM-like контентом
- **Чому:** Правило з prompts.md — промпти мають бути в .txt файлах

### 2c. Блок hardcoded string types
- **Event:** Edit/Write
- **Action:** warn
- **Pattern:** `when.*"assessment"|"active_listening"|"greeting"` тощо
- **Чому:** Zero hardcoded strings rule з architecture.md

### 2d. Auto-run unit tests after implementation
- **Event:** Edit/Write (на server/**/*.kt файлах)
- **Action:** notify
- **Логіка:** Нагадування запустити відповідні unit тести

---

## 3. CI/CD — GitHub Actions

**Зараз: нічого.** Це найбільший gap.

### 3a. Build & Lint on PR
```yaml
# .github/workflows/build.yml
- ./gradlew :server:compileKotlin
- ./gradlew :composeApp:compileKotlin
- ./gradlew :shared:compileKotlin
```

### 3b. Unit Tests on PR
```yaml
# Тільки unit тести, НІКОЛИ інтеграційні
- ./gradlew :server:test --tests "*Test" --exclude-task "*IntegrationTest"
```

### 3c. Dokka on merge to main
```yaml
# Авто-генерація документації при мержі
- ./gradlew :server:dokkaGeneratePublicationHtml
# Deploy to GitHub Pages
```

---

## 4. Git Pre-commit Hooks

**Зараз: нічого.** Claude auto-stages файли, але немає валідації перед комітом.

### 4a. ktlint/detekt check
- Перевірка форматування Kotlin коду
- Швидкий feedback loop ще до CI

### 4b. Перевірка розміру файлів
- Блок комітів з .kt файлами > 150 рядків
- Enforcement architecture.md правила на рівні git

---

## 5. Нові кастомні команди

### 5a. `/review` — Code Review command
```markdown
Запускає code review поточних змін:
1. git diff --staged
2. Перевірка архітектурних правил
3. Перевірка розміру файлів/функцій
4. Перевірка наявності тестів
5. Звіт з findings
```

### 5b. `/test-feature <name>` — Run feature tests
```markdown
Знаходить і запускає всі unit тести для конкретної фічі
./gradlew :server:test --tests "*<name>*Test"
```

### 5c. `/dokka` — Generate & open docs
```markdown
./gradlew :server:dokkaGeneratePublicationHtml && open server/build/dokka/html/index.html
```

---

## 6. Superpowers + поточні команди — інтеграція

### Workflow mapping:

| Задача | Було | Стане |
|--------|------|-------|
| Нова фіча (Day task) | `/day` → manual plan | `brainstorming` → `writing-plans` → `subagent-driven-development` |
| Баг | `/fix` → manual debug | `systematic-debugging` → `test-driven-development` |
| Рефакторинг | Manual | `brainstorming` → worktree → parallel agents |
| Code review | Manual diff reading | `requesting-code-review` → structured feedback |
| Завершення | Manual git push | `finishing-a-development-branch` → auto merge/PR |

### Рекомендація:
- `/day` і `/fix` залишити як є — вони доповнюють superpowers (читають rules перед роботою)
- Superpowers скіли активуються автоматично поверх них
- Для великих фіч — починати з `brainstorming` замість Plan Mode

---

## 7. MCP Servers (додаткові інструменти)

**Зараз:** Тільки chrome та mobile MCP. Рекомендую:

### 7a. Database MCP
- Інспекція Room/SQLite бази прямо з Claude Code
- Корисно для debugging даних

### 7b. HTTP Client MCP
- Тестування Ktor API ендпоінтів без Postman
- Вже є curl в permissions, але MCP дасть структурований інтерфейс

---

## 8. Пріоритетний план дій

| # | Дія | Зусилля | Вплив |
|---|-----|---------|-------|
| 1 | ✅ Superpowers встановлено | Done | Високий |
| 2 | Hookify: блок файлів > 150 рядків | 5 хв | Середній |
| 3 | Hookify: блок inline prompts | 5 хв | Середній |
| 4 | GitHub Actions: build on PR | 15 хв | Високий |
| 5 | GitHub Actions: unit tests on PR | 15 хв | Високий |
| 6 | Command: `/review` | 10 хв | Середній |
| 7 | Command: `/test-feature` | 5 хв | Низький |
| 8 | Git pre-commit: ktlint | 10 хв | Середній |
| 9 | Command: `/dokka` | 5 хв | Низький |
| 10 | MCP: Database inspector | 20 хв | Низький |

---

## 9. Документація — `documentation/automation/`

Створити документацію що пояснює ЩО автоматизовано, ЧОМУ, і ЯК користуватись.

### Файли:

#### `documentation/automation/README.md`
Головний entry point — overview всієї автоматизації з посиланнями на деталі.

#### `documentation/automation/superpowers-workflow.md`
- Що таке superpowers і які скіли доступні
- Workflow: brainstorming → planning → implementation → review → finish
- Приклади використання з поточним проєктом
- Інтеграція з /day та /fix командами

#### `documentation/automation/hookify-rules.md`
- Всі hookify правила (існуючі + нові)
- Чому кожне правило існує (зв'язок з architecture.md)
- Як додати нове правило

#### `documentation/automation/ci-cd.md`
- GitHub Actions workflows
- Що перевіряється на PR
- Як працює Dokka deployment

#### `documentation/automation/commands-reference.md`
- Всі кастомні команди (/day, /fix, /restart, /review, /test-feature, /dokka)
- Коли використовувати яку команду
- Decision tree: яку команду обрати

---

## Verification

- Superpowers: запустити нову сесію Claude Code, перевірити що `using-superpowers` завантажується
- Hookify: створити тестовий файл > 150 рядків, перевірити попередження
- CI/CD: створити PR, перевірити що workflow запускається
- Commands: запустити `/review`, `/test-feature`, `/dokka`
- Документація: перевірити що всі файли створені і містять актуальну інфу
