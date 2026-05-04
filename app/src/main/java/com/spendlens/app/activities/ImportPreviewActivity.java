package com.spendlens.app.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.ImportedTransaction;
import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.notifications.BudgetNotificationManager;
import com.spendlens.app.repository.StatementRepository;
import com.spendlens.app.utils.CategoryDisplayUtils;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.widget.WidgetRefreshHelper;

import java.util.ArrayList;
import java.util.List;

public class ImportPreviewActivity extends AppCompatActivity {

    private StatementRepository repo;
    private PreviewAdapter adapter;
    private BudgetNotificationManager notifManager;
    private String currency;
    private int statementId;
    private List<ImportedTransaction> transactions = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private TextView tvSummary;
    private TextView tvMeta;
    private TextView tvSelectionSummary;
    private Button btnConfirmImport;
    private ProgressBar previewLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_preview);

        repo = new StatementRepository(getApplication());
        notifManager = new BudgetNotificationManager(this);
        currency = PrefsManager.getInstance(this).getCurrency();
        statementId = getIntent().getIntExtra("statement_id", -1);
        String fileName = getIntent().getStringExtra("file_name");

        if (statementId < 0) {
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvPreviewTitle);
        tvSummary = findViewById(R.id.tvPreviewSummary);
        tvMeta = findViewById(R.id.tvPreviewMeta);
        tvSelectionSummary = findViewById(R.id.tvSelectionSummary);
        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);
        btnConfirmImport = findViewById(R.id.btnConfirmImport);
        previewLoading = findViewById(R.id.previewLoading);

        tvTitle.setText(fileName != null ? fileName : "Statement Preview");
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        btnConfirmImport.setEnabled(false);

        findViewById(R.id.btnBack).setOnClickListener(v -> confirmDiscard());
        btnConfirmImport.setOnClickListener(v -> confirmImport());

        AppDatabase.dbExecutor.execute(() -> {
            transactions = repo.getTransactionsForStatement(statementId);
            categories = AppDatabase.getInstance(this).categoryDao().getAllCategoriesSync();

            runOnUiThread(() -> {
                previewLoading.setVisibility(View.GONE);
                adapter = new PreviewAdapter(transactions);
                rvTransactions.setAdapter(adapter);
                updateSummary();
                btnConfirmImport.setEnabled(!transactions.isEmpty());
            });
        });
    }

    private void updateSummary() {
        long debits = 0;
        long credits = 0;
        long selected = 0;
        long learned = 0;
        long aiAssisted = 0;
        long review = 0;
        double totalToAdd = 0;

        for (ImportedTransaction tx : transactions) {
            if ("debit".equals(tx.type)) {
                debits++;
                if (tx.addToExpenses) {
                    selected++;
                    totalToAdd += tx.amount;
                }
            } else if ("credit".equals(tx.type)) {
                credits++;
            }

            if ("Learned".equals(tx.suggestionSource)) {
                learned++;
            } else if ("AI".equals(tx.suggestionSource)) {
                aiAssisted++;
            } else if (!"Rules".equals(tx.suggestionSource) && !"Manual".equals(tx.suggestionSource)) {
                review++;
            }
        }

        tvSummary.setText(transactions.size() + " transactions  |  "
                + debits + " spend rows  |  " + credits + " income rows");
        tvMeta.setText(selected + " selected  |  "
                + CurrencyUtils.formatSmart(currency, totalToAdd)
                + " to add  |  " + learned + " learned  |  "
                + aiAssisted + " AI-assisted  |  " + review + " need review");
        tvSelectionSummary.setText(selected > 0
                ? selected + " expense rows ready to import"
                : "Select at least one debit transaction");
        btnConfirmImport.setText(selected > 0
                ? "Add " + selected + " Selected Expenses"
                : "Select Transactions to Import");
    }

    private void confirmImport() {
        if (transactions.isEmpty()) {
            Toast.makeText(this, "Transactions are still loading.", Toast.LENGTH_SHORT).show();
            return;
        }

        long toAdd = 0;
        for (ImportedTransaction tx : transactions) {
            if ("debit".equals(tx.type) && tx.addToExpenses) toAdd++;
        }

        if (toAdd == 0) {
            Toast.makeText(this, "No transactions selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Add " + toAdd + " expenses?")
                .setMessage("These will be added to your expense history. You can edit or delete them afterwards.")
                .setPositiveButton("Add to Expenses", (d, w) -> runConfirm())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runConfirm() {
        btnConfirmImport.setEnabled(false);

        AppDatabase.dbExecutor.execute(() -> {
            for (ImportedTransaction tx : transactions) {
                if (!"debit".equals(tx.type)) {
                    tx.addToExpenses = false;
                }
                repo.updateTransaction(tx);
            }

            int added = repo.confirmImport(statementId);
            AppDatabase db = AppDatabase.getInstance(this);
            UserProfile profile = db.userProfileDao().getProfileSync();
            long[] monthRange = DateUtils.getCurrentMonthRange();
            double spent = db.expenseDao().getTotalBetweenSync(monthRange[0], monthRange[1]);
            double budget = profile != null ? profile.monthlyBudget : 0.0;
            String activeCurrency = profile != null && profile.currency != null
                    ? profile.currency
                    : currency;

            WidgetRefreshHelper.refresh(this);

            runOnUiThread(() -> {
                notifManager.checkAndNotify(spent, budget, activeCurrency);
                Toast.makeText(this, added + " expenses added!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private void confirmDiscard() {
        new AlertDialog.Builder(this)
                .setTitle("Discard import?")
                .setMessage("Parsed transactions will be deleted. Your expenses are unchanged.")
                .setPositiveButton("Discard", (d, w) -> AppDatabase.dbExecutor.execute(() -> {
                    repo.deleteStatement(statementId);
                    runOnUiThread(this::finish);
                }))
                .setNegativeButton("Keep reviewing", null)
                .show();
    }

    class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.VH> {
        private final List<ImportedTransaction> items;

        PreviewAdapter(List<ImportedTransaction> items) {
            this.items = items;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_import_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ImportedTransaction tx = items.get(position);
            boolean isDebit = "debit".equals(tx.type);

            holder.tvDesc.setText(tx.description != null ? tx.description : "Untitled transaction");
            holder.tvDate.setText(DateUtils.formatDate(tx.date));
            holder.tvAmount.setText((isDebit ? "-" : "+")
                    + CurrencyUtils.formatSmart(currency, tx.amount));
            holder.tvAmount.setTextColor(isDebit
                    ? getColor(R.color.red_600)
                    : getColor(R.color.blue_600));

            holder.cbAdd.setOnCheckedChangeListener(null);
            if (isDebit) {
                holder.cbAdd.setEnabled(true);
                holder.cbAdd.setAlpha(1f);
                holder.cbAdd.setChecked(tx.addToExpenses);
                holder.cbAdd.setOnCheckedChangeListener((buttonView, checked) -> {
                    tx.addToExpenses = checked;
                    updateSummary();
                });
                bindDebitSuggestion(holder, tx);
                holder.tvCategory.setOnClickListener(v ->
                        showCategoryPicker(tx, holder.getBindingAdapterPosition()));
            } else {
                tx.addToExpenses = false;
                holder.cbAdd.setChecked(false);
                holder.cbAdd.setEnabled(false);
                holder.cbAdd.setAlpha(0.45f);
                holder.tvCategory.setText("Income row");
                holder.tvCategory.setOnClickListener(null);
                stylePill(holder.tvCategory, R.color.blue_50, R.color.blue_800);
                holder.tvSource.setText("Credit");
                stylePill(holder.tvSource, R.color.blue_50, R.color.blue_800);
                holder.tvConfidence.setText("Not imported");
                stylePill(holder.tvConfidence, R.color.gray_50, R.color.gray_700);
                holder.tvReason.setText("Credit rows stay visible for statement context and are excluded from expenses.");
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDesc;
            TextView tvDate;
            TextView tvAmount;
            TextView tvCategory;
            TextView tvSource;
            TextView tvConfidence;
            TextView tvReason;
            CheckBox cbAdd;

            VH(View view) {
                super(view);
                tvDesc = view.findViewById(R.id.tvImportDesc);
                tvDate = view.findViewById(R.id.tvImportDate);
                tvAmount = view.findViewById(R.id.tvImportAmount);
                tvCategory = view.findViewById(R.id.tvImportCategory);
                tvSource = view.findViewById(R.id.tvImportSource);
                tvConfidence = view.findViewById(R.id.tvImportConfidence);
                tvReason = view.findViewById(R.id.tvImportReason);
                cbAdd = view.findViewById(R.id.cbAddToExpenses);
            }
        }
    }

    private void bindDebitSuggestion(PreviewAdapter.VH holder, ImportedTransaction tx) {
        String categoryName = tx.categoryName != null
                ? tx.categoryName
                : CategoryDisplayUtils.DEFAULT_CATEGORY_NAME;
        String iconName = resolveCategoryIconName(tx.categoryId);
        holder.tvCategory.setText(CategoryDisplayUtils.getEmojiForIconName(iconName) + " " + categoryName);
        stylePill(holder.tvCategory, R.color.green_50, R.color.green_700);

        String source = tx.suggestionSource != null ? tx.suggestionSource : "Review";
        holder.tvSource.setText(source);
        switch (source) {
            case "Learned":
                stylePill(holder.tvSource, R.color.teal_50, R.color.teal_700);
                break;
            case "AI":
                stylePill(holder.tvSource, R.color.purple_50, R.color.purple_600);
                break;
            case "Rules":
                stylePill(holder.tvSource, R.color.blue_50, R.color.blue_800);
                break;
            case "Manual":
                stylePill(holder.tvSource, R.color.green_50, R.color.green_700);
                break;
            default:
                stylePill(holder.tvSource, R.color.amber_50, R.color.amber_700);
                break;
        }

        String confidence = tx.confidenceLabel != null ? tx.confidenceLabel : "Low confidence";
        holder.tvConfidence.setText(confidence);
        String normalizedConfidence = confidence.toLowerCase();
        if (normalizedConfidence.contains("high")) {
            stylePill(holder.tvConfidence, R.color.teal_50, R.color.teal_700);
        } else if (normalizedConfidence.contains("medium")
                || normalizedConfidence.contains("picked")) {
            stylePill(holder.tvConfidence, R.color.green_50, R.color.green_700);
        } else if (normalizedConfidence.contains("low")) {
            stylePill(holder.tvConfidence, R.color.amber_50, R.color.amber_700);
        } else {
            stylePill(holder.tvConfidence, R.color.gray_50, R.color.gray_700);
        }

        String reason = tx.matchReason != null ? tx.matchReason : "";
        holder.tvReason.setText(reason.isEmpty()
                ? "Tap the category pill if you want to adjust this suggestion."
                : reason);
    }

    private void showCategoryPicker(ImportedTransaction tx, int position) {
        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            names[i] = CategoryDisplayUtils.getEmojiForIconName(category.iconName)
                    + " " + category.categoryName;
        }

        new AlertDialog.Builder(this)
                .setTitle("Change category")
                .setItems(names, (d, which) -> {
                    Category selected = categories.get(which);
                    tx.categoryId = selected.categoryId;
                    tx.categoryName = selected.categoryName;
                    tx.isAutoCategorized = false;
                    tx.suggestionSource = "Manual";
                    tx.confidenceLabel = "Picked by you";
                    tx.matchReason = "Category adjusted during review.";
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        adapter.notifyItemChanged(position);
                    }
                    updateSummary();
                })
                .show();
    }

    private String resolveCategoryIconName(int categoryId) {
        for (Category category : categories) {
            if (category.categoryId == categoryId && category.iconName != null) {
                return category.iconName;
            }
        }
        return CategoryDisplayUtils.DEFAULT_ICON_NAME;
    }

    private void stylePill(TextView view, int backgroundColorRes, int textColorRes) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(backgroundColorRes));
        background.setCornerRadius(dpToPx(999));
        view.setBackground(background);
        view.setTextColor(getColor(textColorRes));
    }

    private float dpToPx(int dp) {
        return getResources().getDisplayMetrics().density * dp;
    }
}
