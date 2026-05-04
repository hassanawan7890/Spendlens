package com.spendlens.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendlens.app.entities.Category;
import com.spendlens.app.repository.CategoryRepository;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final LiveData<List<Category>> allCategories;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        allCategories = repository.getAllCategories();
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void insert(Category c) {
        repository.insert(c);
    }

    public void update(Category c) {
        repository.update(c);
    }

    public void delete(Category c) {
        repository.delete(c);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}