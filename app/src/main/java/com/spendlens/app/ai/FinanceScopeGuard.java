package com.spendlens.app.ai;

import java.util.Locale;

public final class FinanceScopeGuard {

    private static final String[] FINANCE_TERMS = {
            "budget", "spend", "spent", "expense", "expenses", "saving", "save", "savings",
            "category", "categories", "statement", "bank", "transaction", "transactions",
            "money", "cash", "income", "refund", "debt", "bill", "bills", "subscription",
            "subscriptions", "monthly", "weekly", "daily", "cut back", "overspend", "overspending",
            "financial", "finance", "afford", "remaining", "left", "cost", "costs", "purchase",
            "purchases", "condition", "situation", "status", "balance", "payment", "payments",
            "price", "cheap", "expensive", "fund", "funds", "wallet", "account", "limit"
    };

    private FinanceScopeGuard() {
    }

    public static boolean isFinanceQuestion(String question) {
        if (question == null) return false;

        String normalized = question
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) return false;

        for (String keyword : FINANCE_TERMS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return normalized.contains("this month")
                || normalized.contains("this week")
                || normalized.contains("today")
                || normalized.contains("pay less");
    }

    public static String getRefusalMessage() {
        return "I stay focused on budgeting, spending, statement imports, and savings inside SpendLens.";
    }
}
