package com.flinters.adagg;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CLI entry point. Streams the input CSV, aggregates per campaign, and writes
 * top-10 reports for highest CTR and lowest CPA. Reports elapsed time and
 * peak heap usage on stderr at the end of the run.
 */
public final class App {

    private static final String CTR_FILE = "top10_ctr.csv";
    private static final String CPA_FILE = "top10_cpa.csv";
    private static final int TOP_N = 10;

    public static void main(String[] args) {
        try {
            run(args);
        } catch (UsageException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.err.println("Usage: java -jar aggregator.jar --input <csv> --output <dir>");
            System.exit(2);
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws IOException {
        Args parsed = Args.parse(args);

        if (!Files.isRegularFile(parsed.input())) {
            throw new IOException("input file not found: " + parsed.input());
        }
        Files.createDirectories(parsed.output());

        long startNanos = System.nanoTime();
        System.out.println("Processing: " + parsed.input());

        Map<String, CampaignTotals> totals = new Aggregator(System.err).aggregate(parsed.input());

        List<CampaignMetrics> metrics = totals.entrySet().stream()
                .map(e -> CampaignMetrics.from(e.getKey(), e.getValue()))
                .toList();

        List<CampaignMetrics> topCtr = metrics.stream()
                .sorted(Comparator.comparingDouble(CampaignMetrics::ctr).reversed())
                .limit(TOP_N)
                .toList();
        Path ctrPath = parsed.output().resolve(CTR_FILE);
        ReportWriter.write(ctrPath, topCtr);

        List<CampaignMetrics> topCpa = metrics.stream()
                .filter(m -> m.cpa() != null)
                .sorted(Comparator.comparingDouble(CampaignMetrics::cpa))
                .limit(TOP_N)
                .toList();
        Path cpaPath = parsed.output().resolve(CPA_FILE);
        ReportWriter.write(cpaPath, topCpa);

        double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;

        System.out.println("Written:    " + ctrPath);
        System.out.println("            " + cpaPath);
        System.out.println("Campaigns:  " + metrics.size());
        System.out.printf(Locale.ROOT,
                "Time:       %.2fs | Peak heap: %.1f MB%n",
                elapsedSeconds, peakHeapMb());
    }

    private static double peakHeapMb() {
        long bytes = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                bytes += pool.getPeakUsage().getUsed();
            }
        }
        return bytes / 1024.0 / 1024.0;
    }

    record Args(Path input, Path output) {
        static Args parse(String[] argv) {
            Path input = null;
            Path output = null;
            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--input" -> {
                        if (++i >= argv.length) {
                            throw new UsageException("--input requires a value");
                        }
                        input = Path.of(argv[i]);
                    }
                    case "--output" -> {
                        if (++i >= argv.length) {
                            throw new UsageException("--output requires a value");
                        }
                        output = Path.of(argv[i]);
                    }
                    default -> throw new UsageException("unknown argument: " + argv[i]);
                }
            }
            if (input == null) {
                throw new UsageException("--input is required");
            }
            if (output == null) {
                throw new UsageException("--output is required");
            }
            return new Args(input, output);
        }
    }

    static final class UsageException extends RuntimeException {
        UsageException(String message) {
            super(message);
        }
    }
}
