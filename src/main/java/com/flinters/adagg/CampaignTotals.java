package com.flinters.adagg;

/**
 * Mutable per-campaign accumulator. All fields are primitives so the hot path
 * never boxes across the hundreds of millions of row updates required for a
 * 1GB CSV. Spend is stored as {@code long} cents to eliminate floating-point
 * drift on monetary values; conversion back to a 2dp decimal happens only at
 * report-writing time.
 */
final class CampaignTotals {

    long impressions;
    long clicks;
    long spendCents;
    long conversions;

    void add(long impressions, long clicks, long spendCents, long conversions) {
        this.impressions += impressions;
        this.clicks += clicks;
        this.spendCents += spendCents;
        this.conversions += conversions;
    }
}
