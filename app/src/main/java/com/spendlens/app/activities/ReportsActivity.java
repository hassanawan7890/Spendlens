package com.spendlens.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spendlens.app.R;
import com.spendlens.app.databinding.ActivityReportsBinding;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.models.MoodSummary;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.viewmodels.ExpenseViewModel;
import com.spendlens.app.viewmodels.UserProfileViewModel;

import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private ActivityReportsBinding binding;
    private ExpenseViewModel expenseVm;
    private UserProfileViewModel profileVm;
    private String currency = "RM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding   = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expenseVm = new ViewModelProvider(this).get(ExpenseViewModel.class);
        profileVm = new ViewModelProvider(this).get(UserProfileViewModel.class);
        currency  = PrefsManager.getInstance(this).getCurrency();

        binding.tvReportMonth.setText(DateUtils.formatMonthYear(DateUtils.now()));
        binding.btnBack.setOnClickListener(v -> finish());

        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMoods.setLayoutManager(new LinearLayoutManager(this));

        profileVm.getProfile().observe(this, p -> {
            if (p != null) currency = p.currency;
        });

        expenseVm.getTotalThisMonth().observe(this, total -> {
            double t = total != null ? total : 0;
            // Direct binding access — no findViewById on <include> bindings
            binding.statTotal.tvStatLabel.setText("TOTAL SPENT");
            binding.statTotal.tvStatValue.setText(CurrencyUtils.formatSmart(currency, t));
        });

        expenseVm.getCountThisMonth().observe(this, count -> {
            binding.statTxCount.tvStatLabel.setText("TRANSACTIONS");
            binding.statTxCount.tvStatValue.setText(String.valueOf(count != null ? count : 0));
        });

        expenseVm.getCategorySummaryThisMonth().observe(this, cats -> {
            if (cats == null) return;
            double grandTotal = cats.stream().mapToDouble(c -> c.totalAmount).sum();
            binding.rvCategories.setAdapter(new CategoryReportAdapter(cats, grandTotal, currency));
        });

        expenseVm.getMoodSummaryThisMonth().observe(this, moods -> {
            if (moods == null) return;
            int total = moods.stream().mapToInt(m -> m.count).sum();
            binding.rvMoods.setAdapter(new MoodReportAdapter(moods, total));
        });
    }

    // ── Category adapter ───────────────────────────────────────────────────
    static class CategoryReportAdapter extends RecyclerView.Adapter<CategoryReportAdapter.VH> {
        private final List<CategorySummary> items;
        private final double grandTotal;
        private final String currency;
        CategoryReportAdapter(List<CategorySummary> items, double gt, String currency) {
            this.items = items; this.grandTotal = gt; this.currency = currency;
        }
        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_category_report, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            CategorySummary c = items.get(pos);
            h.name.setText(c.categoryName);
            h.amount.setText(CurrencyUtils.formatSmart(currency, c.totalAmount));
            int pct = grandTotal > 0 ? (int)((c.totalAmount / grandTotal) * 100) : 0;
            h.progress.setMax(100);
            h.progress.setProgress(pct);
            h.pct.setText(pct + "%");
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView name, amount, pct;
            android.widget.ProgressBar progress;
            VH(View v) {
                super(v);
                name     = v.findViewById(R.id.tvCatName);
                amount   = v.findViewById(R.id.tvCatAmount);
                pct      = v.findViewById(R.id.tvCatPct);
                progress = v.findViewById(R.id.progressCat);
            }
        }
    }

    // ── Mood adapter ───────────────────────────────────────────────────────
    static class MoodReportAdapter extends RecyclerView.Adapter<MoodReportAdapter.VH> {
        private final List<MoodSummary> items;
        private final int totalCount;
        MoodReportAdapter(List<MoodSummary> items, int total) {
            this.items = items; this.totalCount = total;
        }
        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_mood_report, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            MoodSummary m = items.get(pos);
            h.mood.setText(m.moodTag);
            int pct = totalCount > 0 ? (int)((m.count / (float) totalCount) * 100) : 0;
            h.pct.setText(pct + "%  (" + m.count + " transactions)");
            h.progress.setMax(100);
            h.progress.setProgress(pct);
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView mood, pct;
            android.widget.ProgressBar progress;
            VH(View v) {
                super(v);
                mood     = v.findViewById(R.id.tvMoodTag);
                pct      = v.findViewById(R.id.tvMoodPct);
                progress = v.findViewById(R.id.progressMood);
            }
        }
    }
}