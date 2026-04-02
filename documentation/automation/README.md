# Automation Guide — AI Challenge

Цей проєкт використовує багаторівневу систему автоматизації для підвищення якості коду, швидкості розробки та зменшення людських помилок.

## Навіщо це потрібно?

Без автоматизації розробник повинен пам'ятати:
- Архітектурні правила (150 рядків на файл, DI через конструктор, sealed types...)
- Тестові правила (не запускати інтеграційні тести, 3 тести на мутацію...)
- Промпт-правила (текст тільки в .txt файлах, ніколи inline...)
- Git workflow (worktrees, branch naming, merge process...)

Автоматизація **enforcement'ить** ці правила замість того, щоб покладатись на пам'ять.

## Шари автоматизації

```
┌─────────────────────────────────────────────┐
│          CI/CD (GitHub Actions)              │  ← Фінальна перевірка на PR
│  Build verification + Unit tests              │
├─────────────────────────────────────────────┤
│       Hookify Rules (Claude Code hooks)      │  ← Попередження в реальному часі
│  File size, inline prompts, hardcoded types  │
├─────────────────────────────────────────────┤
│      Superpowers (structured workflow)       │  ← Процесний контроль
│  Brainstorming → Plan → TDD → Review        │
├─────────────────────────────────────────────┤
│      Custom Commands (/day, /fix, /review)   │  ← Стандартизовані процедури
│  Architecture checklist before code          │
├─────────────────────────────────────────────┤
│      Architecture Rules (.claude/rules/)     │  ← Документовані правила
│  Source of truth for all conventions         │
└─────────────────────────────────────────────┘
```

## Зміст

| Документ | Що описує |
|----------|-----------|
| [superpowers-workflow.md](superpowers-workflow.md) | Superpowers plugin: скіли, workflow, інтеграція |
| [hookify-rules.md](hookify-rules.md) | Hookify правила: що блокується, чому, як додати |
| [ci-cd.md](ci-cd.md) | GitHub Actions: build, тести |
| [commands-reference.md](commands-reference.md) | Кастомні команди: /day, /fix, /review та інші |

## Quick Start

1. **Нова фіча**: `/day <опис>` або brainstorming → writing-plans → subagent-driven-development
2. **Баг**: `/fix <опис>` або systematic-debugging → TDD
3. **Рефакторинг**: `/refactor <опис>`
4. **Code review**: `/review`
5. **Тести фічі**: `/test-feature <назва>`
