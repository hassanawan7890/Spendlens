package com.spendlens.app.insights;

import com.spendlens.app.entities.Expense;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.models.DaySummary;
import com.spendlens.app.models.MoodSummary;
import com.spendlens.app.repository.ExpenseRepository;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;

import java.util.List;

/**
 * Generates a plain-English weekly reflection summary.
 */
public class ReflectionEngine {

    public static String generate(ExpenseRepository repo, String currency) {
        List<MoodSummary> moods = repo.getMoodSummaryThisMonthSync();
        List<DaySummary> dailySums = repo.getDailySummaryThisWeek();

        if ((moods == null || moods.isEmpty()) && (dailySums == null || dailySums.isEmpty())) {
            return "No spending recorded yet this week. Start adding expenses to see your reflection.";
        }

        StringBuilder sb = new StringBuilder();

        // Top category
        List<CategorySummary> catSummary = repo.getLeakCandidatesThisWeek(0);
        if (catSummary != null && !catSummary.isEmpty()) {
            CategorySummary top = catSummary.get(0);
            sb.append("Top category: ")
                    .append(top.categoryName)
                    .append(" (")
                    .append(CurrencyUtils.formatSmart(currency, top.totalAmount))
                    .append(").\n");
        }

        // Top mood
        if (moods != null && !moods.isEmpty()) {
            MoodSummary topMood = moods.get(0);
            sb.append("Most tagged mood: ")
                    .append(topMood.moodTag)
                    .append(" (")
                    .append(topMood.count)
                    .append("x).\n");
        }

        // Highest spending day
        if (dailySums != null && !dailySums.isEmpty()) {
            DaySummary peak = dailySums.get(0);
            String dayName = DateUtils.formatDayName(peak.getDayStartTimestamp());
            sb.append("Highest day: ")
                    .append(dayName)
                    .append(" (")
                    .append(CurrencyUtils.formatSmart(currency, peak.totalAmount))
                    .append(").\n");
        }

        // Suggestion based on top mood
        if (moods != null && !moods.isEmpty()) {
            sb.append("\n")
                    .append(getSuggestion(moods.get(0).moodTag));
        }

        return sb.toString().trim();
    }

    private static String getSuggestion(String topMood) {
        switch (topMood) {
            case Expense.MOOD_IMPULSE:
                return "💡 Tip: Try a 24-hour waiting rule before any unplanned purchase next week.";
            case Expense.MOOD_SOCIAL:
                return "💡 Tip: Suggest free or low-cost social activities — parks, home gatherings, free events.";
            case Expense.MOOD_SUBSCRIPTION:
                return "💡 Tip: Review your subscriptions — cancel anything you haven't used this month.";
            case Expense.MOOD_WANT:
                return "💡 Tip: Separate wants from needs. Give yourself a weekly discretionary allowance.";
            default:
                return "💡 Tip: Set a daily spending cap to maintain your monthly budget comfortably.";
        }
    }
}