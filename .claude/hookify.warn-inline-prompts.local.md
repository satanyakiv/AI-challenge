---
name: warn-inline-prompts
enabled: true
event: edit
action: warn
conditions:
  - field: file_path
    operator: matches
    pattern: \.kt$
  - field: file_path
    operator: not_matches
    pattern: resources/
  - field: diff
    operator: matches
    pattern: (\"\"\"[\s\S]{100,}\"\"\")|("You are )|("Act as )|("System prompt)
---

**Знайдено inline prompt text у .kt файлі!**

Правило з `prompts.md`: всі промпти — в `resources/prompts/{feature}/*.txt`, ніколи inline в Kotlin коді.

**Правильний підхід:**
1. Створи `.txt` файл у `resources/prompts/{feature}/`
2. Додай `lazy` property в об'єкт `Prompts` для завантаження
3. Використай `PromptBuilder` для підстановки `{{змінних}}`

```kotlin
// BAD
val prompt = """You are a helpful assistant..."""

// GOOD — prompts/feature/system.txt + Prompts.SYSTEM
```