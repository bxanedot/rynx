#!/usr/bin/env python3
"""Update the approved stable VyonEx pin without modifying upstream sources."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent
DEPENDENCY = ROOT / 'dependencies/vyonex-clean'

def git(*args):
    return subprocess.check_output(['git', '-C', str(DEPENDENCY), *args], text=True).strip()

def main():
    if git('status', '--porcelain'):
        raise SystemExit('Refusing to replace a modified VyonEx checkout')
    old = git('rev-parse', 'HEAD')
    git('fetch', 'origin', 'main')
    new = git('rev-parse', 'origin/main')
    if old == new:
        print('Stable VyonEx is current: ' + old)
        return
    subprocess.run(['git', '-C', str(DEPENDENCY), 'merge-base', '--is-ancestor', old, new], check=True)
    git('checkout', '--detach', new)
    for name in ('tools/audit_workspace.py', 'docs/cleanroom/DEPENDENCY_AUDIT.md',
                 'docs/parity/VYONEX_INTEGRATION.md'):
        path = ROOT / name
        path.write_text(path.read_text().replace(old, new))
    print('Updated stable VyonEx: ' + old + ' -> ' + new)

if __name__ == '__main__':
    main()
