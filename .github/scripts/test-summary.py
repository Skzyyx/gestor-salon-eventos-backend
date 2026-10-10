#!/usr/bin/env python3
"""Summarise the Surefire XML reports as Markdown for $GITHUB_STEP_SUMMARY.

Usage: python3 .github/scripts/test-summary.py target/surefire-reports
Only the standard library is used, so it runs on any GitHub-hosted runner.
"""
import glob
import html
import os
import sys
import xml.etree.ElementTree as ET

report_dir = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports"
files = sorted(glob.glob(os.path.join(report_dir, "TEST-*.xml")))

if not files:
    print("### Tests not run\n")
    print("No Surefire reports were found: the build failed before the tests ran "
          "(compilation error or dependency resolution). Read the log of the previous step.")
    sys.exit(0)

total = failures = errors = skipped = 0
seconds = 0.0
failed = []
for path in files:
    suite = ET.parse(path).getroot()
    total += int(suite.get("tests", 0))
    failures += int(suite.get("failures", 0))
    errors += int(suite.get("errors", 0))
    skipped += int(suite.get("skipped", 0))
    seconds += float(suite.get("time", 0) or 0)
    for case in suite.iter("testcase"):
        for kind in ("failure", "error"):
            node = case.find(kind)
            if node is None:
                continue
            lines = (node.get("message") or node.get("type") or "").strip().splitlines()
            message = lines[0][:240] if lines else ""
            failed.append((case.get("classname", "").rsplit(".", 1)[-1], case.get("name", ""), kind, message))

passed = total - failures - errors - skipped
print(f"### Tests {'passed' if not failed else 'FAILED'}\n")
print("| Total | Passed | Failed | Errors | Skipped | Time |")
print("| ---: | ---: | ---: | ---: | ---: | ---: |")
print(f"| {total} | {passed} | {failures} | {errors} | {skipped} | {seconds:.1f} s |")

if failed:
    print("\n| Test class | Test | Kind | Message: expected vs actual |")
    print("| :--- | :--- | :--- | :--- |")
    for cls, name, kind, message in failed:
        # html.escape keeps <expected> and <actual> visible; | would break the table.
        cell = html.escape(message).replace("|", "\\|")
        print(f"| {html.escape(cls)} | {html.escape(name)} | {kind} | {cell} |")