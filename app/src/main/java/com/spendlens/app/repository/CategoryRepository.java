package com.spendlens.app.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendlens.app.dao.CategoryDao;
import com.spendlens.app.dao.ExpenseDao;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.Category;
import com.spendlens.app.utils.CategoryDisplayUtils;

import java.util.List;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final ExpenseDao expenseDao;
    private final LiveData<List<Category>> allCategories;

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        expenseDao = db.expenseDao();
        allCategories = categoryDao.getAllCategories();
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void insert(Category category) {
        AppDatabase.dbExecutor.execute(() -> categoryDao.insert(category));
    }

    public void update(Category category) {
        AppDatabase.dbExecutor.execute(() -> categoryDao.update(category));
    }

    public void delete(Category category) {
        AppDatabase.dbExecutor.execute(() -> {
            Category fallback = categoryDao.getCategoryByNameSync(
                    CategoryDisplayUtils.DEFAULT_CATEGORY_NAME);

            if (fallback == null) {
                categoryDao.insert(new Category(
                        CategoryDisplayUtils.DEFAULT_CATEGORY_NAME,
                        CategoryDisplayUtils.DEFAULT_ICON_NAME,
                        0.0,
                        true
                ));
                fallback = categoryDao.getCategoryByNameSync(
                        CategoryDisplayUtils.DEFAULT_CATEGORY_NAME);
            }

            if (fallback != null && fallback.categoryId != category.categoryId) {
                expenseDao.reassignCategory(category.categoryId, fallback.categoryId);
            }

            categoryDao.delete(category);
        });
    }

    public void deleteAll() {
        AppDatabase.dbExecutor.execute(categoryDao::deleteAll);
    }
}
