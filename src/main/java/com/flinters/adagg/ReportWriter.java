package com.flinters.adagg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Writes top-N campaign metrics as CSV. All numeric formatting uses
 * {@link Locale#ROOT} so the decimal separator is always {@code '.'}
 * regardless of the host JVM's locale.
 */
final class ReportWriter {

    private static final String HEADER =
            "campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA";

    private ReportWriter() {}

    static void write(Path output, List<CampaignMetrics> rows) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            write(w, rows);
        }
    }

    static void write(Writer w, List<CampaignMetrics> rows) throws IOException {
        w.write(HEADER);
        w.write('\n');
        for (CampaignMetrics m : rows) {
            w.write(formatRow(m));
            w.write('\n');
        }
    }

    static String formatRow(CampaignMetrics m) {
        String cpa = m.cpa() == null
                ? ""
                : String.format(Locale.ROOT, "%.2f", m.cpa());
        return String.format(
                Locale.ROOT,
                "%s,%d,%d,%.2f,%d,%.4f,%s",
                m.campaignId(),
                m.totalImpressions(),
                m.totalClicks(),
                m.totalSpend(),
                m.totalConversions(),
                m.ctr(),
                cpa);
    }
}
