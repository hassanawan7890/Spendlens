package com.spendlens.app.utils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * CurrencyUtils
 *
 * Single source of truth for all amount formatting.
 * Always call these methods — never build currency strings inline in Activities.
 *
 * Examples:
 *   format("RM", 1500.0)   → "RM 1,500.00"
 *   formatShort("RM", 847) → "RM 847"
 *   formatChange(847, 900) → "+RM 53.00"  (green text case)
 */
public class CurrencyUtils {

    /**
     * Full format with 2 decimal places.
     * "RM 1,500.00" — used on Dashboard hero card and expense list.
     */
    public static String format(String currency, double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return currency + " " + nf.format(amount);
    }

    /**
     * No decimals for whole numbers, 2 decimals otherwise.
     * "RM 847" or "RM 847.50" — used on mini stat cards.
     */
    public static String formatSmart(String currency, double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
            nf.setMinimumFractionDigits(0);
            nf.setMaximumFractionDigits(0);
            return currency + " " + nf.format((long) amount);
        }
        return format(currency, amount);
    }

    /**
     * Plain number without currency symbol.
     * Used inside TextInputEditText fields so user doesn't have to type the symbol.
     */
    public static String formatPlain(double amount) {
        if (amount <= 0) return "";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount);
    }

    /**
     * Safe parse — returns 0.0 on bad input instead of crashing.
     * Always use this when reading amount from EditText.
     */
    public static double parse(String input) {
        if (input == null || input.trim().isEmpty()) return 0.0;
        try {
            // Strip commas and spaces before parsing
            String cleaned = input.trim()
                    .replace(",", "")
                    .replace(" ", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Validate: is the input a valid positive amount?
     */
    public static boolean isValidAmount(String input) {
        double val = parse(input);
        return val > 0;
    }

    /**
     * Format a percentage: "56%" or "56.3%"
     */
    public static String formatPercent(double fraction) {
        double pct = fraction * 100.0;
        if (pct == Math.floor(pct)) {
            return (int) pct + "%";
        }
        return String.format(Locale.getDefault(), "%.1f%%", pct);
    }

    /**
     * Format as a diff — used for over-budget indicators.
     * Returns "+RM 50.00" or "-RM 50.00"
     */
    public static String formatDiff(String currency, double diff) {
        String sign = diff >= 0 ? "+" : "-";
        return sign + format(currency, Math.abs(diff));
    }
}
