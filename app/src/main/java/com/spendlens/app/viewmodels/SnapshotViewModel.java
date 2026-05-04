package com.spendlens.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.CategorySnapshot;
import com.spendlens.app.entities.MonthlySnapshot;
import com.spendlens.app.repository.SnapshotRepository;

import java.util.List;

public class SnapshotViewModel extends AndroidViewModel {

    private final SnapshotRepository repository;
    private final LiveData<List<MonthlySnapshot>> allSnapshots;

    public SnapshotViewModel(@NonNull Application application) {
        super(application);
        repository   = new SnapshotRepository(application);
        allSnapshots = repository.getAllSnapshots();
    }

    public LiveData<List<MonthlySnapshot>> getAllSnapshots() {
        return allSnapshots;
    }

    /**
     * Generates (or regenerates) a snapshot for the current month.
     * Fetches the current budget from UserProfile on the background thread
     * before calling the repository — so the correct budget is captured.
     */
    public void generateCurrentMonthSnapshot() {
        AppDatabase.dbExecutor.execute(() -> {
            com.spendlens.app.entities.UserProfile profile =
                    AppDatabase.getInstance(getApplication()).userProfileDao().getProfileSync();
            double budget = profile != null ? profile.monthlyBudget : 0;
            repository.generateSnapshotForCurrentMonth(budget);
        });
    }

    /**
     * Generates a snapshot for a specific past month.
     * Used if the user wants to backfill history.
     */
    public void generateSnapshot(int month, int year, double budget) {
        AppDatabase.dbExecutor.execute(() ->
                repository.generateSnapshot(month, year, budget));
    }

    /**
     * Loads category breakdown for a snapshot synchronously.
     * Call from background thread — do NOT call on main thread.
     */
    public List<CategorySnapshot> getCategoriesForSnapshot(int snapshotId) {
        return repository.getCategoriesForSnapshot(snapshotId);
    }

    /** Last 3 snapshots for the analysis engine */
    public List<MonthlySnapshot> getLastThreeSnapshots() {
        return repository.getLastNSnapshots(3);
    }

    public void deleteSnapshot(MonthlySnapshot snapshot) {
        AppDatabase.dbExecutor.execute(() -> repository.deleteSnapshot(snapshot));
    }
}
