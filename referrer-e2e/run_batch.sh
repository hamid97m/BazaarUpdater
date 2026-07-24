#!/usr/bin/env bash
# Batch-run the referrer E2E test N times (default 100), uninstalling the SDK app
# before each run, and writing structured logs for offline analysis of Bazaar
# install-referrer delivery.
#
# Prerequisites (once):
#   1. Device/emulator with production Bazaar installed + signed in
#   2. Build + install harness + test APKs:
#        cd sdk/BazaarUpdater
#        ./gradlew :referrer-e2e:assembleDebug :referrer-e2e:assembleDebugAndroidTest
#        adb install -r referrer-e2e/build/outputs/apk/debug/referrer-e2e-debug.apk
#        adb install -r referrer-e2e/build/outputs/apk/androidTest/debug/referrer-e2e-debug-androidTest.apk
#
# Usage:
#   ANDROID_SERIAL=emulator-5554 ./run_batch.sh 100
#   ANDROID_SERIAL=<device> OUT_DIR=~/referrer_e2e_100 ./run_batch.sh 100
#
# Outputs (under OUT_DIR, default: ./results/<timestamp>):
#   summary.tsv          — one row per run: run, result, reason, referrer
#   summary.md           — same as markdown table + totals
#   console.log          — live console stream of all runs
#   runs/run_NNN/        — per-run artifacts:
#     instrument.txt     — raw am instrument output
#     e2e.log            — ReferrerE2E logcat (flow + REFERRER_SCREEN dump)
#     referrer_screen.txt— extracted SCREEN: lines (full app referrer UI)
#     result.txt         — PASS/FAIL + reason + referrer one-liner
#   failures/            — copies of failed run dirs for quick triage
#
# After it finishes, inspect:
#   grep FAIL "$OUT_DIR/summary.tsv"
#   ls "$OUT_DIR/failures"
#   cat "$OUT_DIR/failures/run_006/referrer_screen.txt"

set -u

N="${1:-100}"
TARGET="com.farsitel.bazaar.bazaarInstallReferrerTest"
RUNNER="com.farsitel.bazaar.referrere2e.test/androidx.test.runner.AndroidJUnitRunner"
CLASS="com.farsitel.bazaar.referrere2e.ReferrerInstallFlowTest#installViaMetrixLink_thenReferrerIsShownOnScreen"

STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${OUT_DIR:-$(cd "$(dirname "$0")" && pwd)/results/${STAMP}}"
RUNS_DIR="$OUT_DIR/runs"
FAIL_DIR="$OUT_DIR/failures"
mkdir -p "$RUNS_DIR" "$FAIL_DIR"

SUMMARY_TSV="$OUT_DIR/summary.tsv"
SUMMARY_MD="$OUT_DIR/summary.md"
CONSOLE="$OUT_DIR/console.log"

echo -e "run\tresult\treason\treferrer" > "$SUMMARY_TSV"
{
  echo "# Referrer E2E batch — $STAMP"
  echo
  echo "| run | result | reason | referrer |"
  echo "|-----|--------|--------|----------|"
} > "$SUMMARY_MD"

pass=0
fail=0

log_console() {
  # shellcheck disable=SC2001
  echo "$@" | tee -a "$CONSOLE"
}

extract_referrer() {
  local logf="$1"
  # Prefer SUCCESS line; else PARSED_REFERRER from the screen dump.
  local ref
  ref=$(grep -o "SUCCESS: referrer on screen = '.*'" "$logf" 2>/dev/null | tail -1 | sed "s/.*SUCCESS: referrer on screen = '//;s/'$//")
  if [ -z "$ref" ]; then
    ref=$(grep -o "PARSED_REFERRER=.*" "$logf" 2>/dev/null | tail -1 | sed 's/^PARSED_REFERRER=//')
  fi
  echo "${ref:-}"
}

extract_screen() {
  local logf="$1"
  local outf="$2"
  # Pull the REFERRER_SCREEN dump (markers + SCREEN: lines + PARSED_REFERRER).
  if grep -q "REFERRER_SCREEN_BEGIN" "$logf" 2>/dev/null; then
    sed -n '/REFERRER_SCREEN_BEGIN/,/REFERRER_SCREEN_END/p' "$logf" \
      | sed 's/^.*ReferrerE2E: //' > "$outf"
  else
    echo "(no REFERRER_SCREEN dump — app may not have opened)" > "$outf"
  fi
}

