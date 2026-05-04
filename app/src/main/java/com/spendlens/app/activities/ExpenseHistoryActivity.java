package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.spendlens.app.R;
import com.spendlens.app.adapters.ExpenseAdapter;
import com.spendlens.app.databinding.ActivityExpenseHistoryBinding;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.viewmodels.ExpenseViewModel;
import com.spendlens.app.widget.WidgetRefreshHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExpenseHistoryActivity extends AppCompatActivity {

    private ActivityExpenseHistoryBinding binding;
    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;
    private List<Expense> allExpenses = new ArrayList<>();
    private String activeMoodFilter = "";
    private String currency = "CAD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpenseHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        currency = PrefsManager.getInstance(this).getCurrency();

        adapter = new ExpenseAdapter(expense -> {
            Intent intent = new Intent(this, ExpenseDetailActivity.class);
            intent.putExtra("expense_id", expense.expenseId);
            startActivity(intent);
        });
        adapter.setCurrencySymbol(currency);

        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExpenses.setAdapter(adapter);

        setupSearch();
        setupFilters();
        setupSwipeToDelete();
        observeExpenses();

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void observeExpenses() {
        viewModel.getAllExpenses().observe(this, expenses -> {
            allExpenses = expenses != null ? expenses : new ArrayList<>();
            applyFilters();
        });
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty() || ids.get(0) == R.id.chipAll) {
                activeMoodFilter = "";
            } else {
                int id = ids.get(0);
                if (id == R.id.chipFilterNeed) {
                    activeMoodFilter = Expense.MOOD_NEED;
                } else if (id == R.id.chipFilterWant) {
                    activeMoodFilter = Expense.MOOD_WANT;
                } else if (id == R.id.chipFilterImpulse) {
                    activeMoodFilter = Expense.MOOD_IMPULSE;
                } else if (id == R.id.chipFilterSocial) {
                    activeMoodFilter = Expense.MOOD_SOCIAL;
                } else if (id == R.id.chipFilterSubscription) {
                    activeMoodFilter = Expense.MOOD_SUBSCRIPTION;
                } else if (id == R.id.chipFilterEmergency) {
                    activeMoodFilter = Expense.MOOD_EMERGENCY;
                } else {
                    activeMoodFilter = "";
                }
            }
            applyFilters();
        });
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                Expense deleted = adapter.getExpenseAt(position);
                viewModel.delete(deleted);
                WidgetRefreshHelper.refresh(ExpenseHistoryActivity.this);

                Snackbar.make(binding.getRoot(), "Expense deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> {
                            viewModel.insert(deleted);
                            WidgetRefreshHelper.refresh(ExpenseHistoryActivity.this);
                        })
                        .show();
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvExpenses);
    }

    private void applyFilters() {
        String query = binding.editSearch.getText() != null
                ? binding.editSearch.getText().toString().toLowerCase().trim()
                : "";

        List<Expense> filtered = new ArrayList<>();
        for (Expense expense : allExpenses) {
            if (!matchesQuery(expense, query)) continue;
            if (!activeMoodFilter.isEmpty() && !activeMoodFilter.equals(expense.moodTag)) continue;
            filtered.add(expense);
        }

        filtered.sort(Comparator.comparingLong((Expense expense) -> expense.date).reversed());
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        updateMeta(filtered);
    }

    private boolean matchesQuery(Expense expense, String query) {
        if (query.isEmpty()) return true;

        return contains(expense.title, query)
                || contains(expense.note, query)
                || contains(expense.paymentMethod, query)
                || contains(expense.moodTag, query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void updateMeta(List<Expense> filtered) {
        double total = 0;
        for (Expense expense : filtered) {
            total += expense.amount;
        }

        String label = filtered.size() + " expense" + (filtered.size() == 1 ? "" : "s")
                + " | " + CurrencyUtils.formatSmart(currency, total)
                + " | Swipe left to delete";
        binding.tvHistoryMeta.setText(label);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFilters();
    }
}
