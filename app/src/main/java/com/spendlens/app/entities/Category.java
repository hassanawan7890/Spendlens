package com.spendlens.app.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    public int categoryId;

    public String categoryName;   // e.g. "Food & Drinks"
    public String iconName;       // drawable resource name e.g. "ic_food"
    public double spendingLimit;  // 0.0 means no limit set
    public boolean isDefault;     // true = pre-seeded, user cannot delete

    // ── Constructors ──────────────────────────────────────────────────────────

    public Category() {}

    @Ignore
    public Category(String categoryName, String iconName,
                    double spendingLimit, boolean isDefault) {
        this.categoryName = categoryName;
        this.iconName = iconName;
        this.spendingLimit = spendingLimit;
        this.isDefault = isDefault;
    }

    // ── Default category seeds ────────────────────────────────────────────────
    // Call DatabaseSeeder.seedCategories() on first launch

    public static Category[] getDefaultCategories() {
        return new Category[]{
                new Category("Food & Drinks",          "ic_food",         0.0, true),
                new Category("Transport",              "ic_transport",    0.0, true),
                new Category("Leisure",                "ic_leisure",      0.0, true),
                new Category("Subscriptions",          "ic_subscription", 0.0, true),
                new Category("Health & Medical",       "ic_health",       0.0, true),
                new Category("Education",              "ic_education",    0.0, true),
                new Category("Shopping",               "ic_shopping",     0.0, true),
                new Category("Others",                 "ic_others",       0.0, true),
        };
    }
}
