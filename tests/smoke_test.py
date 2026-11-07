#!/usr/bin/env python3

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "out"

subprocess.run(
    ["javac", "--release", "17", "-d", str(OUT), *map(str, (ROOT / "Java/source").glob("*.java"))],
    check=True,
)
run = subprocess.run(
    ["java", "-cp", str(OUT), "Main"],
    input=(ROOT / "tests/smoke_input.jsonl").read_text(encoding="utf-8"),
    text=True,
    capture_output=True,
    check=True,
)
if run.stderr.strip():
    print("Unexpected stderr:", run.stderr, file=sys.stderr)
    sys.exit(1)
actual = [json.loads(line) for line in run.stdout.splitlines()]
expected = [
    {"length": 4, "sequence": "team"},
    {"cost": 14, "plan": [[0, 0, 1], [0, 1, 2]]},
    {"weight": 10, "selected": [0, 2]},
    {"weight": 0, "selected": []},
    {"positions": [0, 1, 2]},
    {"positions": [0, 2, 4]},
    {"positions": [0]},
    {"sa": [5, 3, 1, 0, 4, 2], "lcp": [0, 1, 3, 0, 0, 2], "repeated": "ana"},
    {"counts": [[2, 2, 2], [2, 2, 2], [0, 0, 0]]},
    {"positions": [0, 1, 2, 3]},
    {"sa": [], "lcp": [], "repeated": ""},
    {"counts": [[], []]},
]
assert actual == expected, f"Mismatch\nExpected: {expected}\nActual:   {actual}"
print(f"PASS: {len(expected)} JSON Lines smoke cases")
