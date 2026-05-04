package com.spendlens.app.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.databinding.ActivityAddExpenseBinding;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.fragments.CategoryPickerBottomSheet;
import com.spendlens.app.notifications.BudgetNotificationManager;
import com.spendlens.app.utils.CategoryDisplayUtils;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;
import com.spendlens.app.utils.ValidationUtils;
import com.spendlens.app.viewmodels.ExpenseViewModel;
import com.spendlens.app.viewmodels.UserProfileViewModel;
import com.spendlens.app.widget.WidgetRefreshHelper;

import java.util.Calendar;

public class AddExpenseActivity extends AppCompatActivity {

    private ActivityAddExpenseBinding binding;
    private ExpenseViewModel viewModel;
    private BudgetNotificationManager notifManager;

    private double monthlyBudget = 0;
    private String currency = "CAD";

    private int selectedCategoryId = -1;
    private String selectedCategoryName = "";
    private long selectedDateMs;

    private boolean isEditMode = false;
    private Expense existingExpense = null;

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> { /* handled silently */ });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding      = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel    = new ViewModelProvider(this).get(ExpenseViewModel.class);
        notifManager = new BudgetNotificationManager(this);
        selectedDateMs = DateUtils.now();
        binding.tvDate.setText(DateUtils.formatDate(selectedDateMs));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        observeProfile();

        int expenseId = getIntent().getIntExtra("expense_id", -1);
        if (expenseId != -1) {
            isEditMode = true;
            binding.tvTitle.setText("Edit Expense");
            binding.btnSave.setText("Update Expense");
            loadExpenseForEditing(expenseId);
        }
        setupClickListeners();
    }

    private void observeProfile() {
        new ViewModelProvider(this).get(UserProfileViewModel.class)
                .getProfile().observe(this, profile -> {
                    if (profile == null) return;
                    monthlyBudget = profile.monthlyBudget;
                    currency = profile.currency != null ? profile.currency : "CAD";
                });
    }

    private void loadExpenseForEditing(int expenseId) {
        viewModel.getById(expenseId).observe(this, expense -> {
            if (expense == null || existingExpense != null) return;
            existingExpense = expense;
            binding.editTitle.setText(expense.title);
            binding.editAmount.setText(CurrencyUtils.formatPlain(expense.amount));
            binding.editNote.setText(expense.note != null ? expense.note : "");
            selectedDateMs = expense.date;
            binding.tvDate.setText(DateUtils.formatDate(selectedDateMs));
            selectedCategoryId = expense.categoryId;
            AppDatabase.dbExecutor.execute(() -> {
                com.spendlens.app.entities.Category cat =
                        AppDatabase.getInstance(this).categoryDao().getCategoryByIdSync(expense.categoryId);
                runOnUiThread(() -> {
                    if (cat != null) {
                        selectedCategoryName = cat.categoryName;
                        binding.tvCategoryIcon.setText(
                                CategoryDisplayUtils.getEmojiForIconName(cat.iconName));
                        binding.tvCategoryName.setText(cat.categoryName);
                        binding.tvCategoryName.setTextColor(getColor(R.color.gray_900));
                    } else {
                        binding.tvCategoryIcon.setText(
                                CategoryDisplayUtils.getEmojiForIconName(null));
                    }
                });
            });
            selectMoodChip(expense.moodTag);
            selectPaymentChip(expense.paymentMethod);
        });
    }

    private void selectMoodChip(String mood) {
        if (mood == null) return;
        int chipId;
        switch (mood) {
            case Expense.MOOD_NEED:         chipId = R.id.chipNeed;         break;
            case Expense.MOOD_WANT:         chipId = R.id.chipWant;         break;
            case Expense.MOOD_IMPULSE:      chipId = R.id.chipImpulse;      break;
            case Expense.MOOD_SOCIAL:       chipId = R.id.chipSocial;       break;
            case Expense.MOOD_SUBSCRIPTION: chipId = R.id.chipSubscription; break;
            case Expense.MOOD_EMERGENCY:    chipId = R.id.chipEmergency;    break;
            default: return;
        }
        binding.chipGroupMood.check(chipId);
    }

    private void selectPaymentChip(String payment) {
        if (payment == null) return;
        int chipId;
        switch (payment) {
            case Expense.PAY_CARD:    chipId = R.id.chipCard;    break;
            case Expense.PAY_EWALLET: chipId = R.id.chipEWallet; break;
            default:                  chipId = R.id.chipCash;    break;
        }
        binding.chipGroupPayment.check(chipId);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPickCategory.setOnClickListener(v -> {
            CategoryPickerBottomSheet sheet = new CategoryPickerBottomSheet();
            sheet.setOnCategorySelected((cat, iconEmoji) -> {
                selectedCategoryId   = cat.categoryId;
                selectedCategoryName = cat.categoryName;
                binding.tvCategoryIcon.setText(
                        CategoryDisplayUtils.getEmojiForIconName(cat.iconName));
                binding.tvCategoryName.setText(cat.categoryName);
                binding.tvCategoryName.setTextColor(getColor(R.color.gray_900));
            });
            sheet.show(getSupportFragmentManager(), "cat_picker");
        });
        binding.btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDateMs);
            new DatePickerDialog(this, (dp, y, m, d) -> {
                cal.set(y, m, d);
                selectedDateMs = cal.getTimeInMillis();
                binding.tvDate.setText(DateUtils.formatDate(selectedDateMs));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        binding.btnSave.setOnClickListener(v -> attemptSave());
    }

    private void attemptSave() {
        String amountStr = getText(binding.editAmount);
        String title     = getText(binding.editTitle);

        boolean amountOk = ValidationUtils.requirePositiveAmount(
                binding.layoutAmount, amountStr, "Enter an amount", "Enter a valid amount > 0");
        boolean titleOk = ValidationUtils.requireNonEmpty(
                binding.layoutTitle, title, "Enter a title");
        if (!amountOk || !titleOk) return;

        if (selectedCategoryId < 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        String mood = getSelectedMood();
        if (mood == null) {
            Toast.makeText(this, "Please select a spending mood", Toast.LENGTH_SHORT).show();
            return;
        }

        String payment = getSelectedPayment();
        String note    = getText(binding.editNote);

        if (isEditMode && existingExpense != null) {
            existingExpense.title         = title.trim();
            existingExpense.amount        = CurrencyUtils.parse(amountStr);
            existingExpense.categoryId    = selectedCategoryId;
            existingExpense.date          = selectedDateMs;
            existingExpense.note          = note.isEmpty() ? null : note.trim();
            existingExpense.moodTag       = mood;
            existingExpense.paymentMethod = payment;
            viewModel.update(existingExpense);
            Toast.makeText(this, "Expense updated!", Toast.LENGTH_SHORT).show();
        } else {
            Expense expense = new Expense(title.trim(), CurrencyUtils.parse(amountStr),
                    selectedCategoryId, selectedDateMs,
                    note.isEmpty() ? null : note.trim(), mood, payment);
            viewModel.insert(expense);
            Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show();
        }

        WidgetRefreshHelper.refresh(this);
        checkBudgetAndNotify();
        finish();
    }

    private void checkBudgetAndNotify() {
        if (monthlyBudget <= 0) return;
        AppDatabase.dbExecutor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            double totalSpent = AppDatabase.getInstance(this)
                    .expenseDao().getTotalBetweenSync(cal.getTimeInMillis(), System.currentTimeMillis());
            runOnUiThread(() -> notifManager.checkAndNotify(totalSpent, monthlyBudget, currency));
        });
    }

    private String getSelectedMood() {
        int id = binding.chipGroupMood.getCheckedChipId();
        if (id == R.id.chipNeed)         return Expense.MOOD_NEED;
        if (id == R.id.chipWant)         return Expense.MOOD_WANT;
        if (id == R.id.chipImpulse)      return Expense.MOOD_IMPULSE;
        if (id == R.id.chipSocial)       return Expense.MOOD_SOCIAL;
        if (id == R.id.chipSubscription) return Expense.MOOD_SUBSCRIPTION;
        if (id == R.id.chipEmergency)    return Expense.MOOD_EMERGENCY;
        return null;
    }

    private String getSelectedPayment() {
        int id = binding.chipGroupPayment.getCheckedChipId();
        if (id == R.id.chipCard)    return Expense.PAY_CARD;
        if (id == R.id.chipEWallet) return Expense.PAY_EWALLET;
        return Expense.PAY_CASH;
    }

    private String getText(com.google.android.material.textfield.TextInputEditText f) {
        return f.getText() != null ? f.getText().toString() : "";
    }
}
