package com.spendlens.app.utils;

import android.text.TextUtils;

import com.google.android.material.textfield.TextInputLayout;

/**
 * ValidationUtils
 *
 * Keeps validation logic out of Activities.
 * Each method sets the error on the TextInputLayout and returns true/false.
 *
 * Usage:
 *   boolean ok = ValidationUtils.requireNonEmpty(binding.layoutName, nameValue, "Name is required");
 */
public class ValidationUtils {

    /**
     * Fails if input is null or blank.
     */
    public static boolean requireNonEmpty(TextInputLayout layout, String value, String errorMsg) {
        if (TextUtils.isEmpty(value != null ? value.trim() : null)) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }

    /**
     * Fails if the string is not a valid positive double.
     */
    public static boolean requirePositiveAmount(TextInputLayout layout, String value,
                                                String emptyError, String invalidError) {
        if (TextUtils.isEmpty(value != null ? value.trim() : null)) {
            layout.setError(emptyError);
            return false;
        }
        double parsed = CurrencyUtils.parse(value);
        if (parsed <= 0) {
            layout.setError(invalidError);
            return false;
        }
        layout.setError(null);
        return true;
    }

    /**
     * Fails if value is null or <= 0 (for spinner/dropdown selections).
     * Pass -1 as "nothing selected" sentinel.
     */
    public static boolean requireSelection(TextInputLayout layout, int selectedId, String errorMsg) {
        if (selectedId <= 0) {
            layout.setError(errorMsg);
            return false;
        }
        layout.setError(null);
        return true;
    }

    /**
     * Clears all errors on the given layouts.
     */
    public static void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            if (layout != null) layout.setError(null);
        }
    }
}
