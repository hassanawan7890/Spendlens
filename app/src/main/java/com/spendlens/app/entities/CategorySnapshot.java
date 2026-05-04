package com.spendlens.app.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Stores per-category spending breakdown for one MonthlySnapshot.
 * Multiple rows per snapshot — one per category that had spending that month.
 */
@Entity(
        tableName = "category_snapshots",
        foreignKeys = @ForeignKey(
                entity = MonthlySnapshot.class,
                parentColumns = "id",
                childColumns = "snapshotId",
                onDelete = ForeignKey.CASCADE  // deleting a snapshot removes its category rows
        ),
        indices = {@Index("snapshotId")}
)
public class CategorySnapshot {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int snapshotId;       // FK → MonthlySnapshot.id
    public String categoryName;  // stored as string — category may be renamed/deleted later
    public String iconName;      // e.g. "ic_food" — for display emoji
    public double totalSpent;    // total amount spent in this category that month
    public double percentage;    // totalSpent / monthlyTotal * 100
    public int transactionCount; // number of expenses in this category that month

    public CategorySnapshot() {}

    @Ignore
    public CategorySnapshot(int snapshotId, String categoryName, String iconName,
                            double totalSpent, double percentage, int transactionCount) {
        this.snapshotId        = snapshotId;
        this.categoryName      = categoryName;
        this.iconName          = iconName;
        this.totalSpent        = totalSpent;
        this.percentage        = percentage;
        this.transactionCount  = transactionCount;
    }
}
