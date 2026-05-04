package com.spendlens.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendlens.app.R;
import com.spendlens.app.entities.MonthlySnapshot;
import com.spendlens.app.utils.CurrencyUtils;

public class MonthCardAdapter extends ListAdapter<MonthlySnapshot, MonthCardAdapter.ViewHolder> {

    public interface OnMonthClickListener {
        void onMonthClick(MonthlySnapshot snapshot);
    }

    private final OnMonthClickListener listener;
    private String currency = "RM";

    public MonthCardAdapter(OnMonthClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setCurrency(String currency) {
        this.currency = currency != null ? currency : "RM";
    }

    private static final DiffUtil.ItemCallback<MonthlySnapshot> DIFF =
            new DiffUtil.ItemCallback<MonthlySnapshot>() {
                @Override public boolean areItemsTheSame(@NonNull MonthlySnapshot a, @NonNull MonthlySnapshot b) {
                    return a.id == b.id;
                }
                @Override public boolean areContentsTheSame(@NonNull MonthlySnapshot a, @NonNull MonthlySnapshot b) {
                    return a.totalSpent == b.totalSpent && a.plannedBudget == b.plannedBudget;
                }
            };

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        MonthlySnapshot s = getItem(position);

        h.tvMonthYear.setText(s.getDisplayLabel());
        h.tvSpent.setText(CurrencyUtils.formatSmart(currency, s.totalSpent));
        h.tvBudget.setText("of " + CurrencyUtils.formatSmart(currency, s.plannedBudget));

        // Progress bar — clamped at 100%
        int pct = s.plannedBudget > 0
                ? (int) Math.min(100, (s.totalSpent / s.plannedBudget) * 100) : 0;
        h.progressBar.setProgress(pct);
        h.tvPercent.setText(pct + "% used");

        // Savings / overspend indicator
        double savings = s.savings;
        if (savings >= 0) {
            h.tvSavings.setText("Saved " + CurrencyUtils.formatSmart(currency, savings));
            h.tvSavings.setTextColor(h.itemView.getContext().getColor(R.color.green_600));
            h.tvStatus.setText("Under budget");
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.green_600));
            h.tvStatus.setBackgroundResource(R.color.mood_need_bg);
        } else {
            h.tvSavings.setText("Over " + CurrencyUtils.formatSmart(currency, Math.abs(savings)));
            h.tvSavings.setTextColor(h.itemView.getContext().getColor(R.color.red_600));
            h.tvStatus.setText("Over budget");
            h.tvStatus.setTextColor(h.itemView.getContext().getColor(R.color.red_700));
            h.tvStatus.setBackgroundResource(R.color.mood_emergency_bg);
        }

        h.itemView.setOnClickListener(v -> listener.onMonthClick(s));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthYear, tvSpent, tvBudget, tvPercent, tvSavings, tvStatus;
        ProgressBar progressBar;

        ViewHolder(View v) {
            super(v);
            tvMonthYear  = v.findViewById(R.id.tvMonthYear);
            tvSpent      = v.findViewById(R.id.tvSpent);
            tvBudget     = v.findViewById(R.id.tvBudget);
            tvPercent    = v.findViewById(R.id.tvPercent);
            tvSavings    = v.findViewById(R.id.tvSavings);
            tvStatus     = v.findViewById(R.id.tvStatus);
            progressBar  = v.findViewById(R.id.progressMonth);
        }
    }
}
