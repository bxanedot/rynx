#!/usr/bin/env python3
"""Review the allowed source boundary; this is a repository check, not an OS sandbox."""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
APPROVED = {
    "dependencies/vyonex-clean": ("https://github.com/bxanedot/vyonex-clean", "239a8d056c90b2101bf04de27a639d591acc69a3"),
    "dependencies/GlyphStudio": ("https://github.com/bxanedot/GlyphStudio", "7db5de2e81c6687f9e3b483eeaac43a145e9704c"),
}

def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args], text=True).strip()

def main():
    paths = git("config", "--file", ".gitmodules", "--get-regexp", r"submodule\..*\.path").splitlines()
    if {line.split(None, 1)[1] for line in paths} != set(APPROVED):
        raise ValueError("Unapproved source dependency path")
    for line in paths:
        key, path = line.split(None, 1)
        expected_url, expected_commit = APPROVED[path]
        url = git("config", "--file", ".gitmodules", "--get", key[:-4] + "url").removesuffix(".git")
        if url != expected_url:
            raise ValueError("Unapproved source dependency URL")
        if git("-C", path, "rev-parse", "HEAD") != expected_commit:
            raise ValueError("Unreviewed dependency revision: " + path)
        if git("-C", path, "status", "--porcelain", "--untracked-files=no"):
            raise ValueError("Approved dependency has tracked modifications: " + path)
    for base in ("apps", "shared", "providers", "services"):
        for file in (ROOT / base).rglob("*"):
            if file.is_symlink() and not file.resolve().is_relative_to(ROOT):
                raise ValueError("Implementation symlink crosses workspace boundary")
    print("PASS: approved dependency URLs/revisions, unchanged checkouts, workspace symlinks")
    print("This does not prove source provenance outside the repository; review PROVENANCE.md.")

if __name__ == "__main__":
    try:
        main()
    except (ValueError, subprocess.CalledProcessError) as error:
        print("FAIL: " + str(error), file=sys.stderr)
        sys.exit(1)
