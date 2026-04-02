---
name: warn-large-files
enabled: true
event: edit
action: warn
conditions:
  - field: file_path
    operator: matches
    pattern: \.kt$
  - field: file_line_count
    operator: greater_than
    pattern: "150"
---

**Файл перевищує ліміт 150 рядків!**

Правило з `architecture.md`: кожен файл повинен бути < 150 рядків.

**Що робити:**
1. Виділи логіку в окремий UseCase/Mapper/Component
2. Розбий файл на менші за відповідальністю
3. Один клас/інтерфейс = один файл

Не ігноруй це попередження — великі файли порушують SRP і ускладнюють code review.