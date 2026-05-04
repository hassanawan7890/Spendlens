package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.databinding.ActivityExpenseDetailBinding;
import com.spendlens.app.databinding.ItemDetailRowBinding;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.viewmodels.ExpenseViewModel;
import com.spendlens.app.widget.WidgetRefreshHelper;

public class ExpenseDetailActivity extends AppCompatActivity {

    private ActivityExpenseDetailBinding binding;
    private ExpenseViewModel expenseVm;
    private Expense currentExpense;
    private String currency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = ActivityExpenseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expenseVm = new ViewModelProvider(this).get(ExpenseViewModel.class);
        currency  = PrefsManager.getInstance(this).getCurrency();

        int expenseId = getIntent().getIntExtra("expense_id", -1);
        if (expenseId < 0) { finish(); return; }

        observeExpense(expenseId);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, AddExpenseActivity.class);
            i.putExtra("expense_id", expenseId);
            startActivity(i);
        });
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void observeExpense(int id) {
        expenseVm.getById(id).observe(this, expense -> {
            if (expense == null) return;
            currentExpense = expense;
            bindExpense(expense);
        });
    }

    private void bindExpense(Expense e) {
        binding.tvAmount.setText(CurrencyUtils.format(currency, e.amount));
        binding.tvDetailTitle.setText(e.title);

        AppDatabase.dbExecutor.execute(() -> {
            com.spendlens.app.entities.Category cat =
                    AppDatabase.getInstance(this).categoryDao().getCategoryByIdSync(e.categoryId);
            runOnUiThread(() -> {
                binding.rowCategory.tvRowLabel.setText("Category");
                binding.rowCategory.tvRowValue.setText(cat != null ? cat.categoryName : "Unknown");
            });
        });

        bindRow(binding.rowDate,    "Date",    DateUtils.formatDate(e.date));
        bindRow(binding.rowMood,    "Mood",    e.moodTag != null ? e.moodTag : "—");
        bindRow(binding.rowPayment, "Payment", e.paymentMethod != null ? e.paymentMethod : "—");
        bindRow(binding.rowNote,    "Note",    (e.note != null && !e.note.isEmpty()) ? e.note : "—");
    }

    private void bindRow(ItemDetailRowBinding row, String label, String value) {
        row.tvRowLabel.setText(label);
        row.tvRowValue.setText(value);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete expense?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    if (currentExpense != null) {
                        expenseVm.delete(currentExpense);
                        // Refresh widget so deleted amount is reflected immediately
                        WidgetRefreshHelper.refresh(this);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}