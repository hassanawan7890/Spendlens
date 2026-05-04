package com.spendlens.app.database;

import com.spendlens.app.dao.CategoryDao;
import com.spendlens.app.entities.Category;

/**
 * Seeds the database with default data on first launch.
 * Called from AppDatabase.Callback.onCreate() — runs on background thread.
 */
public class DatabaseSeeder {

    public static void seedCategories(CategoryDao categoryDao) {
        if (categoryDao.getCount() > 0) return; // already seeded

        categoryDao.insertAll(Category.getDefaultCategories());
    }
}
