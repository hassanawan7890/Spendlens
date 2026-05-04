package com.spendlens.app.insights;

import com.spendlens.app.entities.CategorySnapshot;
import com.spendlens.app.entities.MonthlySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects categories where the latest month's spending is significantly
 * above the rolling average across all available snapshots.
 *
 * Threshold: category spending > 20% above its own average = flagged.
 * Minimum: category must appear in at least 1 previous month to be flagged.
 */
public class OverspendDetector {

    private static final double OVERSPEND_THRESHOLD = 0.20; // 20% above average

    public static class OverspendFlag {
        public String categoryName;
        public String iconName;
        public double latestAmount;
        public double averageAmount;
        public double percentAboveAverage;   // e.g. 35.0 means 35% above average
        public String severity;              // "High" ≥50%, "Medium" 20–49%
    }

    /**
     * Compares latest month's category spending against rolling average.
     *
     * @param latestCats      category snapshots for the most recent month
     * @param allPreviousCats category snapshots for ALL prior months (flat list)
     * @return list of flagged categories, sorted by severity descending
     */
    public static List<OverspendFlag> detect(
            List<CategorySnapshot> latestCats,
            List<CategorySnapshot> allPreviousCats) {

        List<OverspendFlag> flags = new ArrayList<>();
        if (latestCats == null || latestCats.isEmpty()) return flags;

        // Build rolling average map from previous months
        // Map: categoryName → {totalSpent, monthCount}
        Map<String, double[]> avgMap = new HashMap<>();
        if (allPreviousCats != null) {
            // Group by category name, track sum and count of months seen
            Map<String, List<Double>> monthlyAmounts = new HashMap<>();
            for (CategorySnapshot c : allPreviousCats) {
                monthlyAmounts.computeIfAbsent(c.categoryName, k -> new ArrayList<>())
                        .add(c.totalSpent);
            }
            for (Map.Entry<String, List<Double>> entry : monthlyAmounts.entrySet()) {
                double sum = 0;
                for (double d : entry.getValue()) sum += d;
                avgMap.put(entry.getKey(), new double[]{
                        sum / entry.getValue().size(),  // [0] = average
                        entry.getValue().size()          // [1] = months seen
                });
            }
        }

        for (CategorySnapshot cat : latestCats) {
            if (!avgMap.containsKey(cat.categoryName)) continue; // no history = skip

            double avg     = avgMap.get(cat.categoryName)[0];
            double latest  = cat.totalSpent;
            if (avg <= 0) continue;

            double pctAbove = ((latest - avg) / avg);
            if (pctAbove < OVERSPEND_THRESHOLD) continue; // within normal range

            OverspendFlag flag = new OverspendFlag();
            flag.categoryName        = cat.categoryName;
            flag.iconName            = cat.iconName;
            flag.latestAmount        = latest;
            flag.averageAmount       = avg;
            flag.percentAboveAverage = pctAbove * 100;
            flag.severity            = pctAbove >= 0.50 ? "High" : "Medium";
            flags.add(flag);
        }

        // Sort: High severity first, then by percent above average
        flags.sort((a, b) -> {
            if (!a.severity.equals(b.severity))
                return a.severity.equals("High") ? -1 : 1;
            return Double.compare(b.percentAboveAverage, a.percentAboveAverage);
        });

        return flags;
    }

    /**
     * Collects all category snapshots across a list of snapshots EXCEPT the latest.
     * Helper to prepare the allPreviousCats parameter for detect().
     */
    public static List<CategorySnapshot> collectPreviousCats(
            List<MonthlySnapshot> snapshots,
            com.spendlens.app.repository.SnapshotRepository repo) {

        List<CategorySnapshot> all = new ArrayList<>();
        // Skip index 0 (latest), collect all others
        for (int i = 1; i < snapshots.size(); i++) {
            List<CategorySnapshot> cats = repo.getCategoriesForSnapshot(snapshots.get(i).id);
            if (cats != null) all.addAll(cats);
        }
        return all;
    }
}
