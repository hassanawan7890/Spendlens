package com.spendlens.app.insights;

import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.repository.ExpenseRepository;
import com.spendlens.app.utils.CurrencyUtils;

import java.util.List;

/**
 * Detects categories with repeated small purchases — the "latte factor".
 * Threshold: 5+ transactions in the current week for the same category.
 */
public class LeakDetector {

    private static final int MIN_COUNT = 5;

    public static String detect(ExpenseRepository repo, String currency) {
        List<CategorySummary> leaks = repo.getLeakCandidatesThisWeek(MIN_COUNT);

        if (leaks == null || leaks.isEmpty()) {
            return "No spending leaks found this week. Great job staying on track!";
        }

        StringBuilder sb = new StringBuilder();
        for (CategorySummary leak : leaks) {
            sb.append("• You spent on ")
                    .append(leak.categoryName)
                    .append(" ")
                    .append(leak.count)
                    .append(" times this week — ")
                    .append(CurrencyUtils.formatSmart(currency, leak.totalAmount))
                    .append(" total.\n");
        }

        return sb.toString().trim();
    }
}