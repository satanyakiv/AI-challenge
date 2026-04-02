## Run Feature Tests

Знайди і запусти всі unit тести для фічі: **$ARGUMENTS**

### Process

1. **FIND** — Знайди тестові файли:
   ```bash
   find server/src/test -name "*$ARGUMENTS*Test.kt" -not -name "*IntegrationTest.kt"
   ```

2. **RUN** — Запусти кожен знайдений тест:
   ```bash
   ./gradlew :server:test --tests "*НазваTest"
   ```
   **НІКОЛИ** не запускай без `--tests` фільтра!

3. **REPORT** — Покажи результат:
   - Знайдено тестів: X
   - Пройшло: Y / X
   - Провалилось: Z (з деталями помилок)

### Rules
- НЕ запускай файли що закінчуються на `IntegrationTest.kt`
- Завжди використовуй `--tests` фільтр
- Якщо тестів не знайдено — повідом про це