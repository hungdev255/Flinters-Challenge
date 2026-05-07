package com.flinters.adagg;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportWriterTest {

    @Test
    void formatsHeaderAndRowWithLocaleStableDecimalSeparator() throws Exception {
        // Force a locale that uses ',' as the decimal separator. Locale.ROOT
        // formatting in ReportWriter must override this and emit '.'.
        Locale prev = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            CampaignMetrics m = new CampaignMetrics(
                    "CMP001", 12000, 300, 4550, 12, 0.025, 3.7916666);

            StringWriter out = new StringWriter();
            ReportWriter.write(out, List.of(m));

            String[] lines = out.toString().split("\n");
            assertEquals(
                    "campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA",
                    lines[0]);
            assertEquals("CMP001,12000,300,45.50,12,0.0250,3.79", lines[1]);
        } finally {
            Locale.setDefault(prev);
        }
    }

    @Test
    void cpaColumnIsBlankWhenNull() {
        CampaignMetrics m = new CampaignMetrics(
                "CMP002", 1000, 10, 500, 0, 0.01, null);

        assertEquals("CMP002,1000,10,5.00,0,0.0100,", ReportWriter.formatRow(m));
    }
}
