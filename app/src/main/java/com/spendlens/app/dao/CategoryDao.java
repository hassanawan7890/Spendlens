package com.spendlens.app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendlens.app.entities.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Category category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Category... categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, categoryName ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, categoryName ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE categoryId = :id")
    Category getCategoryByIdSync(int id);

    @Query("SELECT * FROM categories WHERE LOWER(categoryName) = LOWER(:name) ORDER BY isDefault DESC, categoryId ASC LIMIT 1")
    Category getCategoryByNameSync(String name);

    @Query("SELECT COUNT(*) FROM categories")
    int getCount();

    @Query("DELETE FROM categories WHERE isDefault = 0")
    void deleteAll();

    @Query("DELETE FROM categories")
    void deleteAllIncludingDefaults();
}
