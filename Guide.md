# Reviewer's Guide

A short, operational walkthrough for running this project end-to-end and
finding the things you most likely want to evaluate.

Jump to [Quick start](#quick-start) for a 30-second smoke test, or
[I just want to see the results](#i-just-want-to-see-the-results) to skip
running altogether.

---

## Prerequisites

Choose one of the two paths:

| Path        | What you need                               |
|-------------|---------------------------------------------|
| Native Java | JDK 21 (Temurin / Corretto / Zulu)          |
| Docker      | Docker (any recent version)                 |

The dataset (`ad_data.csv.zip`, ~360 MB compressed → ~995 MB unzipped) ships
in this repo. Unzip it once before running:

```powershell
# Windows PowerShell
Expand-Archive -Path .\ad_data.csv.zip -DestinationPath .
```

```bash
# macOS / Linux
unzip ad_data.csv.zip
```

---

## Quick start

The pre-built `dist/aggregator.jar` (~460 KB) is committed, so you do
**not** need Maven — only a JDK 21.

### Windows — one-liner scripts (simplest)

Two convenience scripts ship with the repo. They auto-locate a JDK 21 from
common install paths (Corretto, Temurin, etc.), unzip the dataset if needed,
and run the jar.

**PowerShell:**

```powershell
.\run.ps1
```

**Command Prompt (cmd.exe):**

```cmd
run.cmd
```

Both scripts prefer `target\aggregator.jar` if a fresh build exists, and
fall back to the committed `dist\aggregator.jar` automatically.

### Windows / macOS / Linux — manual (java on PATH)

```powershell
java -XX:+UseG1GC -Xmx128m -jar dist\aggregator.jar --input ad_data.csv --output results
```

### Build from source

```bash
mvn -B clean package      # runs all 12 tests, produces target/aggregator.jar
java -XX:+UseG1GC -Xmx128m -jar target/aggregator.jar --input ad_data.csv --output results
```

### Expected output

Whichever path you use, the program prints to stderr:

```
Processing: ad_data.csv
Written:    results/top10_ctr.csv
            results/top10_cpa.csv
Campaigns:  50
Time:       <wall clock — depends on CPU/SSD> | Peak heap: ~107 MB
```

Peak heap stays in that ballpark under `-Xmx128m`; wall time differs by machine
(Author's bench: Ryzen 7 9700X / 32 GB — see [README.md → Performance](README.md#performance).)

The two output CSVs land in `results/`.

### Docker

**With docker compose (simplest):**

```bash
docker compose run --rm aggregator
```

**Or manually:**

```bash
docker build -t ad-aggregator .
docker run --rm -v "$PWD:/data" ad-aggregator \
    --input /data/ad_data.csv \
    --output /data/results
```

The image's entrypoint pins `-XX:+UseG1GC -Xmx128m`.

> **Note:** On Docker Desktop for Windows, volume I/O between the Windows
> host and the Linux container adds overhead. Expect on the order of ~50 s
> wall time in Docker vs a few seconds natively — peak heap stays the same
> (~107 MB), proving the logic is the same.

### I just want to see the results

`results/top10_ctr.csv` and `results/top10_cpa.csv` are already committed
from the benchmark run. Open them directly to see the expected output shape.

`benchmarks/run-1gb.log` mirrors the latest measured run (3.15 s @ 106.8 MB
peak heap on `-Xmx128m`; 3.42 s @ 60.8 MB on `-Xmx64m` — same host as README
Performance).

---

## Run the test suite

```bash
mvn -B test
```

12 tests across 3 classes. Should pass in ~1 second.

| Test class           | Focus                                                                               |
|----------------------|-------------------------------------------------------------------------------------|
| `AggregatorTest`     | Aggregation correctness, edge cases, exact `long`-cents accumulation, `selectFields` proof. |
| `ReportWriterTest`   | Locale-stable formatting (forces `Locale.GERMANY` to confirm `Locale.ROOT` works), null-CPA blank column. |
| `AppEndToEndTest`    | Full `App.run` invocation in `@TempDir`, file existence, ordering, missing-input + bad-args handling. |

---

## Where to look when reviewing

Code recommended in reading order:

1. **`Aggregator.java`** — the heart of the solution. Note `selectFields` (skips the `date` column), the `BigDecimal → long cents` parse, and the malformed-row handling.
2. **`CampaignTotals.java`** — the primitive-only mutable accumulator. Hot path never boxes.
3. **`CampaignMetrics.java`** — derived CTR/CPA. Note CPA is `Double` (nullable) and `totalSpend()` returns a `BigDecimal` for exact 2dp output.
4. **`ReportWriter.java`** — every `String.format` uses `Locale.ROOT`.
5. **`App.java`** — CLI parsing, sort + top-10, peak-heap reporting via `MemoryPoolMXBean`.

The three senior-level choices that distinguish this from a passing solution
are summarised in [README.md → Design decisions](README.md#design-decisions).

---

## Optional: shrink the heap to prove memory-efficiency

```bash
java -XX:+UseG1GC -Xmx64m -jar dist/aggregator.jar \
    --input ad_data.csv \
    --output results/
```

Runs to completion. ~8% slower than the 128 MB run on the author's machine
due to more frequent GC cycles, but the working set fits comfortably.

---

## Troubleshooting

**`run.ps1` is blocked by execution policy**

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\run.ps1
```

**`java: command not found`** — JDK 21 isn't on PATH. Use `run.ps1` / `run.cmd`
(they auto-locate it), or set it manually:

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

**`Error: input file not found: ad_data.csv`** — unzip `ad_data.csv.zip` first.

**Decimal commas in output** (e.g. `45,50` instead of `45.50`) — should never
happen because `ReportWriter` uses `Locale.ROOT`. If you see it, please flag
it; that's a bug.

---

## Repo map

```
.
├── pom.xml                       — Maven build (Java 21, Univocity, JUnit 5, shade)
├── Dockerfile                    — Multi-stage build → eclipse-temurin:21-jre-alpine
├── docker-compose.yml            — One-shot compose: docker compose run --rm aggregator
├── run.ps1                       — Windows PowerShell convenience launcher
├── run.cmd                       — Windows cmd.exe convenience launcher
├── .github/workflows/ci.yml      — mvn verify + Docker build on every push/PR
├── README.md                     — Full project documentation
├── Guide.md                      — This file
├── CHALLENGE.md                  — Original Flinters brief
├── PROMPTS.md                    — Raw, unedited AI prompts (per the brief)
├── benchmarks/run-1gb.log        — Timed runs against the 1GB dataset
├── results/                      — Generated top-10 CSVs (committed)
├── dist/aggregator.jar           — Pre-built jar (no Maven required to run)
├── ad_data.csv.zip               — Source dataset (unzip before running)
└── src/
    ├── main/java/com/flinters/adagg/
    │   ├── App.java
    │   ├── Aggregator.java
    │   ├── CampaignTotals.java
    │   ├── CampaignMetrics.java
    │   └── ReportWriter.java
    └── test/java/com/flinters/adagg/
        ├── AggregatorTest.java
        ├── AppEndToEndTest.java
        └── ReportWriterTest.java
```

---

## Contact

Submitted by **hungnm2505.dev@gmail.com** for the Flinters FV-SEC001 challenge.
