package com.flinters.adagg;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Streams the ad-performance CSV row by row and accumulates per-campaign
 * totals. Memory is proportional to the number of unique campaigns, never to
 * the file size:
 *
 * <ul>
 *   <li>Univocity's {@code parseNext()} loop yields one row at a time.</li>
 *   <li>Only the five columns we need are selected; the {@code date} column
 *       is never parsed, eliminating millions of String allocations.</li>
 *   <li>Spend is parsed once per row through {@link BigDecimal} into exact
 *       {@code long} cents, then accumulated as a primitive — no FP drift.</li>
 *   <li>Malformed rows are skipped with a capped warning stream so a single
 *       bad line cannot abort a 1GB run.</li>
 * </ul>
 */
public final class Aggregator {

    private static final String[] SELECTED_FIELDS = {
            "campaign_id", "impressions", "clicks", "spend", "conversions"
    };

    private static final int MAX_LOUD_WARNINGS = 10;

    private final PrintStream warnings;

    public Aggregator() {
        this(System.err);
    }

    public Aggregator(PrintStream warnings) {
        this.warnings = warnings;
    }

    public Map<String, CampaignTotals> aggregate(Path inputFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8)) {
            return aggregate(reader);
        }
    }

    public Map<String, CampaignTotals> aggregate(Reader reader) {
        Map<String, CampaignTotals> totals = new HashMap<>();
        CsvParser parser = newParser();
        parser.beginParsing(reader);

        long lineNo = 1; // 1 = header, parsed rows start at 2
        long skipped = 0;
        String[] row;

        while ((row = parser.parseNext()) != null) {
            lineNo++;
            try {
                String campaignId = requireNonBlank(row[0], "campaign_id");
                long impressions = Long.parseLong(row[1]);
                long clicks = Long.parseLong(row[2]);
                long spendCents = parseCents(row[3]);
                long conversions = Long.parseLong(row[4]);

                totals.computeIfAbsent(campaignId, k -> new CampaignTotals())
                      .add(impressions, clicks, spendCents, conversions);
            } catch (RuntimeException ex) {
                skipped++;
                if (skipped <= MAX_LOUD_WARNINGS) {
                    warnings.printf("  Warning: skipping malformed row %d: %s%n", lineNo, ex.getMessage());
                }
            }
        }

        parser.stopParsing();

        if (skipped > MAX_LOUD_WARNINGS) {
            warnings.printf("  ... %d malformed rows skipped in total%n", skipped);
        } else if (skipped > 0) {
            warnings.printf("  %d malformed row(s) skipped%n", skipped);
        }

        return totals;
    }

    private static CsvParser newParser() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        settings.setLineSeparatorDetectionEnabled(true);
        settings.setMaxCharsPerColumn(4096);
        // Skip the `date` column entirely — never read downstream. This drops
        // tens of millions of String allocations on a 1GB file.
        settings.selectFields(SELECTED_FIELDS);
        return new CsvParser(settings);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + field);
        }
        return value.strip();
    }

    private static long parseCents(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("missing spend");
        }
        return new BigDecimal(raw).movePointRight(2).longValueExact();
    }
}
