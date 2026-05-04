package com.spendlens.app.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendlens.app.entities.CategorySnapshot;

import java.util.List;

@Dao
public interface CategorySnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategorySnapshot snapshot);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategorySnapshot> snapshots);

    /** All category rows for a specific snapshot — ordered by spend descending */
    @Query("SELECT * FROM category_snapshots WHERE snapshotId = :snapshotId ORDER BY totalSpent DESC")
    List<CategorySnapshot> getCategoriesForSnapshot(int snapshotId);

    /** Delete all category rows for a snapshot — used when regenerating */
    @Query("DELETE FROM category_snapshots WHERE snapshotId = :snapshotId")
    void deleteForSnapshot(int snapshotId);

    @Query("DELETE FROM category_snapshots")
    void deleteAll();
}
