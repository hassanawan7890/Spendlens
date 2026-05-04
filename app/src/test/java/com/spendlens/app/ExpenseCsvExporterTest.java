package com.spendlens.app;

import com.spendlens.app.entities.Expense;
import com.spendlens.app.utils.ExpenseCsvExporter;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ExpenseCsvExporterTest {

    @Test
    public void buildCsv_includesHeaderAndFormattedExpense() {
        Expense expense = new Expense(
                "Lunch",
                14.5,
                1,
                1714478400000L,
                "Campus cafe",
                "Need",
                "Card"
        );

        Map<Integer, String> categories = new HashMap<>();
        categories.put(1, "Food & Drinks");

        String csv = ExpenseCsvExporter.buildCsv(
                Collections.singletonList(expense),
                categories,
                "CAD"
        );

        assertTrue(csv.startsWith(
                "title,amount,currency,category,date,mood,payment_method,note\n"));
        assertTrue(csv.contains("\"Lunch\",\"14.50\",\"CAD\",\"Food & Drinks\",\"2024-04-30\""));
        assertTrue(csv.contains("\"Need\",\"Card\",\"Campus cafe\""));
    }

    @Test
    public void buildCsv_escapesQuotesAndCommas() {
        Expense expense = new Expense(
                "Books, pens",
                22,
                8,
                1714564800000L,
                "Bought \"study\" supplies",
                "Want",
                "Cash"
        );

        String csv = ExpenseCsvExporter.buildCsv(
                Collections.singletonList(expense),
                Collections.emptyMap(),
                "$"
        );

        assertTrue(csv.contains("\"Books, pens\""));
        assertTrue(csv.contains("\"Bought \"\"study\"\" supplies\""));
        assertTrue(csv.contains("\",\"Others\","));
    }
}
