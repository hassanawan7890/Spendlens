package com.spendlens.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendlens.app.R;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;

import java.util.Objects;

public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ViewHolder> {
    public interface OnExpenseClickListener { void onExpenseClick(Expense expense); }

    private final OnExpenseClickListener listener;
    private String currencySymbol = "CAD";

    public ExpenseAdapter(OnExpenseClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setCurrencySymbol(String symbol) {
        this.currencySymbol = symbol != null ? symbol : "CAD";
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense a, @NonNull Expense b) {
                    return a.expenseId == b.expenseId;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Expense a, @NonNull Expense b) {
                    return a.amount == b.amount
                            && a.categoryId == b.categoryId
                            && a.date == b.date
                            && Objects.equals(a.title, b.title)
                            && Objects.equals(a.moodTag, b.moodTag)
                            && Objects.equals(a.note, b.note)
                            && Objects.equals(a.paymentMethod, b.paymentMethod);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = getItem(position);
        holder.title.setText(expense.title);
        holder.date.setText(DateUtils.isToday(expense.date)
                ? "Today"
                : DateUtils.formatDate(expense.date));
        holder.amount.setText("-" + CurrencyUtils.formatSmart(currencySymbol, expense.amount));
        holder.icon.setText(getIconForCategoryId(expense.categoryId));
        applyIconBackground(holder.icon, expense.categoryId);

        String mood = expense.moodTag != null ? expense.moodTag : "";
        holder.mood.setText(mood);
        applyMoodColors(holder.mood, mood);
        holder.itemView.setOnClickListener(v -> listener.onExpenseClick(expense));
    }

    public Expense getExpenseAt(int position) {
        return getItem(position);
    }

    private String getIconForCategoryId(int id) {
        switch (id) {
            case 1: return "\uD83C\uDF54";
            case 2: return "\uD83D\uDE8C";
            case 3: return "\uD83C\uDFAE";
            case 4: return "\uD83D\uDD04";
            case 5: return "\uD83D\uDC8A";
            case 6: return "\uD83D\uDCDA";
            case 7: return "\uD83D\uDED2";
            default: return "\uD83D\uDCE6";
        }
    }

    private void applyIconBackground(TextView tv, int categoryId) {
        android.content.Context ctx = tv.getContext();
        int bg;
        switch (categoryId) {
            case 1: bg = ctx.getColor(R.color.mood_impulse_bg);      break;
            case 2: bg = ctx.getColor(R.color.mood_want_bg);         break;
            case 3: bg = ctx.getColor(R.color.mood_social_bg);       break;
            case 4: bg = ctx.getColor(R.color.mood_subscription_bg); break;
            case 5: bg = ctx.getColor(R.color.mood_need_bg);         break;
            case 6: bg = ctx.getColor(R.color.gold_dim);             break;
            case 7: bg = ctx.getColor(R.color.mood_social_bg);       break;
            default: bg = ctx.getColor(R.color.dark_surface_2);      break;
        }
        tv.setBackgroundColor(bg);
    }

    private void applyMoodColors(TextView tv, String mood) {
        android.content.Context ctx = tv.getContext();
        int bg;
        int text;
        switch (mood) {
            case Expense.MOOD_NEED:
                bg = ctx.getColor(R.color.mood_need_bg);
                text = ctx.getColor(R.color.mood_need_text);
                break;
            case Expense.MOOD_WANT:
                bg = ctx.getColor(R.color.mood_want_bg);
                text = ctx.getColor(R.color.mood_want_text);
                break;
            case Expense.MOOD_IMPULSE:
                bg = ctx.getColor(R.color.mood_impulse_bg);
                text = ctx.getColor(R.color.mood_impulse_text);
                break;
            case Expense.MOOD_SOCIAL:
                bg = ctx.getColor(R.color.mood_social_bg);
                text = ctx.getColor(R.color.mood_social_text);
                break;
            case Expense.MOOD_SUBSCRIPTION:
                bg = ctx.getColor(R.color.mood_subscription_bg);
                text = ctx.getColor(R.color.mood_subscription_text);
                break;
            case Expense.MOOD_EMERGENCY:
                bg = ctx.getColor(R.color.mood_emergency_bg);
                text = ctx.getColor(R.color.mood_emergency_text);
                break;
            default:
                bg = ctx.getColor(R.color.dark_surface_2);
                text = ctx.getColor(R.color.text_tertiary);
                break;
        }
        tv.setBackgroundColor(bg);
        tv.setTextColor(text);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView icon, title, date, amount, mood;

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.tvExpenseIcon);
            title = view.findViewById(R.id.tvExpenseTitle);
            date = view.findViewById(R.id.tvExpenseDate);
            amount = view.findViewById(R.id.tvExpenseAmount);
            mood = view.findViewById(R.id.tvExpenseMood);
        }
    }
}
