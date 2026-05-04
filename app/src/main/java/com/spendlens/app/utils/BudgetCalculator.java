package com.spendlens.app.utils;

/**
 * BudgetCalculator
 *
 * All budget math lives here — Dashboard cards, Risk Meter, and Insights
 * all call these static methods. Change the formula once, it updates everywhere.
 *
 * Risk levels:
 *   SAFE      < 40% spent
 *   MODERATE  40–69% spent
 *   WARNING   70–89% spent
 *   CRITICAL  >= 90% spent
 *
 * Adjusted risk also factors in days elapsed vs remaining so a user who
 * spent 80% on day 29 of 31 isn't penalised the same as one who spent
 * 80% on day 5.
 */
public class BudgetCalculator {

    public static final int RISK_SAFE     = 0;
    public static final int RISK_MODERATE = 1;
    public static final int RISK_WARNING  = 2;
    public static final int RISK_CRITICAL = 3;

    // ── Remaining ─────────────────────────────────────────────────────────────

    /**
     * Returns negative value when over budget.
     * Dashboard displays negative remaining in red with a minus sign.
     */
    public static double getRemaining(double budget, double spent) {
        return budget - spent;
    }

    public static double getSpentPercent(double budget, double spent) {
        if (budget <= 0) return 0;
        return (spent / budget) * 100.0;
    }

    // ── Risk level (simple: based on % spent only) ────────────────────────────

    public static int getRiskLevel(double budget, double spent) {
        double pct = getSpentPercent(budget, spent);
        if (pct >= 90) return RISK_CRITICAL;
        if (pct >= 70) return RISK_WARNING;
        if (pct >= 40) return RISK_MODERATE;
        return RISK_SAFE;
    }

    /**
     * Adjusted risk — factors in days elapsed.
     *
     * Formula: adjustedRatio = (spent / budget) / (daysElapsed / totalDays)
     *
     * Example: 80% spent on day 29 of 31 → ratio = 0.80 / 0.935 = 0.855 → WARNING
     * Example: 80% spent on day 5  of 31 → ratio = 0.80 / 0.161 = 4.96  → CRITICAL
     *
     * Falls back to simple risk if days data is unavailable.
     */
    public static int getAdjustedRiskLevel(double budget, double spent,
                                           int daysElapsed, int totalDays) {
        if (budget <= 0 || totalDays <= 0 || daysElapsed <= 0) {
            return getRiskLevel(budget, spent);
        }

        // Too early in the month to judge pace — fall back to simple % spent
        if (daysElapsed < 5) {
            return getRiskLevel(budget, spent);
        }

        double spentRatio    = spent / budget;
        double timeRatio     = (double) daysElapsed / totalDays;
        double adjustedRatio = spentRatio / timeRatio;

        if (adjustedRatio >= 1.2) return RISK_CRITICAL;
        if (adjustedRatio >= 0.9) return RISK_WARNING;
        if (adjustedRatio >= 0.6) return RISK_MODERATE;
        return RISK_SAFE;
    }

    // ── Label helpers ─────────────────────────────────────────────────────────

    public static String getRiskLabel(int riskLevel) {
        switch (riskLevel) {
            case RISK_CRITICAL: return "Critical";
            case RISK_WARNING:  return "Warning";
            case RISK_MODERATE: return "Moderate";
            default:            return "Safe";
        }
    }

    // ── Weekly budget helpers ─────────────────────────────────────────────────

    public static double deriveWeeklyBudget(double monthlyBudget) {
        return monthlyBudget / 4.33;
    }

    public static double getEffectiveWeeklyBudget(double monthlyBudget, double weeklyBudget) {
        return weeklyBudget > 0 ? weeklyBudget : deriveWeeklyBudget(monthlyBudget);
    }

    // ── Progress bar (0.0–1.0 clamped) ───────────────────────────────────────

    public static float getProgressFraction(double budget, double spent) {
        if (budget <= 0) return 0f;
        return (float) Math.min(1.0, spent / budget);
    }
}