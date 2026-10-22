#!/usr/bin/env python3
"""Summarise the JaCoCo CSV report as Markdown for $GITHUB_STEP_SUMMARY.

Usage: python3 .github/scripts/coverage-summary.py target/site/jacoco/jacoco.csv
Only the standard library is used, so it runs on any GitHub-hosted runner.
The 80% minimum is enforced by the coverage-gate execution in pom.xml; this
script only reports the numbers.
"""
import csv
import os
import sys

# UTF-8 even on a Windows console, so the check marks below never crash a local run.
sys.stdout.reconfigure(encoding="utf-8")

MINIMUM = 80.0
path = sys.argv[1] if len(sys.argv) > 1 else "target/site/jacoco/jacoco.csv"

if not os.path.exists(path):
    print("### Coverage not measured\n")
    print("No JaCoCo report was found: the tests did not run. Read the log of the build step.")
    sys.exit(0)

with open(path, newline="", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))


def percent(covered, missed):
    total = covered + missed
    return 100.0 * covered / total if total else 100.0


def total(counter):
    covered = sum(int(r[f"{counter}_COVERED"]) for r in rows)
    missed = sum(int(r[f"{counter}_MISSED"]) for r in rows)
    return covered, missed


print("### Coverage\n")
print("| Counter | Covered | Total | Coverage | Minimum |")
print("| :--- | ---: | ---: | ---: | ---: |")
for label, counter, gated in (("Lines", "LINE", True), ("Branches", "BRANCH", True),
                              ("Instructions", "INSTRUCTION", False)):
    covered, missed = total(counter)
    value = percent(covered, missed)
    mark = "" if not gated else (" ✅" if value >= MINIMUM else " ❌")
    minimum = f"{MINIMUM:.0f} %" if gated else "-"
    print(f"| {label} | {covered} | {covered + missed} | {value:.1f} %{mark} | {minimum} |")

# Where the next tests would pay off most: classes with the most uncovered lines.
pending = sorted(rows, key=lambda r: int(r["LINE_MISSED"]), reverse=True)
pending = [r for r in pending if int(r["LINE_MISSED"]) > 0][:5]
if pending:
    print("\n| Class with most uncovered lines | Uncovered lines | Line coverage |")
    print("| :--- | ---: | ---: |")
    for r in pending:
        name = r["PACKAGE"].rsplit(".", 1)[-1] + "." + r["CLASS"]
        value = percent(int(r["LINE_COVERED"]), int(r["LINE_MISSED"]))
        print(f"| {name} | {r['LINE_MISSED']} | {value:.1f} % |")
