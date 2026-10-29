#!/usr/bin/env bash
set -euo pipefail
device_log_pid=

# Keep actual rendered frames and device diagnostics even when an assertion fails.
collect_device_evidence() {
  local test_exit=$?
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
import xml.etree.ElementTree as ET
root = Path('apps/android/build/reports/androidTests/connected')
for report in root.rglob('*.html'):
    if report.name in ('index.html',):
        continue
    text = report.read_text(errors='replace')
    for failure in re.findall(r'<pre>([\s\S]*?)</pre>', text):
        message = html.unescape(re.sub(r'<[^>]+>', '', failure))
        message = re.sub(r'https?://\S+', '[URL]', message)
        print(f'ANDROID DIAGNOSTIC {report.name}:\n{message[:6000]}')
for report_root in (Path('apps/android/build/outputs/androidTest-results/connected'),
                    Path('apps/android/build/outputs/androidTest-results/connectedAndroidTest')):
    reports = report_root.rglob('TEST-*.xml') if report_root.exists() else ()
    for report in reports:
        try:
            root = ET.parse(report).getroot()
        except ET.ParseError:
            continue
        for case in root.iter('testcase'):
            for failure in case.findall('failure') + case.findall('error'):
                message = failure.get('message', '') + '\n' + (failure.text or '')
                message = re.sub(r'https?://\S+', '[URL]', message)
                print(f'ANDROID TEST FAILURE {case.get("classname")}.{case.get("name")}:\n{message[:6000]}')
PY
  if test "$test_exit" -ne 0; then
    for device_log in "$destination/rynx-process.log" "$destination/logcat.txt"; do
      if test -f "$device_log"; then
        printf 'ANDROID SERVICE DIAGNOSTIC (%s):\n' "$(basename "$device_log")"
        grep -F 'RynxResume' "$device_log" | tail -80 || true
      fi
    done
    python3 - <<'PY'
from pathlib import Path
import re

log = Path('apps/android/build/reports/device/logcat.txt')
if not log.exists():
    raise SystemExit(0)
lines = log.read_text(errors='replace').splitlines()
markers = ('FATAL EXCEPTION', 'Process: dev.bxane.rynx', 'ANR in dev.bxane.rynx')
starts = [index for index, line in enumerate(lines) if any(marker in line for marker in markers)]
if not starts:
    print('ANDROID CRASH DIAGNOSTIC: no app fatal-exception marker found in captured logcat')
for index in starts[-4:]:
    block = lines[max(0, index - 3):min(len(lines), index + 45)]
    print('ANDROID CRASH DIAGNOSTIC:\n' + re.sub(r'https?://\S+', '[URL]', '\n'.join(block)))
PY
  fi
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
