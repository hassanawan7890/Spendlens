package com.spendlens.app;

import com.spendlens.app.utils.CurrencyUtils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ValidationUtils logic
 *
 * ValidationUtils uses TextInputLayout (Android) so we test the
 * underlying validation logic directly — the same rules the methods apply.
 *
 * Place in: app/src/test/java/com/spendlens/app/ValidationUtilsTest.java
 * Run with: Right-click → Run 'ValidationUtilsTest'
 */
public class ValidationUtilsTest {

    // ── Helper: replicates requireNonEmpty logic ──────────────────────────────

    private boolean isNonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // ── Helper: replicates requirePositiveAmount logic ────────────────────────

    private boolean isPositiveAmount(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        double parsed = CurrencyUtils.parse(value);
        return parsed > 0;
    }

    // ── Helper: replicates requireSelection logic ─────────────────────────────

    private boolean isValidSelection(int selectedId) {
        return selectedId > 0;
    }

    // ── requireNonEmpty ───────────────────────────────────────────────────────

    @Test
    public void requireNonEmpty_normalString_returnsTrue() {
        assertTrue(isNonEmpty("John"));
    }

    @Test
    public void requireNonEmpty_emptyString_returnsFalse() {
        assertFalse(isNonEmpty(""));
    }

    @Test
    public void requireNonEmpty_whitespaceOnly_returnsFalse() {
        assertFalse(isNonEmpty("   "));
    }

    @Test
    public void requireNonEmpty_nullInput_returnsFalse() {
        assertFalse(isNonEmpty(null));
    }

    @Test
    public void requireNonEmpty_stringWithSpaces_returnsTrue() {
        assertTrue(isNonEmpty("  valid  "));
    }

    @Test
    public void requireNonEmpty_singleChar_returnsTrue() {
        assertTrue(isNonEmpty("a"));
    }

    // ── requirePositiveAmount ─────────────────────────────────────────────────

    @Test
    public void requirePositiveAmount_validPositive_returnsTrue() {
        assertTrue(isPositiveAmount("150.00"));
    }

    @Test
    public void requirePositiveAmount_zero_returnsFalse() {
        assertFalse(isPositiveAmount("0"));
    }

    @Test
    public void requirePositiveAmount_negative_returnsFalse() {
        assertFalse(isPositiveAmount("-50"));
    }

    @Test
    public void requirePositiveAmount_emptyString_returnsFalse() {
        assertFalse(isPositiveAmount(""));
    }

    @Test
    public void requirePositiveAmount_nullInput_returnsFalse() {
        assertFalse(isPositiveAmount(null));
    }

    @Test
    public void requirePositiveAmount_textInput_returnsFalse() {
        assertFalse(isPositiveAmount("abc"));
    }

    @Test
    public void requirePositiveAmount_smallDecimal_returnsTrue() {
        assertTrue(isPositiveAmount("0.01"));
    }

    @Test
    public void requirePositiveAmount_largeAmount_returnsTrue() {
        assertTrue(isPositiveAmount("99999.99"));
    }

    @Test
    public void requirePositiveAmount_withCommas_returnsTrue() {
        assertTrue(isPositiveAmount("1,500"));
    }

    // ── requireSelection ──────────────────────────────────────────────────────

    @Test
    public void requireSelection_validId_returnsTrue() {
        assertTrue(isValidSelection(1));
    }

    @Test
    public void requireSelection_negativeOne_returnsFalse() {
        // -1 is the "nothing selected" sentinel
        assertFalse(isValidSelection(-1));
    }

    @Test
    public void requireSelection_zero_returnsFalse() {
        assertFalse(isValidSelection(0));
    }

    @Test
    public void requireSelection_largeId_returnsTrue() {
        assertTrue(isValidSelection(999));
    }
}