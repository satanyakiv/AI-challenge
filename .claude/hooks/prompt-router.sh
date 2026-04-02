#!/usr/bin/env bash
# UserPromptSubmit hook: matches Ukrainian/English keywords -> injects context hints
# about relevant skills and rules from .claude/
set -u

# --- Input ---
PROMPT=$(jq -r '.prompt // ""' 2>/dev/null) || exit 0

# --- Early exits (no injection) ---
[ -z "$PROMPT" ] && exit 0
[[ "$PROMPT" == /* ]] && exit 0
[ "${#PROMPT}" -lt 3 ] && exit 0

# Skip continuation/acknowledgement prompts
if echo "$PROMPT" | grep -qiE '^(go|ок|ok|далі|продовжуй|yes|так|давай|плюс|y|n|\+|готово|done|lgtm|ні|no|стоп|stop)$'; then
  exit 0
fi

# --- Category keywords (Ukrainian + English) ---
KW_FIX='пофікси|фікс|баг|помилка|ламається|зламалось|не працює|crash|exception|error|bug|broken|debug|падає|краш|зламано|виправ'
KW_REFACTOR='рефакторинг|рефактор|перепиши|почисти|реструктур|реорганіз|refactor|restructure|cleanup|reorganize|спрости|винеси|виділи|extract'
KW_PIPELINE='фіча|фічу|фічі|нова функціональність|implement|pipeline|нова можливість|зроби щоб|повний цикл'
KW_TEST='протестуй|тести|тестами|тестів|юніт.тест|unit.test|test-feature|запусти тести|перевір тестами|напиши тести|write test|run test'
KW_REVIEW='код.рев|code.review|ревю|ревʼю|поревʼювай|переглянь код|перевір код|перевір|review|подивись на код|оціни код|аудит|чєкни'
KW_DAY='day [0-9]|день [0-9]|денна задача|daily task|day task'
KW_PROMPTS='промпт|prompt|llm|template|шаблон|system prompt|системний промпт|PromptBuilder'
KW_MAPPING='маппер|mapper|конверт|конверс|ToMapper|AtoBMapper|перетвор'
KW_ARCH='новий клас|new class|новий ендпоінт|endpoint|роут|route|новий агент|новий юзкейс|usecase|use case|серверн|server feature|архітектур'

# --- Scoring function: count keyword matches (grep returns 1 on no match — handle it) ---
score() {
  local matches
  matches=$(echo "$PROMPT" | grep -oiE "$1" 2>/dev/null || true)
  if [ -z "$matches" ]; then
    echo "0"
  else
    echo "$matches" | wc -l | tr -d ' '
  fi
}

# --- Calculate scores ---
S_FIX=$(score "$KW_FIX")
S_REFACTOR=$(score "$KW_REFACTOR")
S_PIPELINE=$(score "$KW_PIPELINE")
S_TEST=$(score "$KW_TEST")
S_REVIEW=$(score "$KW_REVIEW")
S_DAY=$(score "$KW_DAY")
S_PROMPTS=$(score "$KW_PROMPTS")
S_MAPPING=$(score "$KW_MAPPING")
S_ARCH=$(score "$KW_ARCH")

# --- Build hints (command categories: threshold 1) ---
HINTS=""
[ "$S_FIX" -ge 1 ]      && HINTS="${HINTS}-> Skill: /fix | Read: .claude/rules/Testing.md\n" || true
[ "$S_REFACTOR" -ge 1 ] && HINTS="${HINTS}-> Skill: /refactor | Read: .claude/rules/Architecture.md\n" || true
[ "$S_TEST" -ge 1 ]     && HINTS="${HINTS}-> Skill: /test-feature | Read: .claude/rules/Testing.md\n" || true
[ "$S_REVIEW" -ge 1 ]   && HINTS="${HINTS}-> Skill: /review | Read: .claude/rules/Architecture.md, Testing.md\n" || true
[ "$S_DAY" -ge 1 ]      && HINTS="${HINTS}-> Skill: /day\n" || true
[ "$S_PIPELINE" -ge 1 ] && HINTS="${HINTS}-> Skill: /pipeline or /ask | Read: .claude/rules/*\n" || true

# Rule-only categories (threshold 2, suppressed when /pipeline or /day active)
if [ "$S_PIPELINE" -lt 1 ] && [ "$S_DAY" -lt 1 ]; then
  [ "$S_PROMPTS" -ge 2 ] && HINTS="${HINTS}-> Read: .claude/rules/Prompts.md\n" || true
  [ "$S_MAPPING" -ge 2 ] && HINTS="${HINTS}-> Read: .claude/rules/mapping.md\n" || true
  [ "$S_ARCH" -ge 2 ]    && HINTS="${HINTS}-> Read: .claude/rules/Architecture.md, Testing.md\n" || true
fi

# --- Output (only if hints exist) ---
[ -z "$HINTS" ] && exit 0

CONTEXT="RELEVANT PROJECT RESOURCES for this prompt:\n${HINTS}ACTION: Use the Skill tool to invoke commands. Read the rule files before writing code."

# Use printf %b to interpret \n, then jq for safe JSON escaping
printf '%b' "$CONTEXT" | jq -Rs '{hookSpecificOutput:{hookEventName:"UserPromptSubmit",additionalContext:.}}'