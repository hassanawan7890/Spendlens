package com.spendlens.app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendlens.app.entities.MonthlySnapshot;

import java.util.List;

@Dao
public interface MonthlySnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MonthlySnapshot snapshot);  // returns rowId for linking category snapshots

    @Delete
    void delete(MonthlySnapshot snapshot);

    /** All snapshots ordered newest first — for the history list */
    @Query("SELECT * FROM monthly_snapshots ORDER BY year DESC, month DESC")
    LiveData<List<MonthlySnapshot>> getAllSnapshots();

    /** Sync version — used inside background thread operations */
    @Query("SELECT * FROM monthly_snapshots ORDER BY year DESC, month DESC")
    List<MonthlySnapshot> getAllSnapshotsSync();

    /** Check if a snapshot already exists for a given month/year */
    @Query("SELECT * FROM monthly_snapshots WHERE month = :month AND year = :year LIMIT 1")
    MonthlySnapshot getSnapshotForMonth(int month, int year);

    /** Last N months — used by the analysis engine */
    @Query("SELECT * FROM monthly_snapshots ORDER BY year DESC, month DESC LIMIT :limit")
    List<MonthlySnapshot> getLastNSnapshots(int limit);

    @Query("SELECT COUNT(*) FROM monthly_snapshots")
    int getCount();

    @Query("DELETE FROM monthly_snapshots")
    void deleteAll();
}
