package com.spendlens.app.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    // Single-user app: userId is always 1
    @PrimaryKey
    public int userId = 1;

    public String name;
    public double monthlyBudget;
    public double weeklyBudget;   // 0.0 = not set (optional sub-limit)
    public String currency;       // display symbol e.g. "RM", "USD", "$"

    // ── Constructors ──────────────────────────────────────────────────────────

    public UserProfile() {}

    @Ignore
    public UserProfile(String name, double monthlyBudget,
                       double weeklyBudget, String currency) {
        this.userId = 1;
        this.name = name;
        this.monthlyBudget = monthlyBudget;
        this.weeklyBudget = weeklyBudget;
        this.currency = currency;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hasWeeklyBudget() {
        return weeklyBudget > 0.0;
    }

    public String getDisplayCurrency() {
        return currency != null ? currency : "RM";
    }
}
