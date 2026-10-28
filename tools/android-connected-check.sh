#!/usr/bin/env bash
set -euo pipefail
device_log_pid=

# Keep actual rendered frames and device diagnostics even when an assertion fails.
collect_device_evidence() {
  local destination=apps/android/build/reports/device
  mkdir -p "$destination"
  if test -n "$device_log_pid"; then kill "$device_log_pid" 2>/dev/null || true; fi
  adb logcat -d -t 3000 > "$destination/logcat.txt" 2>&1 || true
  for frame in rynx-device-player.png rynx-device-queue.png; do
    if ! adb exec-out run-as dev.bxane.rynx cat "files/$frame" > "$destination/$frame" 2>/dev/null; then
      rm -f "$destination/$frame"
    fi
  done
  python3 - <<'PY'
from pathlib import Path
import html, re
root = Path('apps/android/build/reports/androidTests/connected')
for report in root.rglob('*.html'):
    if report.name in ('index.html',):
        continue
    text = report.read_text(errors='replace')
    for failure in re.findall(r'<pre>([\s\S]*?)</pre>', text):
        message = html.unescape(re.sub(r'<[^>]+>', '', failure))
        message = re.sub(r'https?://\S+', '[URL]', message)
        print(f'ANDROID DIAGNOSTIC {report.name}:\n{message[:6000]}')
PY
}
trap collect_device_evidence EXIT
mkdir -p apps/android/build/reports/device
adb install -r apps/android/build/outputs/apk/debug/android-debug.apk >/dev/null
app_uid=$(adb shell cmd package list packages -U dev.bxane.rynx | python3 -c 'import re,sys; text=sys.stdin.read(); match=re.search(r"package:dev\.bxane\.rynx uid:(\d+)",text); print(match.group(1) if match else "")')
if test -n "$app_uid"; then
  adb logcat -v threadtime --uid="$app_uid" > apps/android/build/reports/device/rynx-process.log 2>&1 &
  device_log_pid=$!
fi
./gradlew :apps:android:connectedDebugAndroidTest "$@"
