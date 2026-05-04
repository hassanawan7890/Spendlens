package com.spendlens.app.dao;
import androidx.room.*; import com.spendlens.app.entities.ImportedTransaction; import java.util.List;
@Dao public interface ImportedTransactionDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) void insertAll(List<ImportedTransaction> transactions);
    @Update void update(ImportedTransaction transaction);
    @Query("SELECT * FROM imported_transactions WHERE statementId=:sid ORDER BY date DESC") List<ImportedTransaction> getForStatementSync(int sid);
    @Query("SELECT * FROM imported_transactions WHERE statementId=:sid AND addToExpenses=1 ORDER BY date DESC") List<ImportedTransaction> getConfirmedForStatementSync(int sid);
    @Query("DELETE FROM imported_transactions WHERE statementId=:sid") void deleteForStatement(int sid);
    @Query("DELETE FROM imported_transactions") void deleteAll();
}