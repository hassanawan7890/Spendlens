package com.spendlens.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.spendlens.app.entities.Expense;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.models.MoodSummary;
import com.spendlens.app.repository.ExpenseRepository;

import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {

    private final ExpenseRepository repository;

    // Filter state (used by ExpenseHistoryActivity)
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> filterCategoryId = new MutableLiveData<>(-1); // -1 = all
    private final MutableLiveData<String> filterMood = new MutableLiveData<>(""); // "" = all
    private final MutableLiveData<String> sortOrder = new MutableLiveData<>("newest");

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        repository = new ExpenseRepository(application);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public void insert(Expense expense) { repository.insert(expense); }
    public void update(Expense expense) { repository.update(expense); }
    public void delete(Expense expense) { repository.delete(expense); }
    public void deleteAll()             { repository.deleteAll(); }

    public androidx.lifecycle.LiveData<com.spendlens.app.entities.Expense> getById(int id) {
        return repository.getDao().getById(id);
    }

    // ── All expenses (history) ─────────────────────────────────────────────────

    public LiveData<List<Expense>> getAllExpenses() {
        return repository.getAllExpenses();
    }

    public LiveData<List<Expense>> searchExpenses(String query) {
        return repository.searchExpenses(query);
    }

    // ── Dashboard totals (LiveData — auto-updates UI) ─────────────────────────

    public LiveData<Double> getTotalToday()     { return repository.getTotalToday(); }
    public LiveData<Double> getTotalThisWeek()  { return repository.getTotalThisWeek(); }
    public LiveData<Double> getTotalThisMonth() { return repository.getTotalThisMonth(); }
    public LiveData<Integer> getCountThisMonth(){ return repository.getCountThisMonth(); }

    public LiveData<List<Expense>> getThisMonth() { return repository.getThisMonth(); }
    public LiveData<List<Expense>> getThisWeek()  { return repository.getThisWeek(); }

    // ── Reports ───────────────────────────────────────────────────────────────

    public LiveData<List<CategorySummary>> getCategorySummaryThisMonth() {
        return repository.getCategorySummaryThisMonth();
    }

    public LiveData<List<MoodSummary>> getMoodSummaryThisMonth() {
        return repository.getMoodSummaryThisMonth();
    }

    // ── Filter state setters ──────────────────────────────────────────────────

    public void setSearchQuery(String q)      { searchQuery.setValue(q); }
    public void setFilterCategory(int id)     { filterCategoryId.setValue(id); }
    public void setFilterMood(String mood)    { filterMood.setValue(mood); }
    public void setSortOrder(String order)    { sortOrder.setValue(order); }

    public MutableLiveData<String>  getSearchQuery()      { return searchQuery; }
    public MutableLiveData<Integer> getFilterCategoryId() { return filterCategoryId; }
    public MutableLiveData<String>  getFilterMood()       { return filterMood; }
    public MutableLiveData<String>  getSortOrder()        { return sortOrder; }
}
