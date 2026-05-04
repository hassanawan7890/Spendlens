package com.spendlens.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.spendlens.app.R;
import com.spendlens.app.ai.AiConfig;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.database.DatabaseSeeder;
import com.spendlens.app.databinding.ActivitySettingsBinding;
import com.spendlens.app.databinding.ItemSettingsRowBinding;
import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.notifications.BudgetNotificationManager;
import com.spendlens.app.utils.AppLockManager;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.ExpenseCsvExporter;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.viewmodels.SnapshotViewModel;
import com.spendlens.app.viewmodels.UserProfileViewModel;
import com.spendlens.app.widget.WidgetRefreshHelper;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private UserProfileViewModel profileVm;
    private SnapshotViewModel snapshotVm;
    private AppLockManager lockManager;
    private PrefsManager prefs;
    private BudgetNotificationManager notifManager;
    private UserProfile currentProfile;

    private static final int REQ_LOCK_SETUP = 101;

    private static final String[] CURRENCY_SYMBOLS = {
            "RM", "$", "\u20ac", "\u00a3", "\u00a5", "SGD", "IDR", "THB", "CAD"
    };
    private static final String[] CURRENCY_NAMES = {
            "Malaysian Ringgit (RM)", "US Dollar ($)", "Euro (\u20ac)", "British Pound (\u00a3)",
            "Japanese Yen (\u00a5)", "Singapore Dollar (SGD)", "Indonesian Rupiah (IDR)",
            "Thai Baht (THB)", "Canadian Dollar (CAD)"
    };

    private final ActivityResultLauncher<String> exportCsvLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    exportExpensesToCsv(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileVm = new ViewModelProvider(this).get(UserProfileViewModel.class);
        snapshotVm = new ViewModelProvider(this).get(SnapshotViewModel.class);
        lockManager = AppLockManager.getInstance(this);
        prefs = PrefsManager.getInstance(this);
        notifManager = new BudgetNotificationManager(this);

        setupRows();
        binding.btnBack.setOnClickListener(v -> finish());
        observeProfile();
        binding.tvProfileInfo.setOnLongClickListener(v -> {
            confirmReset();
            return true;
        });
    }

    private void setupRows() {
        configRow(binding.rowEditProfile, "\uD83D\uDC64", "Edit Profile",
                () -> startActivity(new Intent(this, EditProfileActivity.class)));
        configRow(binding.rowChangeBudget, "\uD83D\uDCB0", "Change Budget",
                this::showChangeBudgetDialog);
        configRow(binding.rowChangeCurrency, "\uD83D\uDCB1", "Currency",
                this::showCurrencyPicker);
        configRow(binding.rowManageCategories, "\uD83C\uDFF7", "Categories",
                () -> startActivity(new Intent(this, ManageCategoriesActivity.class)));
        configRow(binding.rowBudgetHistory, "\uD83D\uDCC5", "Budget History",
                () -> startActivity(new Intent(this, BudgetHistoryActivity.class)));
        configRow(binding.rowExportData, "\uD83D\uDCE4", "Export CSV",
                () -> exportCsvLauncher.launch(buildExportFileName()));
        binding.rowExportData.tvRowValue.setText("Ready");
        configRow(binding.rowAiSettings, "\uD83E\uDDE0", "On-device AI",
                () -> startActivity(new Intent(this, AiSettingsActivity.class)));
        updateAiRow();

        updateLockRow();
        binding.rowResetData.getRoot().setOnClickListener(v -> handleLockTap());
        updateNotifRow();
        binding.rowNotifications.getRoot().setOnClickListener(v -> toggleNotifications());
    }

    private void observeProfile() {
        profileVm.getProfile().observe(this, profile -> {
            if (profile == null) return;
            currentProfile = profile;

            String themeLabel = prefs.isDarkMode()
                    ? "\uD83C\uDF19 Dark (tap to switch Light)"
                    : "\u2600\uFE0F Light (tap to switch Dark)";

            binding.tvProfileInfo.setText(
                    profile.name + "  \u00b7  "
                            + CurrencyUtils.formatSmart(profile.currency, profile.monthlyBudget)
                            + " / mo\nTheme: " + themeLabel
            );
            binding.tvProfileInfo.setOnClickListener(v -> toggleTheme());
            binding.rowChangeCurrency.tvRowValue.setText(profile.currency);
            binding.rowChangeBudget.tvRowValue.setText(
                    CurrencyUtils.formatSmart(profile.currency, profile.monthlyBudget));
        });
    }

    private void updateNotifRow() {
        binding.rowNotifications.tvRowIcon.setText("\uD83D\uDD14");
        binding.rowNotifications.tvRowLabel.setText("Budget Alerts");
        binding.rowNotifications.tvRowValue.setText(
                notifManager.isNotificationsEnabled() ? "On" : "Off");
    }

    private void updateAiRow() {
        AiConfig config = prefs.getAiConfig();
        String value;
        if (!config.enabled) {
            value = "Off";
        } else if (!config.isConfigured()) {
            value = "Load model";
        } else if (config.statementImportEnabled && config.budgetCopilotEnabled) {
            value = "Import + Chat";
        } else if (config.statementImportEnabled) {
            value = "Import only";
        } else if (config.budgetCopilotEnabled) {
            value = "Chat only";
        } else {
            value = "Local ready";
        }
        binding.rowAiSettings.tvRowValue.setText(value);
    }

    private void toggleNotifications() {
        boolean current = notifManager.isNotificationsEnabled();
        notifManager.setNotificationsEnabled(!current);
        updateNotifRow();
        Toast.makeText(
                this,
                !current ? "Budget alerts enabled" : "Budget alerts disabled",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateLockRow() {
        binding.rowResetData.tvRowIcon.setText("\uD83D\uDD12");
        binding.rowResetData.tvRowLabel.setText("App Lock");
        binding.rowResetData.tvRowValue.setText(lockManager.isLockEnabled()
                ? "On \u2022 " + (lockManager.isPinMode() ? "PIN" : "Password")
                : "Off");
    }

    private void handleLockTap() {
        if (lockManager.isLockEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("App Lock")
                    .setItems(new String[]{"Change PIN / Password", "Disable lock"}, (d, which) -> {
                        if (which == 0) {
                            startActivityForResult(
                                    new Intent(this, LockSetupActivity.class),
                                    REQ_LOCK_SETUP
                            );
                        } else {
                            confirmDisableLock();
                        }
                    })
                    .show();
        } else {
            startActivityForResult(new Intent(this, LockSetupActivity.class), REQ_LOCK_SETUP);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOCK_SETUP && resultCode == RESULT_OK) {
            updateLockRow();
        }
    }

    private void confirmDisableLock() {
        new AlertDialog.Builder(this)
                .setTitle("Disable app lock?")
                .setMessage("Anyone will be able to open SpendLens.")
                .setPositiveButton("Disable", (d, which) -> {
                    lockManager.clearCredential();
                    updateLockRow();
                    Toast.makeText(this, "App lock disabled", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void configRow(ItemSettingsRowBinding row, String icon, String label, Runnable onClick) {
        row.tvRowIcon.setText(icon);
        row.tvRowLabel.setText(label);
        row.getRoot().setOnClickListener(v -> onClick.run());
    }

    private void toggleTheme() {
        String newMode = prefs.isDarkMode() ? "light" : "dark";
        prefs.setThemeMode(newMode);
        AppCompatDelegate.setDefaultNightMode("light".equals(newMode)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES);
        recreate();
    }

    private void showChangeBudgetDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_change_budget, null);
        TextInputEditText edit = view.findViewById(R.id.editNewBudget);

        if (currentProfile != null) {
            edit.setText(CurrencyUtils.formatPlain(currentProfile.monthlyBudget));
        }

        new AlertDialog.Builder(this)
                .setTitle("Change Monthly Budget")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String value = edit.getText() != null ? edit.getText().toString() : "";
                    double newBudget = CurrencyUtils.parse(value);
                    if (newBudget > 0) {
                        profileVm.updateBudget(newBudget);
                        snapshotVm.generateCurrentMonthSnapshot();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCurrencyPicker() {
        new AlertDialog.Builder(this)
                .setTitle("Select Currency")
                .setItems(CURRENCY_NAMES, (d, which) -> {
                    String symbol = CURRENCY_SYMBOLS[which];
                    profileVm.updateCurrency(symbol);
                    prefs.setCurrency(symbol);
                })
                .show();
    }

    private String buildExportFileName() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date(System.currentTimeMillis()));
        return "spendlens-expenses-" + date + ".csv";
    }

    private void exportExpensesToCsv(Uri uri) {
        binding.rowExportData.tvRowValue.setText("Exporting...");

        AppDatabase.dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                List<Expense> expenses = db.expenseDao().getAllExpensesSync();
                List<Category> categories = db.categoryDao().getAllCategoriesSync();
                Map<Integer, String> categoryNames = new HashMap<>();

                for (Category category : categories) {
                    categoryNames.put(category.categoryId, category.categoryName);
                }

                String activeCurrency = currentProfile != null && currentProfile.currency != null
                        ? currentProfile.currency
                        : prefs.getCurrency();

                String csv = ExpenseCsvExporter.buildCsv(expenses, categoryNames, activeCurrency);
                OutputStream stream = getContentResolver().openOutputStream(uri);
                if (stream == null) throw new IllegalStateException("Could not open export file.");

                try (OutputStream output = stream) {
                    output.write(csv.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }

                runOnUiThread(() -> {
                    binding.rowExportData.tvRowValue.setText(expenses.size() + " rows");
                    Toast.makeText(this, "CSV export saved", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.rowExportData.tvRowValue.setText("Retry");
                    Toast.makeText(
                            this,
                            "Export failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Reset all data?")
                .setMessage("Permanently deletes all expenses, categories, and profile. Cannot be undone.")
                .setPositiveButton("Yes, reset everything", (d, which) ->
                        AppDatabase.dbExecutor.execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);

                            db.expenseDao().deleteAll();
                            db.categorySnapshotDao().deleteAll();
                            db.monthlySnapshotDao().deleteAll();
                            db.statementImportDao().deleteAll();
                            db.userProfileDao().deleteAll();
                            db.categoryDao().deleteAllIncludingDefaults();
                            DatabaseSeeder.seedCategories(db.categoryDao());

                            lockManager.clearCredential();
                            prefs.clearAll();
                            notifManager.resetNotificationState();
                            WidgetRefreshHelper.refresh(this);

                            runOnUiThread(() -> {
                                Intent intent = new Intent(this, WelcomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAiRow();
    }
}
