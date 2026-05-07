# Ad Performance Aggregator (Java)

Streaming CLI that aggregates a ~1GB ad-performance CSV per `campaign_id` and
emits two reports: top-10 CTR and lowest-10 CPA.

This is my submission for the Flinters **FV-SEC001 Software Engineer
Challenge**. The original brief is preserved in [CHALLENGE.md](CHALLENGE.md).
Raw AI-assistant prompts used while building this are in
[PROMPTS.md](PROMPTS.md).

## Highlights

- **Streaming** parse over the 1GB CSV — heap stays bounded by *unique
  campaigns*, not file size. Verified to run under `-Xmx128m` (and
  `-Xmx64m` in the stress benchmark).
- **Zero-allocation for unused columns** — Univocity's `selectFields(...)`
  skips the `date` column entirely, so the parser never allocates a String
  for it. This drops tens of millions of allocations on a 1GB file.
- **Exact currency** — `total_spend` is parsed once per row through
  `BigDecimal` into `long` cents and accumulated as a primitive. No IEEE-754
  drift on monetary values, no per-row `BigDecimal` allocation on the hot
  path.
- **Locale-stable output** — every numeric format uses `Locale.ROOT`, so the
  decimal separator is always `.` regardless of the host JVM locale.
- **Graceful errors** — missing input → exit 1 with a clear stderr message;
  malformed rows → skipped with a capped warning stream.

## Prerequisites

- JDK 21 (Temurin recommended)
- Maven 3.9+
- (Optional) Docker, if you'd rather not install Java locally

## Build

```bash
mvn -B package
```

Produces a self-contained `target/aggregator.jar`.

## Run

Unzip the dataset first if you haven't already:

```bash
unzip ad_data.csv.zip
```

A pre-built jar is committed at `dist/aggregator.jar`, so you can run
without building first — only a JDK 21 is required:

```bash
java -XX:+UseG1GC -Xmx128m -jar dist/aggregator.jar \
    --input ad_data.csv \
    --output results/
```

If you built from source instead, use `target/aggregator.jar`.

Outputs:

- `results/top10_ctr.csv` — top 10 campaigns by CTR (highest first)
- `results/top10_cpa.csv` — top 10 campaigns by CPA (lowest first; campaigns with zero conversions are excluded)

stderr will print the elapsed time and peak heap usage at the end of the run.

### Run via Docker

```bash
docker build -t ad-aggregator .
docker run --rm -v "$PWD:/data" ad-aggregator \
    --input /data/ad_data.csv \
    --output /data/results
```

The container's entrypoint pins `-Xmx128m`.

## Tests

```bash
mvn -B test
```

The suite covers happy-path aggregation, zero-conversion CPA handling,
zero-impression CTR handling, malformed-row skipping, empty input,
locale-stable formatting, exact currency accumulation across the classic
`0.1 + 0.2 ≠ 0.3` floating-point trap, and a check that garbage in the
unused `date` column doesn't break parsing (proving `selectFields` is
active).

## Libraries

- [`com.univocity:univocity-parsers`](https://github.com/uniVocity/univocity-parsers) — fast, low-allocation CSV parser
- [`org.junit.jupiter:junit-jupiter`](https://junit.org/junit5/) — tests

No other runtime dependencies.

## Performance

Measured on the provided `ad_data.csv` (1,043,304,870 bytes / ~995 MB) on
Windows 11, AMD Ryzen 7 9700X, 32 GB RAM, JDK 21 Temurin.

| JVM flags                    | Wall time | Peak heap |
|------------------------------|-----------|-----------|
| `-XX:+UseG1GC -Xmx128m`      | **3.15 s** | **106.8 MB** |
| `-XX:+UseG1GC -Xmx64m`       | **3.42 s** | **60.8 MB**  |

50 unique campaigns aggregated from ~50M+ rows. The `-Xmx64m` run
completed successfully — proof that the streaming + primitive-accumulator
design keeps the working set well under the documented 128MB operating
ceiling. The ~8% wall-time penalty at `-Xmx64m` is just more frequent GC
cycles under a tighter heap.

## Design decisions

A few non-obvious choices, documented for the reviewer:

1. **Currency stored as `long` cents, not `double`.** The Python reference
   uses `double`, but in Java floating-point money is a well-known
   anti-pattern: `0.1 + 0.2 != 0.3`. I parse each spend cell once through
   `BigDecimal` (exact) and accumulate as `long` cents. This is the
   canonical Java pattern and is also faster than per-row `BigDecimal`
   accumulation. Conversion back to a 2dp value happens only at CSV write
   time via `BigDecimal.valueOf(cents, 2)`.

2. **`selectFields` to skip the unused `date` column.** The output reports
   never reference `date`, so I tell Univocity to skip parsing it entirely.
   On a 1GB file with ~50–100M rows, that is tens of millions of String
   allocations avoided and noticeably less GC pressure.

3. **Full sort over the *aggregated* map for top-10.** The number of unique
   campaigns is small relative to the row count, so a `stream().sorted()
   .limit(10)` over the aggregated map is simpler and fast enough. A bounded
   heap would only help if cardinality grew into the millions.

4. **Single-threaded streaming.** A shard-then-merge parallel pipeline could
   improve wall time on multi-core machines, but the I/O-bound nature of the
   workload and the simplicity of correctness analysis weren't worth the
   complexity for this brief. Documented here as a deliberate tradeoff.

5. **Hand-rolled CLI parsing.** Two flags (`--input`, `--output`) — not
   worth pulling in picocli or commons-cli.

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `mvn verify` on
JDK 21 (Temurin) for every push and pull request. The built jar is uploaded
as an artifact on success.

## Project layout

```
.
├── pom.xml
├── Dockerfile
├── .github/workflows/ci.yml
├── README.md           — this file
├── CHALLENGE.md        — original Flinters brief
├── PROMPTS.md          — raw AI prompts used during development
├── benchmarks/         — recorded runs against the 1GB dataset
├── results/            — generated top-10 CSVs
└── src/
    ├── main/java/com/flinters/adagg/
    │   ├── App.java              — CLI entry, time + peak-heap reporting
    │   ├── Aggregator.java       — streaming Univocity parse + accumulate
    │   ├── CampaignTotals.java   — primitive-field accumulator
    │   ├── CampaignMetrics.java  — derived CTR/CPA
    │   └── ReportWriter.java     — locale-stable CSV output
    └── test/java/com/flinters/adagg/
        ├── AggregatorTest.java
        ├── ReportWriterTest.java
        └── AppEndToEndTest.java
```
