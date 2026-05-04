package com.spendlens.app.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "imported_transactions",
        foreignKeys = @ForeignKey(
                entity = StatementImport.class,
                parentColumns = "id",
                childColumns = "statementId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("statementId")}
)
public class ImportedTransaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int statementId;
    public long date;
    public String description;
    public double amount;
    public String type;
    public int categoryId;
    public String categoryName;
    public boolean isAutoCategorized;
    public boolean addToExpenses;
    public String suggestionSource;
    public String confidenceLabel;
    public String matchReason;

    public ImportedTransaction() {}

    @Ignore
    public ImportedTransaction(int statementId, long date, String description, double amount,
                               String type, int categoryId, String categoryName,
                               boolean isAutoCategorized, String suggestionSource,
                               String confidenceLabel, String matchReason) {
        this.statementId = statementId;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.isAutoCategorized = isAutoCategorized;
        this.addToExpenses = "debit".equals(type);
        this.suggestionSource = suggestionSource;
        this.confidenceLabel = confidenceLabel;
        this.matchReason = matchReason;
    }
}
