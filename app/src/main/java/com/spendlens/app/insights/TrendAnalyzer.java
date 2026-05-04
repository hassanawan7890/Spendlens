package com.spendlens.app.insights;

import com.spendlens.app.entities.MonthlySnapshot;

import java.util.List;

/**
 * Pure logic class — no Android context, no DB calls.
 * Takes a list of MonthlySnapshots (newest first) and returns
 * structured analysis results. Call on a background thread.
 */
public class TrendAnalyzer {

    // ── Result models ─────────────────────────────────────────────────────────

    public static class TrendResult {
        public double averageMonthlySpending;
        public double averageSavings;
        public double averageSavingsRate;      // 0.0–1.0
        public MonthlySnapshot bestMonth;      // lowest spending vs budget
        public MonthlySnapshot worstMonth;     // highest overspend or lowest savings
        public double spendingChangePercent;   // latest vs previous month, can be negative
        public boolean isImprovingTrend;       // spending going down over time
        public int monthsAnalyzed;
    }

    public static class CategoryComparison {
        public String categoryName;
        public String iconName;
        public double latestMonthAmount;
        public double previousMonthAmount;
        public double changePercent;           // positive = increased, negative = decreased
        public boolean isIncrease;
    }

    // ── Main analysis methods ─────────────────────────────────────────────────

    /**
     * Generates the overall trend result from a list of snapshots.
     * Requires at least 1 snapshot. Returns null if list is empty.
     * Snapshots must be ordered newest first.
     */
    public static TrendResult analyze(List<MonthlySnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return null;

        TrendResult result = new TrendResult();
        result.monthsAnalyzed = snapshots.size();

        // Average monthly spending
        double totalSpent = 0;
        double totalSavings = 0;
        double totalBudget = 0;
        for (MonthlySnapshot s : snapshots) {
            totalSpent   += s.totalSpent;
            totalSavings += s.savings;
            totalBudget  += s.plannedBudget;
        }
        result.averageMonthlySpending = totalSpent / snapshots.size();
        result.averageSavings         = totalSavings / snapshots.size();
        result.averageSavingsRate     = totalBudget > 0 ? totalSavings / totalBudget : 0;

        // Best month = highest savings (or least overspend)
        result.bestMonth  = snapshots.get(0);
        result.worstMonth = snapshots.get(0);
        for (MonthlySnapshot s : snapshots) {
            if (s.savings > result.bestMonth.savings)  result.bestMonth  = s;
            if (s.savings < result.worstMonth.savings) result.worstMonth = s;
        }

        // Spending change: latest vs previous month
        if (snapshots.size() >= 2) {
            double latest   = snapshots.get(0).totalSpent;
            double previous = snapshots.get(1).totalSpent;
            result.spendingChangePercent = previous > 0
                    ? ((latest - previous) / previous) * 100 : 0;
        }

        // Improving trend: check if spending is generally going down
        // (compare first half avg vs second half avg of the list)
        if (snapshots.size() >= 3) {
            int mid = snapshots.size() / 2;
            double recentAvg = 0, olderAvg = 0;
            for (int i = 0; i < mid; i++)               recentAvg += snapshots.get(i).totalSpent;
            for (int i = mid; i < snapshots.size(); i++) olderAvg  += snapshots.get(i).totalSpent;
            recentAvg /= mid;
            olderAvg  /= (snapshots.size() - mid);
            result.isImprovingTrend = recentAvg < olderAvg;
        }

        return result;
    }

    /**
     * Compares category spending between the latest and previous month.
     * Returns empty list if fewer than 2 snapshots.
     * Snapshots must be ordered newest first.
     */
    public static java.util.List<CategoryComparison> compareCategoriesMonthOverMonth(
            List<MonthlySnapshot> snapshots,
            java.util.List<com.spendlens.app.entities.CategorySnapshot> latestCats,
            java.util.List<com.spendlens.app.entities.CategorySnapshot> previousCats) {

        java.util.List<CategoryComparison> result = new java.util.ArrayList<>();
        if (latestCats == null || latestCats.isEmpty()) return result;

        // Build a map of previous month category amounts for O(1) lookup
        java.util.Map<String, Double> prevMap = new java.util.HashMap<>();
        if (previousCats != null) {
            for (com.spendlens.app.entities.CategorySnapshot c : previousCats) {
                prevMap.put(c.categoryName, c.totalSpent);
            }
        }

        for (com.spendlens.app.entities.CategorySnapshot cat : latestCats) {
            CategoryComparison comp = new CategoryComparison();
            comp.categoryName        = cat.categoryName;
            comp.iconName            = cat.iconName;
            comp.latestMonthAmount   = cat.totalSpent;
            comp.previousMonthAmount = prevMap.getOrDefault(cat.categoryName, 0.0);

            if (comp.previousMonthAmount > 0) {
                comp.changePercent = ((comp.latestMonthAmount - comp.previousMonthAmount)
                        / comp.previousMonthAmount) * 100;
            } else {
                comp.changePercent = comp.latestMonthAmount > 0 ? 100 : 0;
            }
            comp.isIncrease = comp.changePercent > 0;
            result.add(comp);
        }

        // Sort by absolute change descending — biggest movers first
        result.sort((a, b) ->
                Double.compare(Math.abs(b.changePercent), Math.abs(a.changePercent)));

        return result;
    }

    /** Formats a percent change for display e.g. "+18%" or "-12%" */
    public static String formatChangePercent(double pct) {
        String sign = pct >= 0 ? "+" : "";
        return sign + (int) pct + "%";
    }
}
