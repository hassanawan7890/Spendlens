package com.spendlens.app.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Stores a complete financial summary for one calendar month.
 * Generated either on month rollover or manually from BudgetHistoryActivity.
 * One row per month — identified by month + year combination.
 */
@Entity(tableName = "monthly_snapshots")
public class MonthlySnapshot {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int month;            // 1–12
    public int year;             // e.g. 2026
    public double plannedBudget; // monthlyBudget from UserProfile at time of snapshot
    public double totalSpent;    // sum of all expenses in that month
    public double totalIncome;   // 0 for now — populated if income tracking added later
    public double savings;       // plannedBudget - totalSpent (can be negative)
    public long createdAt;       // Unix timestamp ms when snapshot was generated

    public MonthlySnapshot() {}

    @Ignore
    public MonthlySnapshot(int month, int year, double plannedBudget,
                           double totalSpent, double totalIncome) {
        this.month         = month;
        this.year          = year;
        this.plannedBudget = plannedBudget;
        this.totalSpent    = totalSpent;
        this.totalIncome   = totalIncome;
        this.savings       = plannedBudget - totalSpent;
        this.createdAt     = System.currentTimeMillis();
    }

    /** True if the user stayed within budget this month */
    public boolean isUnderBudget() {
        return savings >= 0;
    }

    /** Display label e.g. "March 2026" */
    public String getDisplayLabel() {
        String[] months = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        return months[Math.max(0, month - 1)] + " " + year;
    }
}
