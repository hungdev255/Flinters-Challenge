# PROMPTS

Raw, unedited prompts from the AI-assisted session that produced this solution.
Pasted verbatim — exactly as typed — per the challenge instructions.

Assistant used: Claude Opus 4.7 (1M context) via Claude Code CLI.

---

## Prompt 1

> This is my Java interview test, Flinters give me this document, please read the readme.md first and follow it rule.
>   Make a plan for this for me. Tell me anything you will do and which rule you will follows.

## Prompt 2

> The plan looks incredibly solid and covers all the requirements perfectly. Let's proceed to the implementation phase. However, before you generate the code, I want to elevate this from a "passing" solution to an "outstanding" senior-level Java implementation. Please integrate the following 3 critical optimizations into your proposed architecture:1. Zero-Allocation Parsing for Unused Data: The requirements do not use the `date` column for the final outputs. Configure Univocity Parsers (e.g., using `selectFields` or `selectIndexes`) to completely ignore the `date` column. This will prevent the JVM from allocating millions of useless String objects, significantly reducing GC overhead and boosting I/O throughput.2. Production-Grade Currency Handling: I noticed the plan suggested using `double` for `total_spend` to match the Python reference. In Java, using floating-point types for currency is a major anti-pattern due to precision loss. Please change the `spend` field in `CampaignTotals` to `long` (storing cents/micros) or `BigDecimal`. Only format it back to standard decimal (%.2f) at the very end when writing the CSV. We will document this deviation from the Python script as a deliberate best-practice decision.3. Flexing Memory Efficiency: Since we are using streaming and primitive wrappers, the memory footprint will be tiny. Let's optimize the code and adjust our Dockerfile plan to run comfortably with `-Xmx128m` (or even `-Xmx64m`). This will clearly demonstrate to the reviewers that the solution is highly memory-efficient for a 1GB file.With these 3 strict rules added to our plan, please go ahead and generate the complete Java source code for the main application and core logic. Please provide:- App.java (with CLI parsing and memory/time logging)- Aggregator.java (with the Univocity streaming logic)- CampaignTotals.java & CampaignMetrics.java (Data structures)- ReportWriter.java (Output formatting with Locale.ROOT)

## Prompt 3

> The generated code looks absolutely fantastic. The integration of the three optimizations (zero-allocation parsing for the date column, primitive long cents for currency, and primitive-based accumulator) is exactly the senior-level quality I am aiming for.
>
> Please proceed with the rest of the plan: generating the Tests, Dockerfile, CI pipeline, and README.md.

## Prompt 4

> This is absolutely perfect. The deliverables are exceptionally well-structured, and the CI/CD plus Docker configurations reflect exactly the senior-level quality we need.
>
> Yes, please take it from here and complete Task #4. You have my full authorization to execute commands on this machine.
>
> Here is your final checklist:
> 1. Verify if JDK 21 and Maven are available. If they are, build the project (`mvn clean package`).
> 2. Run the actual 1GB benchmark using the downloaded dataset (`ad_data.csv`).
> 3. Capture the exact execution time and peak memory usage, and update the placeholder values in the `README.md`.
> 4. Generate the final real `results/top10_ctr.csv` and `results/top10_cpa.csv`.
> 5. Clean up the repository: DELETE `aggregator.py`, `tests/__init__.py`, and any other unnecessary Python or temp files.
> 6. Append this current prompt to `PROMPTS.md`.
>
> Once you are done, print a final success message summarizing the benchmark results (Time & Memory). Let's finish strong!
