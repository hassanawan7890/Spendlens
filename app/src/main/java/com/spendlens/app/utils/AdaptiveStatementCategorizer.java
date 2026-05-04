package com.spendlens.app.utils;

import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.Expense;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AdaptiveStatementCategorizer {

    private static final Set<String> NOISE_TOKENS = new HashSet<>();

    static {
        String[] tokens = {
                "pos", "purchase", "debit", "credit", "payment", "online", "transfer",
                "etransfer", "interac", "visa", "mc", "card", "transaction", "store",
                "ref", "canada", "cdn", "toronto", "on", "qc", "bc", "ab", "ns",
                "mb", "nb", "nl", "nsf", "preauth", "retail", "authorized", "auth"
        };
        for (String token : tokens) {
            NOISE_TOKENS.add(token);
        }
    }

    private AdaptiveStatementCategorizer() {}

    public static ImportCategorySuggestion categorize(String description,
                                                      List<Expense> expenseHistory,
                                                      List<Category> categories) {
        ImportCategorySuggestion learned = findLearnedMatch(description, expenseHistory, categories);
        if (learned != null) return learned;

        ImportCategorySuggestion keywordMatch = KeywordCategorizer.categorizeDetailed(description, categories);
        if (keywordMatch != null) return keywordMatch;

        return new ImportCategorySuggestion(
                KeywordCategorizer.resolveCategoryId(CategoryDisplayUtils.DEFAULT_CATEGORY_NAME, categories),
                KeywordCategorizer.resolveCategoryName(CategoryDisplayUtils.DEFAULT_CATEGORY_NAME, categories),
                "Review",
                "Low confidence",
                "No strong merchant memory or keyword match. Review before import.",
                false
        );
    }

    private static ImportCategorySuggestion findLearnedMatch(String description,
                                                             List<Expense> expenseHistory,
                                                             List<Category> categories) {
        if (description == null || description.trim().isEmpty() || expenseHistory == null) {
            return null;
        }

        String merchantKey = normalizeMerchant(description);
        if (merchantKey.isEmpty()) return null;

        Map<Integer, Integer> countsByCategory = new HashMap<>();
        String sampleTitle = null;
        int bestCategoryId = -1;
        int bestCount = 0;

        for (Expense expense : expenseHistory) {
            String existingKey = normalizeMerchant(expense.title);
            if (existingKey.isEmpty()) continue;

            boolean sameMerchant = merchantKey.equals(existingKey)
                    || merchantKey.contains(existingKey)
                    || existingKey.contains(merchantKey);
            if (!sameMerchant) continue;

            sampleTitle = expense.title;
            int newCount = countsByCategory.getOrDefault(expense.categoryId, 0) + 1;
            countsByCategory.put(expense.categoryId, newCount);
            if (newCount > bestCount) {
                bestCount = newCount;
                bestCategoryId = expense.categoryId;
            }
        }

        if (bestCategoryId < 0) return null;

        int bestCountMatches = 0;
        for (int count : countsByCategory.values()) {
            if (count == bestCount) {
                bestCountMatches++;
            }
        }
        if (bestCountMatches > 1) return null;

        String categoryName = resolveCategoryName(bestCategoryId, categories);
        String confidence = bestCount >= 2 ? "High confidence" : "Medium confidence";
        String reason = bestCount >= 2
                ? "Matched your past merchant pattern for " + safeSample(sampleTitle) + "."
                : "Matched a similar past merchant: " + safeSample(sampleTitle) + ".";

        return new ImportCategorySuggestion(
                bestCategoryId,
                categoryName,
                "Learned",
                confidence,
                reason,
                true
        );
    }

    private static String resolveCategoryName(int categoryId, List<Category> categories) {
        if (categories != null) {
            for (Category category : categories) {
                if (category.categoryId == categoryId && category.categoryName != null) {
                    return category.categoryName;
                }
            }
        }
        return KeywordCategorizer.getCategoryName(categoryId);
    }

    private static String safeSample(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "past entries";
        }
        return "\"" + title.trim() + "\"";
    }

    static String normalizeMerchant(String raw) {
        if (raw == null) return "";

        String cleaned = raw.toLowerCase(Locale.US)
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) return "";

        String[] parts = cleaned.split(" ");
        StringBuilder builder = new StringBuilder();
        int kept = 0;
        for (String part : parts) {
            if (part.length() < 2 || NOISE_TOKENS.contains(part)) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(part);
            kept++;
            if (kept >= 3) break;
        }
        return builder.toString().trim();
    }
}
