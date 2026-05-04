package com.spendlens.app.insights;

import com.spendlens.app.entities.Expense;
import com.spendlens.app.models.MoodSummary;
import com.spendlens.app.repository.ExpenseRepository;

import java.util.List;

/**
 * Classifies the user into a spending personality based on mood tag distribution.
 * Returns [name, description] as a String[2].
 */
public class PersonalityEngine {

    public static String[] classify(ExpenseRepository repo) {
        List<MoodSummary> moods = repo.getMoodSummaryThisMonthSync();

        if (moods == null || moods.isEmpty()) {
            return new String[]{"Not enough data", "Add more expenses to reveal your spending personality."};
        }

        int total = moods.stream().mapToInt(m -> m.count).sum();
        if (total == 0) return new String[]{"Not enough data", "Add more expenses to unlock insights."};

        int impulseCount = 0, subCount = 0, socialCount = 0, needCount = 0;
        double subAmount = 0, totalAmount = 0;

        for (MoodSummary m : moods) {
            totalAmount += m.totalAmount;
            switch (m.moodTag) {
                case Expense.MOOD_IMPULSE:      impulseCount = m.count; break;
                case Expense.MOOD_SUBSCRIPTION: subCount = m.count; subAmount = m.totalAmount; break;
                case Expense.MOOD_SOCIAL:       socialCount = m.count; break;
                case Expense.MOOD_NEED:         needCount = m.count; break;
            }
        }

        double impulsePct = (double) impulseCount / total;
        double subAmtPct  = totalAmount > 0 ? subAmount / totalAmount : 0;
        double socialPct  = (double) socialCount / total;
        double needPct    = (double) needCount / total;

        if (impulsePct > 0.40) {
            return new String[]{"Impulse Spender",
                "Over 40% of your purchases were unplanned. Try a 24-hour rule before non-essential buys."};
        }
        if (subAmtPct > 0.25) {
            return new String[]{"Subscription Heavy",
                "Subscriptions make up over 25% of your spending. Review which ones you actively use."};
        }
        if (socialPct > 0.35) {
            return new String[]{"Social Spender",
                "Most of your spending happens in social contexts. Consider free alternatives occasionally."};
        }
        if (needPct > 0.70) {
            return new String[]{"Essentials-Only",
                "Almost all your spending is on essentials. Very disciplined — make sure you have some balance."};
        }
        if (needPct > 0.55 && totalAmount > 0) {
            return new String[]{"Careful Planner",
                "You prioritise essentials and keep discretionary spending in check. Well balanced."};
        }
        return new String[]{"Balanced Spender",
            "Your spending is spread across categories without a strong pattern. Keep monitoring monthly."};
    }
}