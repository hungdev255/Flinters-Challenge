# FV-SEC001 - Software Engineer Challenge — Ad Performance Aggregator

> This file preserves the original challenge brief from Flinters. The
> candidate-facing project documentation lives in [README.md](README.md).

## Introduction
This is a data processing challenge for Developer candidates applying to our company.
You will work with a large CSV dataset (~1GB) containing advertising performance records.

The goal is to evaluate your ability to write clean code, handle large datasets efficiently, optimize performance/memory usage, and design a robust data-processing workflow.

---

## Input Data

### Download the Dataset

1. Download the `ad_data.csv.zip` file from this repository folder
2. Unzip it to get the `ad_data.csv` file (~1GB)
3. Use this CSV file for your solution

```bash
# Example: Unzip the file
unzip ad_data.csv.zip
```

### CSV Schema

| Column         | Type      | Description |
|----------------|-----------|-------------|
| campaign_id    | string    | Campaign ID |
| date           | string    | Date in `YYYY-MM-DD` format |
| impressions    | integer   | Number of impressions |
| clicks         | integer   | Number of clicks |
| spend          | float     | Advertising cost (USD) |
| conversions    | integer   | Number of conversions |

---

# Task Requirements

You must build a console application (CLI) in any programming language (Python, NodeJS, Go, Java, Rust, etc.) that processes the CSV file and produces aggregated analytics.

## 1. Aggregate data by `campaign_id`

For each `campaign_id`, compute:

- `total_impressions`
- `total_clicks`
- `total_spend`
- `total_conversions`
- `CTR` = total_clicks / total_impressions
- `CPA` = total_spend / total_conversions
  - If conversions = 0, ignore or return `null` for CPA

## 2. Generate two result lists

- **A. Top 10 campaigns with the highest CTR** → `top10_ctr.csv`
- **B. Top 10 campaigns with the lowest CPA** (excluding conversions = 0) → `top10_cpa.csv`

## 3. Technical Requirements

- The file is large (~1GB). Solution must handle large datasets efficiently with good memory/performance.
- The program must be runnable via CLI, e.g. `python aggregator.py --input ad_data.csv --output results/`.

## Submission

Submit a GitHub repository link via email to **backoffice@flinters.vn** containing:
1. Source code
2. Output files (`top10_ctr.csv`, `top10_cpa.csv`)
3. README with setup, run instructions, libraries, processing time, peak memory
4. (Optional) Dockerfile, benchmark logs
5. (If AI used) `PROMPTS.md` with raw, unedited prompts

## Code Quality Expectations
- Correct results, clean code, error handling, performance awareness, tests, documented decisions.
