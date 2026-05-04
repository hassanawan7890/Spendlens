package com.spendlens.app;

import com.spendlens.app.utils.BudgetCalculator;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for BudgetCalculator
 *
 * These are pure Java tests — no Android device or emulator needed.
 * Run with: Right-click the file → Run 'BudgetCalculatorTest'
 */
public class BudgetCalculatorTest {

    // ── getRemaining ──────────────────────────────────────────────────────────

    @Test
    public void getRemaining_underBudget_returnsPositive() {
        double result = BudgetCalculator.getRemaining(1000, 400);
        assertEquals(600.0, result, 0.01);
    }

    @Test
    public void getRemaining_overBudget_returnsNegative() {
        double result = BudgetCalculator.getRemaining(1000, 1200);
        assertEquals(-200.0, result, 0.01);
    }

    @Test
    public void getRemaining_exactBudget_returnsZero() {
        double result = BudgetCalculator.getRemaining(1000, 1000);
        assertEquals(0.0, result, 0.01);
    }

    // ── getSpentPercent ───────────────────────────────────────────────────────

    @Test
    public void getSpentPercent_halfSpent_returns50() {
        double result = BudgetCalculator.getSpentPercent(1000, 500);
        assertEquals(50.0, result, 0.01);
    }

    @Test
    public void getSpentPercent_zeroBudget_returnsZero() {
        double result = BudgetCalculator.getSpentPercent(0, 500);
        assertEquals(0.0, result, 0.01);
    }

    @Test
    public void getSpentPercent_nothingSpent_returnsZero() {
        double result = BudgetCalculator.getSpentPercent(1000, 0);
        assertEquals(0.0, result, 0.01);
    }

    // ── getRiskLevel ──────────────────────────────────────────────────────────

    @Test
    public void getRiskLevel_lessThan40Percent_returnsSafe() {
        // 300 / 1000 = 30% → SAFE
        int result = BudgetCalculator.getRiskLevel(1000, 300);
        assertEquals(BudgetCalculator.RISK_SAFE, result);
    }

    @Test
    public void getRiskLevel_between40And69Percent_returnsModerate() {
        // 500 / 1000 = 50% → MODERATE
        int result = BudgetCalculator.getRiskLevel(1000, 500);
        assertEquals(BudgetCalculator.RISK_MODERATE, result);
    }

    @Test
    public void getRiskLevel_between70And89Percent_returnsWarning() {
        // 800 / 1000 = 80% → WARNING
        int result = BudgetCalculator.getRiskLevel(1000, 800);
        assertEquals(BudgetCalculator.RISK_WARNING, result);
    }

    @Test
    public void getRiskLevel_above90Percent_returnsCritical() {
        // 950 / 1000 = 95% → CRITICAL
        int result = BudgetCalculator.getRiskLevel(1000, 950);
        assertEquals(BudgetCalculator.RISK_CRITICAL, result);
    }

    @Test
    public void getRiskLevel_exactly90Percent_returnsCritical() {
        // 900 / 1000 = 90% → CRITICAL (boundary)
        int result = BudgetCalculator.getRiskLevel(1000, 900);
        assertEquals(BudgetCalculator.RISK_CRITICAL, result);
    }

    // ── getRiskLabel ──────────────────────────────────────────────────────────

    @Test
    public void getRiskLabel_safe_returnsCorrectString() {
        assertEquals("Safe", BudgetCalculator.getRiskLabel(BudgetCalculator.RISK_SAFE));
    }

    @Test
    public void getRiskLabel_critical_returnsCorrectString() {
        assertEquals("Critical", BudgetCalculator.getRiskLabel(BudgetCalculator.RISK_CRITICAL));
    }

    // ── getProgressFraction ───────────────────────────────────────────────────

    @Test
    public void getProgressFraction_halfSpent_returns0Point5() {
        float result = BudgetCalculator.getProgressFraction(1000, 500);
        assertEquals(0.5f, result, 0.01f);
    }

    @Test
    public void getProgressFraction_overBudget_clampedTo1() {
        // Over budget should clamp to 1.0, not exceed it
        float result = BudgetCalculator.getProgressFraction(1000, 2000);
        assertEquals(1.0f, result, 0.01f);
    }

    @Test
    public void getProgressFraction_zeroBudget_returnsZero() {
        float result = BudgetCalculator.getProgressFraction(0, 500);
        assertEquals(0f, result, 0.01f);
    }

    // ── deriveWeeklyBudget ────────────────────────────────────────────────────

    @Test
    public void deriveWeeklyBudget_monthly1000_returnsApprox231() {
        // 1000 / 4.33 ≈ 231
        double result = BudgetCalculator.deriveWeeklyBudget(1000);
        assertEquals(231.0, result, 1.0);
    }

    // ── getAdjustedRiskLevel ──────────────────────────────────────────────────

    @Test
    public void getAdjustedRiskLevel_earlyMonthHighSpend_returnsCritical() {
        // Spent 80% on day 5 of 31 — very risky
        int result = BudgetCalculator.getAdjustedRiskLevel(1000, 800, 5, 31);
        assertEquals(BudgetCalculator.RISK_CRITICAL, result);
    }

    @Test
    public void getAdjustedRiskLevel_lateMonthHighSpend_returnsModerate() {
        // Spent 80% on day 29 of 31
        // adjustedRatio = (800/1000) / (29/31) = 0.8 / 0.935 = 0.855 → MODERATE
        int result = BudgetCalculator.getAdjustedRiskLevel(1000, 800, 29, 31);
        assertEquals(BudgetCalculator.RISK_MODERATE, result);
    }
}