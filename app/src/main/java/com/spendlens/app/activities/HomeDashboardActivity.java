package com.spendlens.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.spendlens.app.R;
import com.spendlens.app.adapters.ExpenseAdapter;
import com.spendlens.app.ai.AiConfig;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.databinding.ActivityHomeDashboardBinding;
import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.utils.BudgetCalculator;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;
import com.spendlens.app.utils.ExpenseCsvExporter;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.viewmodels.ExpenseViewModel;
import com.spendlens.app.viewmodels.UserProfileViewModel;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeDashboardActivity extends AppCompatActivity {

    private ActivityHomeDashboardBinding binding;
    private ExpenseViewModel expenseVm;
    private UserProfileViewModel profileVm;
    private ExpenseAdapter adapter;
    private String currency = "RM";
    private double monthlyBudget = 0;
    private double lastSpent = 0;

    private final ActivityResultLauncher<String> exportCsvLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    exportExpensesToCsv(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expenseVm = new ViewModelProvider(this).get(ExpenseViewModel.class);
        profileVm = new ViewModelProvider(this).get(UserProfileViewModel.class);
        currency = PrefsManager.getInstance(this).getCurrency();

        setupRecyclerView();
        setupClickListeners();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(expense -> {
            Intent intent = new Intent(this, ExpenseDetailActivity.class);
            intent.putExtra("expense_id", expense.expenseId);
            startActivity(intent);
        });
        adapter.setCurrencySymbol(currency);
        binding.rvRecentExpenses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentExpenses.setAdapter(adapter);
        binding.rvRecentExpenses.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        binding.fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));

        binding.tvSeeAll.setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseHistoryActivity.class)));

        binding.cardQuickImport.setOnClickListener(v ->
                startActivity(new Intent(this, UploadStatementActivity.class)));

        binding.cardQuickExport.setOnClickListener(v ->
                exportCsvLauncher.launch(buildExportFileName()));

        binding.cardQuickCopilot.setOnClickListener(v -> openBudgetCopilot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, ExpenseHistoryActivity.class));
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                return true;
            } else if (id == R.id.nav_analysis) {
                startActivity(new Intent(this, AnalysisActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return true;
        });
        binding.bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    private void observeData() {
        profileVm.getProfile().observe(this, profile -> {
            if (profile == null) return;
            monthlyBudget = profile.monthlyBudget;
            currency = profile.currency;
            adapter.setCurrencySymbol(currency);
            binding.tvGreeting.setText(profile.name);
            bindHeroCard(lastSpent);
        });

        expenseVm.getTotalThisMonth().observe(this, spent -> {
            lastSpent = spent != null ? spent : 0.0;
            bindHeroCard(lastSpent);
        });

        expenseVm.getTotalToday().observe(this, today -> {
            double totalToday = today != null ? today : 0.0;
            binding.statToday.tvStatLabel.setText("TODAY");
            binding.statToday.tvStatValue.setText(CurrencyUtils.formatSmart(currency, totalToday));
        });

        expenseVm.getTotalThisWeek().observe(this, week -> {
            double totalWeek = week != null ? week : 0.0;
            binding.statWeek.tvStatLabel.setText("THIS WEEK");
            binding.statWeek.tvStatValue.setText(CurrencyUtils.formatSmart(currency, totalWeek));
        });

        expenseVm.getAllExpenses().observe(this, expenses -> {
            if (expenses == null || expenses.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.rvRecentExpenses.setVisibility(View.GONE);
                adapter.submitList(null);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.rvRecentExpenses.setVisibility(View.VISIBLE);
                List<Expense> recent = expenses.size() > 5
                        ? expenses.subList(0, 5)
                        : expenses;
                adapter.submitList(recent);
            }
        });
    }

    private void bindHeroCard(double spent) {
        if (monthlyBudget <= 0) return;

        binding.tvTotalSpent.setText(CurrencyUtils.format(currency, spent));
        binding.tvBudgetSubtitle.setText(
                "spent of " + CurrencyUtils.formatSmart(currency, monthlyBudget) + " budget");

        int progress = (int) (BudgetCalculator.getProgressFraction(monthlyBudget, spent) * 100);
        binding.progressBudget.setProgress(progress);
        binding.tvSpentPercent.setText(
                (int) BudgetCalculator.getSpentPercent(monthlyBudget, spent) + "% used");
        binding.tvDaysLeft.setText(DateUtils.getDaysRemainingThisMonth() + " days left");

        double remaining = BudgetCalculator.getRemaining(monthlyBudget, spent);
        binding.statRemain.tvStatLabel.setText("REMAINING");
        if (remaining < 0) {
            binding.statRemain.tvStatValue.setText(
                    "-" + CurrencyUtils.formatSmart(currency, Math.abs(remaining)));
            binding.statRemain.tvStatValue.setTextColor(getColor(R.color.red_600));
        } else {
            binding.statRemain.tvStatValue.setText(CurrencyUtils.formatSmart(currency, remaining));
            binding.statRemain.tvStatValue.setTextColor(getColor(R.color.gray_900));
        }

        int risk = BudgetCalculator.getAdjustedRiskLevel(
                monthlyBudget,
                spent,
                DateUtils.getDaysElapsedThisMonth(),
                DateUtils.getTotalDaysThisMonth()
        );
        binding.statRisk.tvStatLabel.setText("RISK LEVEL");
        binding.statRisk.tvStatValue.setText(BudgetCalculator.getRiskLabel(risk));
        binding.statRisk.tvStatValue.setTextColor(getRiskColor(risk));
    }

    private int getRiskColor(int risk) {
        switch (risk) {
            case BudgetCalculator.RISK_CRITICAL:
                return getColor(R.color.red_primary);
            case BudgetCalculator.RISK_WARNING:
                return getColor(R.color.risk_warning);
            case BudgetCalculator.RISK_MODERATE:
                return getColor(R.color.amber_primary);
            default:
                return getColor(R.color.green_primary);
        }
    }

    private String buildExportFileName() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date(System.currentTimeMillis()));
        return "spendlens-expenses-" + date + ".csv";
    }

    private void openBudgetCopilot() {
        AiConfig config = PrefsManager.getInstance(this).getAiConfig();
        if (config.canUseBudgetCopilot()) {
            startActivity(new Intent(this, BudgetCopilotActivity.class));
        } else {
            Toast.makeText(
                    this,
                    "Load an on-device AI model first.",
                    Toast.LENGTH_SHORT
            ).show();
            startActivity(new Intent(this, AiSettingsActivity.class));
        }
    }

    private void exportExpensesToCsv(Uri uri) {
        Toast.makeText(this, "Preparing CSV export...", Toast.LENGTH_SHORT).show();

        AppDatabase.dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                List<Expense> expenses = db.expenseDao().getAllExpensesSync();
                if (expenses.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(
                            this,
                            "No expenses to export yet.",
                            Toast.LENGTH_SHORT
                    ).show());
                    return;
                }

                List<Category> categories = db.categoryDao().getAllCategoriesSync();
                Map<Integer, String> categoryNames = new HashMap<>();
                for (Category category : categories) {
                    categoryNames.put(category.categoryId, category.categoryName);
                }

                String csv = ExpenseCsvExporter.buildCsv(expenses, categoryNames, currency);
                OutputStream stream = getContentResolver().openOutputStream(uri);
                if (stream == null) {
                    throw new IllegalStateException("Could not open export file.");
                }

                try (OutputStream output = stream) {
                    output.write(csv.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }

                runOnUiThread(() -> Toast.makeText(
                        this,
                        "CSV export saved",
                        Toast.LENGTH_SHORT
                ).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Export failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                        Toast.LENGTH_LONG
                ).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }
}
