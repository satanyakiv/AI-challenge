# Superpowers Plugin — Workflow Guide

## Що це?

[Superpowers](https://github.com/obra/superpowers) — це framework структурованого розробницького workflow для Claude Code. Замість того, щоб одразу писати код, він примушує пройти через цикл проєктування → планування → імплементація → review.

## Навіщо?

Проблема з AI-кодингом без структури:
- Код пишеться без дизайну → потім рефакторинг
- Тести пишуться після коду → пропускаються edge cases
- Великі зміни в одному контексті → помилки через context pollution
- Немає review → баги проходять непоміченими

Superpowers вирішує це через **примусовий процес**.

## Доступні скіли (14)

### Процесні скіли (як підходити до задач)

| Скіл | Коли використовувати | Що робить |
|------|---------------------|-----------|
| `brainstorming` | Перед будь-якою новою фічею | Socratic questioning → 2-3 підходи → design doc |
| `writing-plans` | Після затвердження дизайну | Розбивка на задачі по 2-5 хв з exact code |
| `subagent-driven-development` | Виконання плану | Fresh subagent на задачу + 2-stage review |
| `executing-plans` | Альтернатива subagent-driven | Виконання плану inline в окремій сесії |
| `test-driven-development` | Під час імплементації | RED → GREEN → REFACTOR цикл |

### Скіли вирішення проблем

| Скіл | Коли використовувати | Що робить |
|------|---------------------|-----------|
| `systematic-debugging` | Будь-який баг | 4-фазний root cause analysis |
| `verification-before-completion` | Перед завершенням задачі | Фінальна перевірка вимог |

### Git та workspace скіли

| Скіл | Коли використовувати | Що робить |
|------|---------------------|-----------|
| `using-git-worktrees` | Ізоляція фічі | Створення worktree з clean test baseline |
| `finishing-a-development-branch` | Після завершення роботи | Test suite → merge/PR decision |

### Code review скіли

| Скіл | Коли використовувати | Що робить |
|------|---------------------|-----------|
| `requesting-code-review` | Запит review | Template для reviewer'а |
| `receiving-code-review` | Отримання feedback | Процес обробки коментарів |

### Координація

| Скіл | Коли використовувати | Що робить |
|------|---------------------|-----------|
| `dispatching-parallel-agents` | Незалежні задачі | Паралельне виконання subagents |
| `writing-skills` | Створення нових скілів | TDD для документації |

## Повний Workflow (нова фіча)

```
1. brainstorming
   └── Socratic questions → 2-3 approach proposals → Design doc
       └── User approves design

2. writing-plans
   └── Map file structure → Decompose into tasks → Exact code snippets
       └── Plan saved to docs/superpowers/plans/

3. using-git-worktrees
   └── Create isolated workspace → Verify tests pass

4. subagent-driven-development
   ├── Task 1 → Subagent → Spec review → Quality review ✓
   ├── Task 2 → Subagent → Spec review → Quality review ✓
   └── Task N → Subagent → Spec review → Quality review ✓

5. finishing-a-development-branch
   └── Run all tests → Merge/PR decision
```

## Інтеграція з існуючими командами

| Задача | Раніше | Тепер з Superpowers |
|--------|--------|---------------------|
| Нова Day-фіча | `/day` → Plan Mode → код | `brainstorming` → `writing-plans` → `subagent-driven-development` |
| Баг | `/fix` → пошук → виправлення | `systematic-debugging` → `test-driven-development` |
| Рефакторинг | `/refactor` → аудит → зміни | `brainstorming` → worktree → parallel agents |

**Важливо:** `/day` і `/fix` команди залишаються і доповнюють superpowers. Вони читають architecture/testing rules перед роботою — superpowers цього не робить автоматично.

### Рекомендований підхід

- **Невелика задача** (< 30 хв): `/day` або `/fix` — простіше, швидше
- **Велика фіча** (> 1 год): Superpowers workflow — більш структуровано, менше помилок
- **Критичний баг**: `/fix` + `systematic-debugging` — максимальна ретельність

## Ключові принципи Superpowers

1. **Skill Priority**: процесні скіли (brainstorming) → імплементаційні → domain-specific
2. **User Instructions Win**: CLAUDE.md правила мають пріоритет над скілами
3. **No Placeholders**: плани містять exact code, exact commands, exact file paths
4. **Two-Stage Review**: кожна задача — spec compliance + code quality
5. **Subagent Isolation**: fresh subagent на задачу = без context pollution
6. **TDD Everywhere**: RED-GREEN-REFACTOR і для коду, і для скілів
