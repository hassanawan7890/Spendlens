package com.spendlens.app.utils;

import com.spendlens.app.entities.Expense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExpenseCsvExporter {

    private static final String HEADER =
            "title,amount,currency,category,date,mood,payment_method,note\n";

    private ExpenseCsvExporter() {}

    public static String buildCsv(List<Expense> expenses,
                                  Map<Integer, String> categoryNames,
                                  String currency) {
        StringBuilder builder = new StringBuilder(HEADER);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String activeCurrency = currency != null ? currency : "CAD";

        for (Expense expense : expenses) {
            String categoryName = categoryNames.get(expense.categoryId);
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = CategoryDisplayUtils.DEFAULT_CATEGORY_NAME;
            }

            appendCell(builder, expense.title);
            appendCell(builder, CurrencyUtils.formatPlain(expense.amount));
            appendCell(builder, activeCurrency);
            appendCell(builder, categoryName);
            appendCell(builder, dateFormat.format(new Date(expense.date)));
            appendCell(builder, expense.moodTag);
            appendCell(builder, expense.paymentMethod);
            appendLastCell(builder, expense.note);
        }

        return builder.toString();
    }

    private static void appendCell(StringBuilder builder, String value) {
        builder.append(escape(value)).append(',');
    }

    private static void appendLastCell(StringBuilder builder, String value) {
        builder.append(escape(value)).append('\n');
    }

    static String escape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
