package com.spendlens.app.utils;

import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.Expense;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdaptiveStatementCategorizerTest {

    @Test
    public void categorize_usesLearnedMerchantHistory() {
        Category food = buildCategory(21, "Dining", "ic_food");
        Category other = buildCategory(42, "Other Stuff", "ic_others");
        List<Category> categories = Arrays.asList(food, other);

        Expense coffee = new Expense(
                "Starbucks Queen Street",
                6.75,
                21,
                1714478400000L,
                "",
                Expense.MOOD_NEED,
                Expense.PAY_CARD
        );

        ImportCategorySuggestion suggestion = AdaptiveStatementCategorizer.categorize(
                "STARBUCKS TORONTO ON",
                Collections.singletonList(coffee),
                categories
        );

        assertEquals(21, suggestion.categoryId);
        assertEquals("Dining", suggestion.categoryName);
        assertEquals("Learned", suggestion.sourceLabel);
        assertTrue(suggestion.autoCategorized);
    }

    @Test
    public void categorize_resolvesRuleMatchesAgainstActualCategoryIds() {
        Category food = buildCategory(41, "Meals", "ic_food");
        Category others = buildCategory(99, "Unsorted", "ic_others");

        ImportCategorySuggestion suggestion = AdaptiveStatementCategorizer.categorize(
                "Tim Hortons #1024",
                Collections.emptyList(),
                Arrays.asList(food, others)
        );

        assertEquals(41, suggestion.categoryId);
        assertEquals("Meals", suggestion.categoryName);
        assertEquals("Rules", suggestion.sourceLabel);
        assertTrue(suggestion.autoCategorized);
    }

    @Test
    public void categorize_fallsBackToReviewWhenNothingMatches() {
        Category others = buildCategory(77, "Needs Review", "ic_others");

        ImportCategorySuggestion suggestion = AdaptiveStatementCategorizer.categorize(
                "RANDOM VENDOR 12345",
                Collections.emptyList(),
                Collections.singletonList(others)
        );

        assertEquals(77, suggestion.categoryId);
        assertEquals("Needs Review", suggestion.categoryName);
        assertEquals("Review", suggestion.sourceLabel);
        assertEquals("Low confidence", suggestion.confidenceLabel);
        assertFalse(suggestion.autoCategorized);
    }

    private Category buildCategory(int id, String name, String iconName) {
        Category category = new Category(name, iconName, 0.0, true);
        category.categoryId = id;
        return category;
    }
}
