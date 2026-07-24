#!/usr/bin/env bash
# Runs the referrer E2E test N times, removing the SDK app before each run.
# Usage: ANDROID_SERIAL=emulator-5554 ./run_10x.sh [N]
set -u

N="${1:-10}"
TARGET="com.farsitel.bazaar.bazaarInstallReferrerTest"
RUNNER="com.farsitel.bazaar.referrere2e.test/androidx.test.runner.AndroidJUnitRunner"
OUT=/tmp/e2e_10x
mkdir -p "$OUT"

pass=0; fail=0
for i in $(seq 1 "$N"); do
  echo "==================== RUN $i / $N ===================="
  adb shell pm uninstall "$TARGET" >/dev/null 2>&1
  adb shell am force-stop com.android.chrome >/dev/null 2>&1
  adb shell input keyevent KEYCODE_HOME >/dev/null 2>&1
  adb logcat -c >/dev/null 2>&1
  adb logcat -s ReferrerE2E:I > "$OUT/log_$i.txt" 2>&1 &
  LC=$!
  adb shell am instrument -w -r "$RUNNER" > "$OUT/inst_$i.txt" 2>&1
  kill "$LC" 2>/dev/null

  if grep -q "OK (1 test)" "$OUT/inst_$i.txt"; then
    ref=$(grep -o "SUCCESS: referrer on screen = .*" "$OUT/log_$i.txt" | tail -1)
    echo "RUN $i: PASS  | $ref"
    pass=$((pass+1))
  else
    reason=$(grep -o "AssertionError: .*" "$OUT/inst_$i.txt" | head -1)
    echo "RUN $i: FAIL  | $reason"
    fail=$((fail+1))
  fi
done

echo "==================== SUMMARY ===================="
echo "PASS=$pass  FAIL=$fail  (of $N)"
