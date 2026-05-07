package com.flinters.adagg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppEndToEndTest {

    @Test
    void runsEndToEndAndProducesBothReports(@TempDir Path tmp) throws Exception {
        Path input = tmp.resolve("ad_data.csv");
        String csv = """
                campaign_id,date,impressions,clicks,spend,conversions
                CMP001,2025-01-01,1000,100,10.00,10
                CMP002,2025-01-01,1000,50,10.00,5
                CMP003,2025-01-01,1000,200,10.00,0
                """;
        Files.writeString(input, csv, StandardCharsets.UTF_8);

        Path outDir = tmp.resolve("out");

        App.run(new String[]{"--input", input.toString(), "--output", outDir.toString()});

        Path ctrFile = outDir.resolve("top10_ctr.csv");
        Path cpaFile = outDir.resolve("top10_cpa.csv");
        assertTrue(Files.exists(ctrFile), "top10_ctr.csv must be written");
        assertTrue(Files.exists(cpaFile), "top10_cpa.csv must be written");

        List<String> ctr = Files.readAllLines(ctrFile);
        // header + 3 data rows
        assertEquals(4, ctr.size());
        // CMP003 has the highest CTR (200/1000 = 0.20) → first
        assertTrue(ctr.get(1).startsWith("CMP003,"));
        assertTrue(ctr.get(2).startsWith("CMP001,"));
        assertTrue(ctr.get(3).startsWith("CMP002,"));

        List<String> cpa = Files.readAllLines(cpaFile);
        // header + 2 data rows (CMP003 has 0 conversions and is excluded)
        assertEquals(3, cpa.size());
        assertTrue(cpa.get(1).startsWith("CMP001,"));
        assertTrue(cpa.get(2).startsWith("CMP002,"));
    }

    @Test
    void missingInputFileExitsWithUsefulError(@TempDir Path tmp) {
        Path missing = tmp.resolve("does-not-exist.csv");
        Path outDir = tmp.resolve("out");

        java.io.IOException ex = org.junit.jupiter.api.Assertions.assertThrows(
                java.io.IOException.class,
                () -> App.run(new String[]{"--input", missing.toString(), "--output", outDir.toString()}));
        assertTrue(ex.getMessage().contains("input file not found"));
    }

    @Test
    void missingArgsThrowUsageException(@TempDir Path tmp) {
        org.junit.jupiter.api.Assertions.assertThrows(
                App.UsageException.class,
                () -> App.run(new String[]{"--input", tmp.resolve("x.csv").toString()}));
    }
}
