package com.spendlens.app.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendlens.app.dao.CategorySnapshotDao;
import com.spendlens.app.dao.MonthlySnapshotDao;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.CategorySnapshot;
import com.spendlens.app.entities.MonthlySnapshot;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SnapshotRepository {

    private final MonthlySnapshotDao snapshotDao;
    private final CategorySnapshotDao categorySnapshotDao;
    private final AppDatabase db;

    public SnapshotRepository(Application application) {
        db                  = AppDatabase.getInstance(application);
        snapshotDao         = db.monthlySnapshotDao();
        categorySnapshotDao = db.categorySnapshotDao();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public LiveData<List<MonthlySnapshot>> getAllSnapshots() {
        return snapshotDao.getAllSnapshots();
    }

    public List<MonthlySnapshot> getLastNSnapshots(int n) {
        return snapshotDao.getLastNSnapshots(n);
    }

    public List<CategorySnapshot> getCategoriesForSnapshot(int snapshotId) {
        return categorySnapshotDao.getCategoriesForSnapshot(snapshotId);
    }

    // ── Generate snapshot for a given month ───────────────────────────────────

    /**
     * Builds and saves a MonthlySnapshot for the given month/year.
     * Queries the expenses table for that month's range, sums by category,
     * then writes MonthlySnapshot + CategorySnapshot rows to Room.
     *
     * Safe to call multiple times — replaces existing snapshot for that month.
     * Must be called on a background thread.
     *
     * @param month       1–12
     * @param year        e.g. 2026
     * @param budget      planned budget (from UserProfile)
     */
    public void generateSnapshot(int month, int year, double budget) {
        // Calculate date range for the target month
        Calendar start = Calendar.getInstance();
        start.set(year, month - 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(year, month - 1, start.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        end.set(Calendar.MILLISECOND, 999);

        long fromMs = start.getTimeInMillis();
        long toMs   = end.getTimeInMillis();

        // Get total spent for the month
        double totalSpent = db.expenseDao().getTotalBetweenSync(fromMs, toMs);

        // Get category breakdown
        List<CategorySummary> catSummaries =
                db.expenseDao().getCategorySummarySync(fromMs, toMs);

        // Delete existing snapshot for this month if any (to regenerate cleanly)
        MonthlySnapshot existing = snapshotDao.getSnapshotForMonth(month, year);
        if (existing != null) {
            categorySnapshotDao.deleteForSnapshot(existing.id);
            snapshotDao.delete(existing);
        }

        // Insert new MonthlySnapshot and get its auto-generated id
        MonthlySnapshot snapshot = new MonthlySnapshot(month, year, budget, totalSpent, 0.0);
        long newId = snapshotDao.insert(snapshot);

        // Insert CategorySnapshot rows for each category
        if (catSummaries != null && !catSummaries.isEmpty()) {
            List<CategorySnapshot> catSnapshots = new ArrayList<>();
            for (CategorySummary cs : catSummaries) {
                double pct = totalSpent > 0 ? (cs.totalAmount / totalSpent) * 100 : 0;
                catSnapshots.add(new CategorySnapshot(
                        (int) newId,
                        cs.categoryName,
                        cs.iconName,
                        cs.totalAmount,
                        pct,
                        cs.count
                ));
            }
            categorySnapshotDao.insertAll(catSnapshots);
        }
    }

    /**
     * Convenience method — generates snapshot for the current month.
     * Call from BudgetHistoryActivity's "Save This Month" button.
     */
    public void generateSnapshotForCurrentMonth(double budget) {
        Calendar cal = Calendar.getInstance();
        generateSnapshot(
                cal.get(Calendar.MONTH) + 1,  // Calendar.MONTH is 0-indexed
                cal.get(Calendar.YEAR),
                budget
        );
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteSnapshot(MonthlySnapshot snapshot) {
        snapshotDao.delete(snapshot);
        // CategorySnapshots are cascade-deleted by the FK constraint
    }

    public void deleteAll() {
        AppDatabase.dbExecutor.execute(() -> {
            snapshotDao.deleteAll();
            categorySnapshotDao.deleteAll();
        });
    }
}
