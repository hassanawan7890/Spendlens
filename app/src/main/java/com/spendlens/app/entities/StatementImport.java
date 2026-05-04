package com.spendlens.app.entities;
import androidx.room.Entity; import androidx.room.Ignore; import androidx.room.PrimaryKey;
@Entity(tableName = "statement_imports")
public class StatementImport {
    @PrimaryKey(autoGenerate = true) public int id;
    public String fileName; public int month; public int year;
    public long importedAt; public String sourceType;
    public int totalTransactions; public int addedToExpenses;
    public StatementImport() {}
    @Ignore
    public StatementImport(String fileName, int month, int year, String sourceType, int totalTransactions) {
        this.fileName=fileName; this.month=month; this.year=year;
        this.importedAt=System.currentTimeMillis(); this.sourceType=sourceType;
        this.totalTransactions=totalTransactions; this.addedToExpenses=0;
    }
}