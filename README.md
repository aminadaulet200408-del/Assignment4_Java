# Assignment 4 — Dynamic Programming and String Processing (Java)

A Java 17-compatible implementation of all eight JSON Lines commands required by A4.

## Requirements

- JDK 17 or newer (`javac` and `java` on PATH).
- Python 3.12+ for the course-style launcher (`tools/run.py`).
- No third-party Java libraries.

## Project structure

```text
Assignment4/
├── Java/source/
│   ├── Main.java
│   ├── Json.java
│   ├── Algorithms.java
│   └── Benchmark.java
├── docs/CONTRACTS.md
├── tests/
│   ├── public.json
│   ├── smoke_input.jsonl
│   ├── smoke_test.py
│   └── student/
│       ├── expected.json
│       ├── own_cases.jsonl
│       ├── run_student_tests.py
│       └── search_benchmark.csv
├── tools/
│   ├── run.py
│   └── public_validate.py
├── submission.json
├── ANALYSIS.md
└── README.md
```

## Build and run

From this directory:

```bash
python3 tools/run.py --setup
python3 tools/run.py --check
python3 tests/student/run_student_tests.py
python3 tests/smoke_test.py
```

To send your own JSON Lines file:

```bash
python3 tools/run.py --input tests/student/own_cases.jsonl
```

Or compile/run manually:

```bash
javac --release 17 -encoding UTF-8 -d Java/build Java/source/*.java
java -Xmx384m -cp Java/build Main < tests/student/own_cases.jsonl
```

Each non-empty input line is one JSON request. The program prints exactly one JSON result per request; diagnostics do not go to standard output.

## Benchmark

The search comparison is implemented in `Java/source/Benchmark.java`:

```bash
javac --release 17 -encoding UTF-8 -d Java/build Java/source/*.java
java -Xmx384m -cp Java/build Benchmark
```

The recorded measurements and methodology are in `ANALYSIS.md` and `tests/student/search_benchmark.csv`.
