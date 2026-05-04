package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spendlens.app.R;
import com.spendlens.app.adapters.MonthCardAdapter;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.*;
import com.spendlens.app.utils.*;
import com.spendlens.app.viewmodels.*;

import java.util.Calendar;
import java.util.List;

public class BudgetHistoryActivity extends AppCompatActivity {

    private SnapshotViewModel viewModel;
    private MonthCardAdapter adapter;
    private String currency;

    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_history);

        currency = PrefsManager.getInstance(this).getCurrency();
        viewModel = new ViewModelProvider(this).get(SnapshotViewModel.class);

        ((ImageButton) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvMonths);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MonthCardAdapter(this::showMonthDetail);
        adapter.setCurrency(currency);
        rv.setAdapter(adapter);

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        viewModel.getAllSnapshots().observe(this, snapshots -> {
            adapter.submitList(snapshots);
            tvEmpty.setVisibility(snapshots == null || snapshots.isEmpty()
                    ? View.VISIBLE : View.GONE);
        });

        // FAB — long press = pick past month, single tap = save current month
        FloatingActionButton fab = findViewById(R.id.fabSaveMonth);
        fab.setOnClickListener(v -> confirmSaveCurrentMonth());
        fab.setOnLongClickListener(v -> {
            showPastMonthPicker();
            return true;
        });

        // Import Statement button
        com.google.android.material.button.MaterialButton btnImport =
                findViewById(R.id.btnImportStatement);
        if (btnImport != null)
            btnImport.setOnClickListener(v ->
                    startActivity(new Intent(this, UploadStatementActivity.class)));

        new ViewModelProvider(this).get(UserProfileViewModel.class)
                .getProfile().observe(this, profile -> {
                    if (profile != null) {
                        currency = profile.currency;
                        adapter.setCurrency(currency);
                    }
                });
    }

    // ── Save current month ────────────────────────────────────────────────────

    private void confirmSaveCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        String monthName = MONTH_NAMES[cal.get(Calendar.MONTH)];
        int year = cal.get(Calendar.YEAR);

        new AlertDialog.Builder(this)
                .setTitle("Save " + monthName + " " + year + "?")
                .setMessage("Saves a snapshot of your current month's spending.\n"
                        + "If a snapshot already exists it will be replaced.\n\n"
                        + "To save a past month, long press the + button.")
                .setPositiveButton("Save", (d, w) -> {
                    viewModel.generateCurrentMonthSnapshot();
                    Toast.makeText(this, monthName + " snapshot saved!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Past month picker ─────────────────────────────────────────────────────

    private void showPastMonthPicker() {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH); // 0-based
        int currentYear  = now.get(Calendar.YEAR);

        // Build a list of the last 12 months excluding the current month
        String[] options = new String[12];
        final int[] months = new int[12];
        final int[] years  = new int[12];

        for (int i = 0; i < 12; i++) {
            int m = currentMonth - 1 - i; // start from last month
            int y = currentYear;
            while (m < 0) { m += 12; y--; }
            months[i] = m + 1; // convert to 1-based
            years[i]  = y;
            options[i] = MONTH_NAMES[m] + " " + y;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select a past month to save")
                .setItems(options, (dialog, which) -> {
                    int selectedMonth = months[which];
                    int selectedYear  = years[which];
                    String label = options[which];
                    confirmSavePastMonth(selectedMonth, selectedYear, label);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmSavePastMonth(int month, int year, String label) {
        new AlertDialog.Builder(this)
                .setTitle("Save " + label + "?")
                .setMessage("This will calculate the total spending for " + label
                        + " using your expense history and save it as a snapshot.\n\n"
                        + "If a snapshot for " + label + " already exists it will be replaced.")
                .setPositiveButton("Save", (d, w) -> {
                    AppDatabase.dbExecutor.execute(() -> {
                        // Get current budget to use as the planned budget for the snapshot
                        com.spendlens.app.entities.UserProfile profile =
                                AppDatabase.getInstance(this).userProfileDao().getProfileSync();
                        double budget = profile != null ? profile.monthlyBudget : 0;
                        viewModel.generateSnapshot(month, year, budget);
                        runOnUiThread(() ->
                                Toast.makeText(this, label + " snapshot saved!", Toast.LENGTH_SHORT).show()
                        );
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Show month detail ─────────────────────────────────────────────────────

    private void showMonthDetail(MonthlySnapshot snapshot) {
        AppDatabase.dbExecutor.execute(() -> {
            List<CategorySnapshot> cats = viewModel.getCategoriesForSnapshot(snapshot.id);
            runOnUiThread(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Total: ").append(CurrencyUtils.formatSmart(currency, snapshot.totalSpent))
                        .append("\nBudget: ").append(CurrencyUtils.formatSmart(currency, snapshot.plannedBudget))
                        .append("\n");
                if (snapshot.savings >= 0)
                    sb.append("Saved: ").append(CurrencyUtils.formatSmart(currency, snapshot.savings));
                else
                    sb.append("Overspent: ").append(CurrencyUtils.formatSmart(currency, Math.abs(snapshot.savings)));

                sb.append("\n\nBy category:");
                if (cats != null)
                    for (CategorySnapshot c : cats)
                        sb.append("\n  \u2022 ").append(c.categoryName)
                                .append(" \u2014 ").append(CurrencyUtils.formatSmart(currency, c.totalSpent))
                                .append(" (").append((int) c.percentage).append("%)");

                new AlertDialog.Builder(this)
                        .setTitle(snapshot.getDisplayLabel())
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Delete", (d, w) -> confirmDelete(snapshot))
                        .show();
            });
        });
    }

    // ── Delete snapshot ───────────────────────────────────────────────────────

    private void confirmDelete(MonthlySnapshot snapshot) {
        new AlertDialog.Builder(this)
                .setTitle("Delete snapshot?")
                .setMessage("Permanently delete " + snapshot.getDisplayLabel() + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    viewModel.deleteSnapshot(snapshot);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}