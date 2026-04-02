# Commands Reference

## Що це?

Кастомні команди Claude Code — це markdown-шаблони в `.claude/commands/` які стандартизують процеси розробки. Кожна команда задає чіткий процес з кроками, чеклістами та правилами.

## Навіщо?

Без стандартних процесів:
- Кожна задача вирішується ad-hoc → inconsistent якість
- Архітектурні правила забуваються → технічний борг
- Тести пропускаються → баги в продакшені
- Code review — суб'єктивний → пропущені проблеми

Команди **кодифікують** найкращі практики в виконувані процедури.

## Decision Tree

```
Нова задача?
├── Фіча / Day task ──────────→ /day <опис>
├── Баг ──────────────────────→ /fix <опис>
├── Рефакторинг ──────────────→ /refactor <опис>
├── Хочу перевірити зміни ────→ /review
├── Запустити тести фічі ─────→ /test-feature <назва>
└── Велика фіча (> 1 год) ───→ Superpowers brainstorming
```

## Команди

### /day — Нова фіча (Day task)

**Файл:** `.claude/commands/day.md`
**Використання:** `/day Реалізувати crisis detection для psy-agent`

**Процес (8 кроків):**
1. READ — сканування існуючого коду
2. PLAN — чекліст файлів, змін, тестів
3. ARCHITECTURE REVIEW — самоперевірка плану (11 правил)
4. TEST FIRST — тести що фейляться
5. IMPLEMENT — код за планом
6. VERIFY — запуск тестів
7. MANUAL TEST PLAN — документ для ручного тестування
8. TEST DATA AS CHIPS — UI chips для тестових сценаріїв

**Особливість:** Зупиняється після кроку 3 і чекає "go" від користувача.

---

### /fix — Виправлення бага

**Файл:** `.claude/commands/fix.md`
**Використання:** `/fix Session state не оновлюється після crisis detection`

**Процес (4 кроки):**
1. REPRODUCE — знайти код, пояснити проблему
2. TEST FIRST — тест що відтворює баг
3. FIX — мінімальна зміна
4. VERIFY — всі тести проходять

**Правило:** Мінімальний diff. Не чіпати що не зламано.

---

### /refactor — Рефакторинг

**Файл:** `.claude/commands/refactor.md`
**Використання:** `/refactor Виділити crisis detection з PsyAgent в UseCase`

**Процес (4 кроки):**
1. AUDIT — список порушень, план змін
2. REFACTOR — zero behavior changes
3. VERIFY — тести проходять
4. CHECKLIST — пост-рефакторинг перевірка

**Правило:** Нуль змін поведінки. Ті самі input → ті самі output.

---

### /review — Code Review

**Файл:** `.claude/commands/review.md`
**Використання:** `/review`

**Процес (4 кроки):**
1. DIFF — перегляд staged і unstaged змін
2. ARCHITECTURE CHECK — 9-point checklist (розмір, DI, types, prompts...)
3. TEST CHECK — наявність тестів для мутацій (happy/no-op/persistence)
4. REPORT — таблиця проблем з severity

**Output:** Структурований звіт з конкретними рекомендаціями.

---

### /test-feature — Запуск тестів фічі

**Файл:** `.claude/commands/test-feature.md`
**Використання:** `/test-feature Day13`

**Процес (3 кроки):**
1. FIND — пошук тестових файлів за назвою
2. RUN — запуск з `--tests` фільтром
3. REPORT — результати (passed/failed)

**Захист:** Виключає `*IntegrationTest.kt` файли.

---

### /restart — Перезапуск серверів

**Файл:** `.claude/commands/restart.sh`
**Використання:** `/restart`, `/restart --server-only`, `/restart --app-only`

**Що робить:**
1. Kill існуючих процесів (port 8080, composeApp)
2. Запуск Ktor сервера з background logging
3. Запуск Compose Desktop через Hot Reload
4. Очікування "Responding at" перед продовженням

## Глобальні правила (всі команди)

- **Ніколи** не запускати `./gradlew test` без `--tests` фільтра
- **Ніколи** не викликати реальний API в тестах (мокати LlmClient)
- **Ніколи** не ламати існуючі тести
- Читати architecture/testing rules **перед** роботою
