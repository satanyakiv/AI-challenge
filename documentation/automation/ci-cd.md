# CI/CD — GitHub Actions

## Що це?

GitHub Actions автоматично перевіряє код при створенні Pull Request і при мержі в main.

## Навіщо?

Локальна автоматизація (hookify, Claude Code) працює тільки коли використовується Claude Code. GitHub Actions — це **safety net** який ловить проблеми незалежно від того, як код потрапив у PR:
- Ручне редагування без Claude Code
- Зміни від інших контрибʼюторів
- Merge conflicts що зламали код

## Workflows

### build.yml — Build & Test

**Trigger:** PR в main + push в main

```
┌──────────────┐     ┌──────────────┐
│  Compile     │     │  Unit Tests  │
│  - server    │ ──> │  - server    │
│  - shared    │     │  (NO integ!) │
│  - composeApp│     │              │
└──────────────┘     └──────────────┘
```

**Кроки:**
1. **Compile** — `compileKotlin` для всіх модулів (server, shared, composeApp JVM)
2. **Unit Tests** — `./gradlew :server:test --tests "*Test"` з виключенням `*IntegrationTest`

**Критично:** Інтеграційні тести НІКОЛИ не запускаються в CI. Вони викликають реальний DeepSeek API.

### Concurrency

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Якщо з'являється новий push в той самий PR — попередній workflow скасовується. Це економить CI minutes.

## Secrets та Environment

| Variable | Де налаштувати | Для чого |
|----------|---------------|----------|
| `DEEPSEEK_API_KEY` | **НЕ додавати в CI!** | Тільки для інтеграційних тестів (локально) |

Unit тести мокають `LlmClient` і не потребують API ключів.

## Розширення (TODO)

Можливі покращення на майбутнє:
- Dokka HTML generation + GitHub Pages deployment
- Lint check (ktlint/detekt) як окремий job
- Android APK build verification
- Dependency vulnerability scanning
