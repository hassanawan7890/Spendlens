package com.spendlens.app;

import com.spendlens.app.utils.CurrencyUtils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CurrencyUtils
 *
 * Place in: app/src/test/java/com/spendlens/app/CurrencyUtilsTest.java
 * Run with: Right-click → Run 'CurrencyUtilsTest'
 */
public class CurrencyUtilsTest {

    // ── parse ─────────────────────────────────────────────────────────────────

    @Test
    public void parse_validInteger_returnsCorrectValue() {
        assertEquals(100.0, CurrencyUtils.parse("100"), 0.01);
    }

    @Test
    public void parse_validDecimal_returnsCorrectValue() {
        assertEquals(123.45, CurrencyUtils.parse("123.45"), 0.01);
    }

    @Test
    public void parse_numberWithCommas_stripsCommasAndParses() {
        assertEquals(1500.0, CurrencyUtils.parse("1,500"), 0.01);
    }

    @Test
    public void parse_nullInput_returnsZero() {
        assertEquals(0.0, CurrencyUtils.parse(null), 0.01);
    }

    @Test
    public void parse_emptyString_returnsZero() {
        assertEquals(0.0, CurrencyUtils.parse(""), 0.01);
    }

    @Test
    public void parse_invalidText_returnsZero() {
        assertEquals(0.0, CurrencyUtils.parse("abc"), 0.01);
    }

    @Test
    public void parse_whitespaceOnly_returnsZero() {
        assertEquals(0.0, CurrencyUtils.parse("   "), 0.01);
    }

    // ── isValidAmount ─────────────────────────────────────────────────────────

    @Test
    public void isValidAmount_positiveNumber_returnsTrue() {
        assertTrue(CurrencyUtils.isValidAmount("50.00"));
    }

    @Test
    public void isValidAmount_zero_returnsFalse() {
        assertFalse(CurrencyUtils.isValidAmount("0"));
    }

    @Test
    public void isValidAmount_negativeNumber_returnsFalse() {
        assertFalse(CurrencyUtils.isValidAmount("-10"));
    }

    @Test
    public void isValidAmount_emptyString_returnsFalse() {
        assertFalse(CurrencyUtils.isValidAmount(""));
    }

    @Test
    public void isValidAmount_text_returnsFalse() {
        assertFalse(CurrencyUtils.isValidAmount("hello"));
    }

    // ── formatPlain ───────────────────────────────────────────────────────────

    @Test
    public void formatPlain_zeroOrNegative_returnsEmptyString() {
        assertEquals("", CurrencyUtils.formatPlain(0));
        assertEquals("", CurrencyUtils.formatPlain(-5));
    }

    // ── formatPercent ─────────────────────────────────────────────────────────

    @Test
    public void formatPercent_wholeNumber_returnsNoDecimal() {
        assertEquals("50%", CurrencyUtils.formatPercent(0.5));
    }

    @Test
    public void formatPercent_fullBudget_returns100Percent() {
        assertEquals("100%", CurrencyUtils.formatPercent(1.0));
    }

    @Test
    public void formatPercent_zeroBudget_returns0Percent() {
        assertEquals("0%", CurrencyUtils.formatPercent(0.0));
    }

    // ── formatDiff ────────────────────────────────────────────────────────────

    @Test
    public void formatDiff_positiveDiff_hasPlusSign() {
        String result = CurrencyUtils.formatDiff("CAD", 50.0);
        assertTrue(result.startsWith("+"));
    }

    @Test
    public void formatDiff_negativeDiff_hasMinusSign() {
        String result = CurrencyUtils.formatDiff("CAD", -50.0);
        assertTrue(result.startsWith("-"));
    }
}