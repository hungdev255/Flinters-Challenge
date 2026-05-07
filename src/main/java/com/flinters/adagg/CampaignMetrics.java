package com.flinters.adagg;

import java.math.BigDecimal;

/**
 * Immutable derived view of a campaign's totals. CTR is 0.0 when there were
 * no impressions (no division by zero). CPA is {@code null} when there were
 * no conversions; such campaigns are excluded from the lowest-CPA report.
 */
record CampaignMetrics(
        String campaignId,
        long totalImpressions,
        long totalClicks,
        long totalSpendCents,
        long totalConversions,
        double ctr,
        Double cpa) {

    static CampaignMetrics from(String campaignId, CampaignTotals totals) {
        double ctr = totals.impressions > 0
                ? (double) totals.clicks / (double) totals.impressions
                : 0.0;
        Double cpa = totals.conversions > 0
                ? ((double) totals.spendCents / 100.0) / (double) totals.conversions
                : null;
        return new CampaignMetrics(
                campaignId,
                totals.impressions,
                totals.clicks,
                totals.spendCents,
                totals.conversions,
                ctr,
                cpa);
    }

    /** Exact 2dp representation of {@code total_spend} for CSV output. */
    BigDecimal totalSpend() {
        return BigDecimal.valueOf(totalSpendCents, 2);
    }
}