log_console "OUT_DIR=$OUT_DIR"
log_console "N=$N  ANDROID_SERIAL=${ANDROID_SERIAL:-"(default adb device)"}"
log_console "Starting at $(date -Iseconds)"
log_console ""

for i in $(seq 1 "$N"); do
  pad=$(printf "%03d" "$i")
  RUN_DIR="$RUNS_DIR/run_$pad"
  mkdir -p "$RUN_DIR"
  INST="$RUN_DIR/instrument.txt"
  E2ELOG="$RUN_DIR/e2e.log"
  SCREEN="$RUN_DIR/referrer_screen.txt"
  RESULT="$RUN_DIR/result.txt"

  log_console "==================== RUN $i / $N ===================="

  adb shell pm uninstall "$TARGET" >/dev/null 2>&1 || true
  adb shell am force-stop com.android.chrome >/dev/null 2>&1 || true
  adb shell am force-stop com.farsitel.bazaar >/dev/null 2>&1 || true
  adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
  adb logcat -c >/dev/null 2>&1 || true

  # Capture ReferrerE2E only (flow + REFERRER_SCREEN dump). Keep PID to stop later.
  adb logcat -v time -s ReferrerE2E:I > "$E2ELOG" 2>&1 &
  LC_PID=$!

  adb shell am instrument -w -r \
    -e class "$CLASS" \
    "$RUNNER" > "$INST" 2>&1
  INST_RC=$?

  # Give logcat a beat to flush, then stop.
  sleep 0.5
  kill "$LC_PID" 2>/dev/null || true
  wait "$LC_PID" 2>/dev/null || true

  extract_screen "$E2ELOG" "$SCREEN"
  REF="$(extract_referrer "$E2ELOG")"

  if grep -q "OK (1 test)" "$INST"; then
    STATUS="PASS"
    REASON=""
    pass=$((pass + 1))
    log_console "RUN $i: PASS  | referrer=$REF"
  else
    STATUS="FAIL"
    REASON=$(grep -o "AssertionError: .*" "$INST" | head -1)
    if [ -z "$REASON" ]; then
      REASON=$(grep -E "INSTRUMENTATION_CODE:|Process crashed|Error in " "$INST" | head -1)
    fi
    REASON="${REASON:-instrument_rc=$INST_RC}"
    fail=$((fail + 1))
    log_console "RUN $i: FAIL  | $REASON"
    log_console "         referrer=$REF"
    log_console "         screen -> $SCREEN"
    # Snapshot failed run for quick triage.
    cp -R "$RUN_DIR" "$FAIL_DIR/run_$pad"
  fi

  {
    echo "result=$STATUS"
    echo "reason=$REASON"
    echo "referrer=$REF"
    echo "instrument=$INST"
    echo "e2e_log=$E2ELOG"
    echo "referrer_screen=$SCREEN"
  } > "$RESULT"

  # TSV: escape tabs/newlines in fields
  safe_reason=$(printf '%s' "$REASON" | tr '\t\n' '  ')
  safe_ref=$(printf '%s' "$REF" | tr '\t\n' '  ')
  echo -e "$i\t$STATUS\t$safe_reason\t$safe_ref" >> "$SUMMARY_TSV"
  echo "| $i | $STATUS | ${safe_reason:-} | \`${safe_ref:-}\` |" >> "$SUMMARY_MD"
done

{
  echo
  echo "## Totals"
  echo
  echo "- PASS: $pass"
  echo "- FAIL: $fail"
  echo "- N: $N"
  echo "- Finished: $(date -Iseconds)"
  echo
  echo "Failed run dirs: \`$FAIL_DIR\`"
} >> "$SUMMARY_MD"

log_console ""
log_console "==================== SUMMARY ===================="
log_console "PASS=$pass  FAIL=$fail  (of $N)"
log_console "OUT_DIR=$OUT_DIR"
log_console "summary: $SUMMARY_TSV"
log_console "failures: $FAIL_DIR ($(ls "$FAIL_DIR" 2>/dev/null | wc -l | tr -d ' ') dirs)"
log_console ""
log_console "Quick triage:"
log_console "  grep FAIL \"$SUMMARY_TSV\""
log_console "  ls \"$FAIL_DIR\""
log_console "  # for a failed run:"
log_console "  cat \"$FAIL_DIR\"/run_*/referrer_screen.txt"
log_console "  cat \"$FAIL_DIR\"/run_*/e2e.log | sed 's/.*ReferrerE2E: //'"
