package com.spendlens.app.dao;
import androidx.room.*; import com.spendlens.app.entities.StatementImport; import java.util.List;
@Dao public interface StatementImportDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) long insert(StatementImport s);
    @Update void update(StatementImport s);
    @Query("SELECT * FROM statement_imports ORDER BY importedAt DESC") List<StatementImport> getAllSync();
    @Query("SELECT * FROM statement_imports WHERE id=:id LIMIT 1") StatementImport getByIdSync(int id);
    @Query("DELETE FROM statement_imports WHERE id=:id") void deleteById(int id);
    @Query("DELETE FROM statement_imports") void deleteAll();
}