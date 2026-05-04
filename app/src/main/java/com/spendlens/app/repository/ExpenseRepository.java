package com.spendlens.app.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendlens.app.dao.ExpenseDao;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.models.DaySummary;
import com.spendlens.app.models.MoodSummary;
import com.spendlens.app.utils.DateUtils;

import java.util.List;

public class ExpenseRepository {

    private final ExpenseDao dao;

    public ExpenseRepository(Application application) {
        dao = AppDatabase.getInstance(application).expenseDao();
    }

    // ── Write operations (always on background thread) ────────────────────────

    public void insert(Expense expense) {
        AppDatabase.dbExecutor.execute(() -> dao.insert(expense));
    }

    public void update(Expense expense) {
        AppDatabase.dbExecutor.execute(() -> dao.update(expense));
    }

    public void delete(Expense expense) {
        AppDatabase.dbExecutor.execute(() -> dao.delete(expense));
    }

    public void deleteAll() {
        AppDatabase.dbExecutor.execute(dao::deleteAll);
    }

    // ── Read: LiveData (observed by Activities/ViewModels) ────────────────────

    public LiveData<List<Expense>> getAllExpenses() {
        return dao.getAllExpenses();
    }

    public LiveData<List<Expense>> searchExpenses(String query) {
        return dao.searchExpenses(query);
    }

    public LiveData<List<Expense>> getByCategory(int categoryId) {
        return dao.getByCategory(categoryId);
    }

    public LiveData<List<Expense>> getByMood(String mood) {
        return dao.getByMood(mood);
    }

    public LiveData<List<Expense>> getThisMonth() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getExpensesBetween(range[0], range[1]);
    }

    public LiveData<List<Expense>> getThisWeek() {
        long[] range = DateUtils.getCurrentWeekRange();
        return dao.getExpensesBetween(range[0], range[1]);
    }

    public LiveData<Double> getTotalThisMonth() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getTotalBetween(range[0], range[1]);
    }

    public LiveData<Double> getTotalThisWeek() {
        long[] range = DateUtils.getCurrentWeekRange();
        return dao.getTotalBetween(range[0], range[1]);
    }

    public LiveData<Double> getTotalToday() {
        long[] range = DateUtils.getTodayRange();
        return dao.getTotalBetween(range[0], range[1]);
    }

    public LiveData<Integer> getCountThisMonth() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getCountBetween(range[0], range[1]);
    }

    // ── Read: Reports ─────────────────────────────────────────────────────────

    public LiveData<List<CategorySummary>> getCategorySummaryThisMonth() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getCategorySummary(range[0], range[1]);
    }

    public LiveData<List<MoodSummary>> getMoodSummaryThisMonth() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getMoodSummary(range[0], range[1]);
    }

    // ── Read: Insights (sync — called from InsightEngine on background thread) ─

    public List<CategorySummary> getLeakCandidatesThisWeek(int minCount) {
        long[] range = DateUtils.getCurrentWeekRange();
        return dao.getLeakCandidatesSync(range[0], range[1], minCount);
    }

    public List<DaySummary> getDailySummaryThisWeek() {
        long[] range = DateUtils.getCurrentWeekRange();
        return dao.getDailySummarySync(range[0], range[1]);
    }

    public List<MoodSummary> getMoodSummaryThisMonthSync() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getMoodSummarySync(range[0], range[1]);
    }

    public ExpenseDao getDao() { return dao; }

    public java.util.List<com.spendlens.app.models.CategorySummary> getCategorySummaryThisMonthSync() {
        long[] range = com.spendlens.app.utils.DateUtils.getCurrentMonthRange();
        return dao.getCategorySummarySync(range[0], range[1]);
    }

    public double getTotalThisMonthSync() {
        long[] range = DateUtils.getCurrentMonthRange();
        return dao.getTotalBetweenSync(range[0], range[1]);
    }
}
