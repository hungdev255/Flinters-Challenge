package com.flinters.adagg;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregatorTest {

    @Test
    void aggregatesMultipleRowsPerCampaign() {
        String csv = """
                campaign_id,date,impressions,clicks,spend,conversions
                CMP001,2025-01-01,12000,300,45.50,12
                CMP002,2025-01-01,8000,120,28.00,4
                CMP001,2025-01-02,14000,340,48.20,15
                """;

        Map<String, CampaignTotals> totals = new Aggregator(silent()).aggregate(new StringReader(csv));

        CampaignTotals one = totals.get("CMP001");
        assertEquals(26000, one.impressions);
        assertEquals(640, one.clicks);
        assertEquals(9370, one.spendCents); // 45.50 + 48.20 = 93.70 → 9370 cents (exact)
        assertEquals(27, one.conversions);

        CampaignTotals two = totals.get("CMP002");
        assertEquals(8000, two.impressions);
        assertEquals(120, two.clicks);
        assertEquals(2800, two.spendCents);
        assertEquals(4, two.conversions);
    }

    @Test
    void zeroConversionsYieldsNullCpa() {
        String csv = """
                campaign_id,date,impressions,clicks,spend,conversions
                CMP001,2025-01-01,1000,10,5.00,0
                """;

        Map<String, CampaignTotals> totals = new Aggregator(silent()).aggregate(new StringReader(csv));
        CampaignMetrics m = CampaignMetrics.from("CMP001", totals.get("CMP001"));

        assertNull(m.cpa(), "CPA must be null when conversions are zero");
        assertEquals(0.01, m.ctr(), 1e-9);
    }

    @Test
    void zeroImpressionsYieldsZeroCtrWithoutDivisionByZero() {
        String csv = """
                campaign_id,date,impressions,clicks,spend,conversions
                CMP001,2025-01-01,0,0,1.00,1
                """;

        Map<String, CampaignTotals> totals = new Aggregator(silent()).aggregate(new StringReader(csv));
        CampaignMetrics m = CampaignMetrics.from("CMP001", totals.get("CMP001"));

        assertEquals(0.0, m.ctr());
    }

    @Test
    void skipsMalformedRowsAndContinuesProcessing() {
        String csv = """
                campaign_id,date,impressions,clicks,spend,conversions
                CMP001,2025-01-01,1000,10,5.00,1
                CMP002,2025-01-01,not_a_number,10,5.00,1
                ,2025-01-01,1000,10,5.00,1
                CMP001,2025-01-02,500,5,2.50,0
                """;
        ByteArrayOutputStream warnings = new ByteArrayOutputStream();
        Map<String, CampaignTotals> totals = new Aggregator(new PrintStream(warnings, true, StandardCharsets.UTF_8))
                .aggregate(new StringReader(csv));

        assertEquals(1, totals.size(), "only CMP001's two valid rows should aggregate");
        assertEquals(1500, totals.get("CMP001").impressions);
        assertTrue(warnings.toString().contains("malformed"));
    }

    @Test
    void emptyHeaderOnlyInputProducesEmptyMap() {
        String csv = "campaign_id,date,impressions,clicks,spend,conversions\n";
        Map<String, CampaignTotals> totals = new Aggregator(silent()).aggregate(new StringReader(csv));
        assertTrue(totals.isEmpty());
    }

    @Test
    void currencyAccumulatesExactlyWithoutFloatingPointDrift() {
        // Classic FP-drift case: 0.1 + 0.2 ≠ 0.3 in IEEE-754 double.
        // The long-cents accumulator must give the exact result.
        StringBuilder sb = new StringBuilder("campaign_id,date,impressions,clicks,spend,conversions\n");
        for (int i = 0; i < 1000; i++) {
            sb.append("CMP001,2025-01-01,1,1,0.10,1\n");
            sb.append("CMP001,2025-01-01,1,1,0.20,1\n");
        }

        Map<String, CampaignTotals> totals = new Aggregator(silent()).aggregate(new StringReader(sb.toString()));

        // 1000 × (0.10 + 0.20) = 300.00 → 30_000 cents exactly.
        assertEquals(30_000L, totals.get("CMP001").spendCents);
    }

    @Test
    void garbageInDateColumnDoesNotBreakParsing() {
        // Proves selectFields() is in effect: the parser never tries to
        // interpret the `date` column, so even pure garbage there is fine.
        String csv = """
                campaign_id,date,impressions,clicks,spend,conversions
                CMP001,!@#$%^&*not-a-date,100,5,1.00,1
                """;

        Map<String, CampaignTotals> totals = new Aggregator(silent()).aggregate(new StringReader(csv));

        assertEquals(100, totals.get("CMP001").impressions);
        assertEquals(100, totals.get("CMP001").spendCents);
    }

    private static PrintStream silent() {
        return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
    }
}
