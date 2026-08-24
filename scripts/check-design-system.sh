#!/usr/bin/env bash
#
# Design-system compliance check.
#
# Guards the two DESIGN.md invariants that can be verified *mechanically*:
#
#   1. No raw hex colours outside the token file  (DESIGN.md -> "Colour / Role mapping")
#   2. No Material `Icons.*` in the main source set (DESIGN.md -> "Spacing, shape, elevation")
#      — both the use site and the `androidx.compose.material.icons` import.
#
# Deliberately NOT checked: spacing / corner-radius / elevation values. The spacing
# scale (4/9/13/18/26/35dp) and the shape set are real rules, but `18.dp` and `19.dp`
# are syntactically identical, so a grep cannot tell a scale step from a typo without
# knowing what the value means. Those are covered by the screenshot goldens
# (`./gradlew verifyRoborazziDebug`), which compare actual rendered pixels. Adding a
# numeric grep here would produce false positives and train people to ignore this script.
#
# --- Why comments are stripped before matching -------------------------------------
#
# DESIGN.md and CLAUDE.md both *quote* the forbidden constructs when explaining the
# rule ("use painterResource(R.drawable.ic_*), never Icons.*"), and the same sentence
# is likely to end up in a KDoc on the code it governs. A plain `grep -r 'Icons\.'`
# would flag that documentation and punish people for writing it down.
#
# So instead of grepping the raw text, each file is first passed through a small awk
# scanner that blanks out everything that is not executable code, preserving one
# output line per input line so `grep -n` still reports true file line numbers. The
# scanner tracks Kotlin's real lexical states rather than pattern-matching: nested
# `/* */` block comments, `//` to end of line, `"` strings with backslash escapes,
# and `"""` raw strings.
#
# String *bodies* are blanked as well as comments. Neither rule can be legitimately
# satisfied inside a string — `Color(0x...)` in a string is not a colour construction
# and `Icons.` in a string is not an icon reference — so blanking them removes that
# whole false-positive class (a URL like "https://x" also stops looking like a
# comment start, since string state is tracked before `//` is considered).
#
# Escape hatch: put `design-system-ok` in a comment on the offending line and the
# line is skipped. It is visible in review, greppable, and it forces whoever takes
# the exception to write down why.
#
# Usage: scripts/check-design-system.sh   (exit 0 = clean, 1 = violations)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MAIN_SRC="app/src/main/java"
UI_DIR="$MAIN_SRC/com/example/ui"
COLOR_TOKENS="$UI_DIR/theme/Color.kt"

violations=0

# Blank out comment bodies, one output line per input line, so grep -n line numbers
# still point at the real file. See the rationale block above.
strip_comments() {
  awk '
    FNR == 1 { depth = 0; instr = 0; inraw = 0 }
    {
      line = $0; out = ""; i = 1; n = length(line)
      while (i <= n) {
        c = substr(line, i, 1); two = substr(line, i, 2); three = substr(line, i, 3)
        if (depth > 0) {
          if (two == "/*") { depth++; out = out "  "; i += 2; continue }
          if (two == "*/") { depth--; out = out "  "; i += 2; continue }
          out = out " "; i++; continue
        }
        if (inraw) {
          if (three == "\"\"\"") { inraw = 0; out = out three; i += 3; continue }
          out = out " "; i++; continue
        }
        if (instr) {
          if (c == "\\") { out = out "  "; i += 2; continue }
          if (c == "\"") { instr = 0; out = out c; i++; continue }
          out = out " "; i++; continue
        }
        if (three == "\"\"\"") { inraw = 1; out = out three; i += 3; continue }
        if (c == "\"")         { instr = 1; out = out c;     i++;     continue }
        if (two == "//")       { break }
        if (two == "/*")       { depth++; out = out "  "; i += 2; continue }
        out = out c; i++
      }
      instr = 0                      # a non-raw string cannot span lines
      if (line ~ /design-system-ok/) # explicit, greppable opt-out
        print ""
      else
        print out
    }
  ' "$1"
}

# report_hits <pattern> <file>...
# Prints one "    path:line: original source text" per violation, nothing when clean.
# The line number comes from the comment-stripped stream; the text shown is pulled
# back out of the real file so the report reads like the code does.
report_hits() {
  local pattern="$1"; shift
  local f lineno original
  for f in "$@"; do
    while IFS=: read -r lineno _; do
      [ -n "$lineno" ] || continue
      original="$(sed -n "${lineno}p" "$f" | sed 's/^[[:space:]]*//')"
      printf '    %s:%s: %s\n' "$f" "$lineno" "$original"
    done < <(strip_comments "$f" | grep -nE "$pattern" || true)
  done
}

# count_lines <text> -> number of non-empty lines
count_lines() {
  [ -n "$1" ] || { echo 0; return; }
  printf '%s\n' "$1" | grep -c . || true
}

# ---------------------------------------------------------------------------------
# Rule 1: raw hex colours
# ---------------------------------------------------------------------------------
mapfile -t ui_files < <(
  find "$UI_DIR" \( -name '*.kt' -o -name '*.java' \) -type f ! -path "$COLOR_TOKENS" | sort
)

out=""
if [ "${#ui_files[@]}" -gt 0 ]; then
  out="$(report_hits 'Color\(0[xX]' "${ui_files[@]}")"
fi
hits="$(count_lines "$out")"

if [ "$hits" -gt 0 ]; then
  echo "FAIL  raw hex colour in ui/ ($hits)"
  printf '%s\n' "$out"
  echo
  echo "    Colours must come from MaterialTheme.colorScheme (a role) or a named token"
  echo "    in ui/theme/Color.kt. The role table is DESIGN.md -> 'Colour / Role mapping';"
  echo "    it is what keeps the ramps on one lightness scale and lets stock components"
  echo "    inherit the palette. If the colour genuinely does not exist yet, add it as a"
  echo "    token in ui/theme/Color.kt (the one file allowed to hold hex) and map it."
  echo
  violations=$((violations + hits))
fi

# ---------------------------------------------------------------------------------
# Rule 2: Material Icons
# ---------------------------------------------------------------------------------
mapfile -t main_files < <(
  find "$MAIN_SRC" \( -name '*.kt' -o -name '*.java' \) -type f | sort
)

out=""
if [ "${#main_files[@]}" -gt 0 ]; then
  # Two alternatives: a use site (`Icons.Default.Mic`, guarded by a leading
  # non-word char so `MyIcons.` does not match) and the import itself, which ends
  # in a bare `Icons` with no trailing dot and would otherwise slip through.
  out="$(report_hits '(^|[^A-Za-z0-9_])Icons\.|androidx\.compose\.material\.icons' "${main_files[@]}")"
fi
hits="$(count_lines "$out")"

if [ "$hits" -gt 0 ]; then
  echo "FAIL  Material Icons in main source set ($hits)"
  printf '%s\n' "$out"
  echo
  echo "    Use painterResource(R.drawable.ic_*) instead. The 16 bundled icons are"
  echo "    Lucide-style: 24 viewport, 2.75 stroke, round caps, no fill. The Material set"
  echo "    is filled and reads as a different family — see the end of DESIGN.md ->"
  echo "    'Spacing, shape, elevation'. If the glyph you need is missing, add an"
  echo "    ic_*.xml to res/drawable drawn to those same metrics."
  echo
  violations=$((violations + hits))
fi

# ---------------------------------------------------------------------------------
if [ "$violations" -gt 0 ]; then
  echo "design-system check: $violations violation(s)"
  exit 1
fi

echo "design-system check: OK (no raw hex in ui/, no Icons.* in main)"
