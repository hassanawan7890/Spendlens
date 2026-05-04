package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.CategorySnapshot;
import com.spendlens.app.entities.MonthlySnapshot;
import com.spendlens.app.insights.OverspendDetector;
import com.spendlens.app.insights.TrendAnalyzer;
import com.spendlens.app.repository.SnapshotRepository;
import com.spendlens.app.utils.CategoryDisplayUtils;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.viewmodels.SnapshotViewModel;
import com.spendlens.app.viewmodels.UserProfileViewModel;

import java.util.List;

public class AnalysisActivity extends AppCompatActivity {

    private SnapshotViewModel snapshotVm;
    private UserProfileViewModel profileVm;
    private String currency = "RM";

    // Section containers
    private LinearLayout layoutEmpty, layoutContent;
    private TextView tvEmptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        snapshotVm = new ViewModelProvider(this).get(SnapshotViewModel.class);
        profileVm  = new ViewModelProvider(this).get(UserProfileViewModel.class);

        // Bind views
        layoutEmpty    = findViewById(R.id.layoutEmpty);
        layoutContent  = findViewById(R.id.layoutContent);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bottom nav wiring
        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_analysis);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, HomeDashboardActivity.class)); finish(); return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, ExpenseHistoryActivity.class)); finish(); return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class)); finish(); return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class)); finish(); return true;
            }
            return true;
        });

        profileVm.getProfile().observe(this, profile -> {
            if (profile != null) currency = profile.currency;
        });

        // Observe snapshots and run analysis on background thread
        snapshotVm.getAllSnapshots().observe(this, snapshots -> {
            if (snapshots == null || snapshots.isEmpty()) {
                showEmpty("No budget history yet.\n\nGo to Settings \u2192 Budget History\nand tap + to save this month first.");
                return;
            }
            if (snapshots.size() == 1) {
                runAnalysis(snapshots, true);
            } else {
                runAnalysis(snapshots, false);
            }
        });
    }

    private void runAnalysis(List<MonthlySnapshot> snapshots, boolean limitedData) {
        AppDatabase.dbExecutor.execute(() -> {
            SnapshotRepository repo = new SnapshotRepository(getApplication());

            List<CategorySnapshot> latestCats =
                    repo.getCategoriesForSnapshot(snapshots.get(0).id);

            List<CategorySnapshot> previousCats = snapshots.size() >= 2
                    ? repo.getCategoriesForSnapshot(snapshots.get(1).id)
                    : null;

            List<CategorySnapshot> allPreviousCats =
                    OverspendDetector.collectPreviousCats(snapshots, repo);

            TrendAnalyzer.TrendResult trend = TrendAnalyzer.analyze(snapshots);
            List<TrendAnalyzer.CategoryComparison> catComparisons =
                    TrendAnalyzer.compareCategoriesMonthOverMonth(snapshots, latestCats, previousCats);
            List<OverspendDetector.OverspendFlag> flags =
                    OverspendDetector.detect(latestCats, allPreviousCats);

            runOnUiThread(() -> bindAnalysis(snapshots, trend, catComparisons, flags, limitedData));
        });
    }

    private void bindAnalysis(
            List<MonthlySnapshot> snapshots,
            TrendAnalyzer.TrendResult trend,
            List<TrendAnalyzer.CategoryComparison> catComparisons,
            List<OverspendDetector.OverspendFlag> flags,
            boolean limitedData) {

        layoutEmpty.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        if (limitedData) {
            TextView tvNote = findViewById(R.id.tvLimitedDataNote);
            tvNote.setVisibility(View.VISIBLE);
        }

        // Average monthly spending
        if (trend != null) {
            TextView tvAvg = findViewById(R.id.tvAverageAmount);
            TextView tvMonthsCount = findViewById(R.id.tvMonthsCount);
            tvAvg.setText(CurrencyUtils.format(currency, trend.averageMonthlySpending));
            tvMonthsCount.setText("Based on " + trend.monthsAnalyzed + " month"
                    + (trend.monthsAnalyzed > 1 ? "s" : ""));

            TextView tvChange = findViewById(R.id.tvSpendingChange);
            if (snapshots.size() >= 2) {
                double pct = trend.spendingChangePercent;
                String label = TrendAnalyzer.formatChangePercent(pct) + " vs last month";
                tvChange.setText(label);
                tvChange.setTextColor(pct <= 0
                        ? getColor(R.color.success)
                        : getColor(R.color.danger));
            } else {
                tvChange.setVisibility(View.GONE);
            }
        }

        // Trend bar chart
        LinearLayout chartContainer = findViewById(R.id.chartContainer);
        chartContainer.removeAllViews();

        double maxSpent = 0;
        for (MonthlySnapshot s : snapshots) maxSpent = Math.max(maxSpent, s.totalSpent);
        if (maxSpent <= 0) maxSpent = 1;

        // Show up to 6 months oldest to newest
        chartContainer.removeAllViews();
        for (int i = snapshots.size() - 1; i >= Math.max(0, snapshots.size() - 6); i--) {
            addBarToChart(chartContainer, snapshots.get(i), maxSpent);
        }

        // Overspend flags
        LinearLayout overspendContainer = findViewById(R.id.overspendContainer);
        overspendContainer.removeAllViews();
        TextView tvNoFlags = findViewById(R.id.tvNoOverspendFlags);

        if (flags.isEmpty()) {
            tvNoFlags.setVisibility(View.VISIBLE);
            tvNoFlags.setText(snapshots.size() < 2
                    ? "Need at least 2 months of history to detect overspending."
                    : "No overspending detected. All categories within normal range.");
        } else {
            tvNoFlags.setVisibility(View.GONE);
            for (OverspendDetector.OverspendFlag flag : flags) {
                addOverspendRow(overspendContainer, flag);
            }
        }

        // Category comparison
        LinearLayout catContainer = findViewById(R.id.categoryComparisonContainer);
        catContainer.removeAllViews();
        TextView tvNoCats = findViewById(R.id.tvNoCategoryComparison);

        if (catComparisons.isEmpty() || snapshots.size() < 2) {
            tvNoCats.setVisibility(View.VISIBLE);
            tvNoCats.setText("Need at least 2 months of history\nfor category comparison.");
        } else {
            tvNoCats.setVisibility(View.GONE);
            int limit = Math.min(5, catComparisons.size());
            for (int i = 0; i < limit; i++) {
                addCategoryComparisonRow(catContainer, catComparisons.get(i));
            }
        }
    }

    private void addBarToChart(LinearLayout container, MonthlySnapshot s, double maxSpent) {
        LinearLayout barWrapper = new LinearLayout(this);
        barWrapper.setOrientation(LinearLayout.VERTICAL);
        barWrapper.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        wrapperParams.setMargins(4, 0, 4, 0);
        barWrapper.setLayoutParams(wrapperParams);

        // Bar fill
        View bar = new View(this);
        int barHeightPx = (int) ((s.totalSpent / maxSpent) * dpToPx(120));
        barHeightPx = Math.max(barHeightPx, dpToPx(4));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, barHeightPx);
        bar.setLayoutParams(barParams);

        // FIX: use success (green #1DB954) and danger (red #FF453A)
        // not green_600 which was aliased to gold in the Midnight Gold theme
        bar.setBackgroundColor(s.isUnderBudget()
                ? getColor(R.color.success)
                : getColor(R.color.danger));

        // Month label
        TextView label = new TextView(this);
        label.setText(getShortMonth(s.month));
        label.setTextSize(10);
        label.setTextColor(getColor(R.color.gray_500));
        label.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dpToPx(4);
        label.setLayoutParams(labelParams);

        // Amount label above bar
        TextView amountLabel = new TextView(this);
        amountLabel.setText(CurrencyUtils.formatSmart(currency, s.totalSpent));
        amountLabel.setTextSize(9);
        amountLabel.setTextColor(getColor(R.color.gray_500));
        amountLabel.setGravity(android.view.Gravity.CENTER);

        barWrapper.addView(amountLabel);
        barWrapper.addView(bar);
        barWrapper.addView(label);
        container.addView(barWrapper);
    }

    private void addOverspendRow(LinearLayout container, OverspendDetector.OverspendFlag flag) {
        View row = getLayoutInflater().inflate(R.layout.item_overspend_flag, container, false);

        TextView tvCat      = row.findViewById(R.id.tvFlagCategory);
        TextView tvSeverity = row.findViewById(R.id.tvFlagSeverity);
        TextView tvAmount   = row.findViewById(R.id.tvFlagAmount);
        TextView tvAvg      = row.findViewById(R.id.tvFlagAverage);
        ProgressBar progress = row.findViewById(R.id.progressFlag);

        tvCat.setText(CategoryDisplayUtils.getEmojiForIconName(flag.iconName)
                + "  " + flag.categoryName);
        tvSeverity.setText(flag.severity);
        tvSeverity.setTextColor(flag.severity.equals("High")
                ? getColor(R.color.danger) : getColor(R.color.warning));
        tvSeverity.setBackgroundColor(flag.severity.equals("High")
                ? getColor(R.color.danger_dim) : getColor(R.color.warning_dim));

        tvAmount.setText(CurrencyUtils.formatSmart(currency, flag.latestAmount)
                + "  (" + TrendAnalyzer.formatChangePercent(flag.percentAboveAverage) + ")");
        tvAmount.setTextColor(getColor(R.color.danger));
        tvAvg.setText("avg " + CurrencyUtils.formatSmart(currency, flag.averageAmount));

        int pct = (int) Math.min(100, (flag.latestAmount / (flag.averageAmount * 2)) * 100);
        progress.setProgress(pct);

        container.addView(row);
    }

    private void addCategoryComparisonRow(LinearLayout container,
                                          TrendAnalyzer.CategoryComparison comp) {
        View row = getLayoutInflater().inflate(R.layout.item_category_comparison, container, false);

        TextView tvCat    = row.findViewById(R.id.tvCompCat);
        TextView tvLatest = row.findViewById(R.id.tvCompLatest);
        TextView tvChange = row.findViewById(R.id.tvCompChange);
        TextView tvPrev   = row.findViewById(R.id.tvCompPrev);

        tvCat.setText(CategoryDisplayUtils.getEmojiForIconName(comp.iconName)
                + "  " + comp.categoryName);
        tvLatest.setText(CurrencyUtils.formatSmart(currency, comp.latestMonthAmount));

        if (comp.previousMonthAmount > 0) {
            tvChange.setText(TrendAnalyzer.formatChangePercent(comp.changePercent));
            tvChange.setTextColor(comp.isIncrease
                    ? getColor(R.color.danger) : getColor(R.color.success));
            tvPrev.setText("was " + CurrencyUtils.formatSmart(currency, comp.previousMonthAmount));
        } else {
            tvChange.setText("New");
            tvChange.setTextColor(getColor(R.color.info));
            tvPrev.setText("no previous data");
        }

        container.addView(row);
    }

    private void showEmpty(String message) {
        layoutContent.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(message);
    }

    private String getShortMonth(int month) {
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"};
        return months[Math.max(0, month - 1)];
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
