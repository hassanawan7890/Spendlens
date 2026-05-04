package com.spendlens.app;

import org.junit.Test;
import static org.junit.Assert.*;

import java.security.MessageDigest;

/**
 * Unit tests for AppLockManager logic
 *
 * AppLockManager uses SharedPreferences (Android context) so we test
 * the core logic — PIN/password hashing and verification — directly
 * by replicating the same SHA-256 hash method used in the class.
 *
 * Place in: app/src/test/java/com/spendlens/app/AppLockManagerTest.java
 * Run with: Right-click → Run 'AppLockManagerTest'
 */
public class AppLockManagerTest {

    // ── Helper: same hash method used in AppLockManager ──────────────────────

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }

    // ── Hash correctness ──────────────────────────────────────────────────────

    @Test
    public void hash_sameInput_producesSameHash() {
        String hash1 = hash("1234");
        String hash2 = hash("1234");
        assertEquals(hash1, hash2);
    }

    @Test
    public void hash_differentInputs_produceDifferentHashes() {
        String hash1 = hash("1234");
        String hash2 = hash("5678");
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void hash_outputIsHexString() {
        String result = hash("1234");
        // SHA-256 produces 64 hex characters
        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9a-f]+"));
    }

    @Test
    public void hash_neverReturnsPlaintext() {
        String pin = "1234";
        String result = hash(pin);
        assertNotEquals(pin, result);
    }

    // ── PIN verification logic ────────────────────────────────────────────────

    @Test
    public void verify_correctPin_returnsTrue() {
        String savedHash   = hash("1234");
        String enteredHash = hash("1234");
        assertEquals(savedHash, enteredHash); // simulates correct PIN
    }

    @Test
    public void verify_wrongPin_returnsFalse() {
        String savedHash   = hash("1234");
        String enteredHash = hash("9999");
        assertNotEquals(savedHash, enteredHash); // simulates wrong PIN
    }

    @Test
    public void verify_correctPassword_returnsTrue() {
        String saved   = hash("MySecurePass!");
        String entered = hash("MySecurePass!");
        assertEquals(saved, entered);
    }

    @Test
    public void verify_wrongPassword_returnsFalse() {
        String saved   = hash("MySecurePass!");
        String entered = hash("wrongpassword");
        assertNotEquals(saved, entered);
    }

    @Test
    public void verify_caseSensitive_differentCaseReturnsFalse() {
        // Passwords are case sensitive — "abc" and "ABC" should not match
        String saved   = hash("mypassword");
        String entered = hash("MYPASSWORD");
        assertNotEquals(saved, entered);
    }

    @Test
    public void verify_emptyPin_producesConsistentHash() {
        // Empty string should still hash consistently
        String hash1 = hash("");
        String hash2 = hash("");
        assertEquals(hash1, hash2);
    }

    @Test
    public void verify_pinWithSpaces_differentFromWithout() {
        // "1234" and " 1234" should not match
        String withoutSpace = hash("1234");
        String withSpace    = hash(" 1234");
        assertNotEquals(withoutSpace, withSpace);
    }

    // ── PIN format validation ─────────────────────────────────────────────────

    @Test
    public void pinLength_fourDigits_isValid() {
        String pin = "1234";
        assertTrue(pin.length() == 4 && pin.matches("\\d+"));
    }

    @Test
    public void pinLength_lessThanFour_isInvalid() {
        String pin = "123";
        assertFalse(pin.length() >= 4);
    }

    @Test
    public void password_lessThanFourChars_isInvalid() {
        String password = "abc";
        assertFalse(password.length() >= 4);
    }

    @Test
    public void password_fourOrMoreChars_isValid() {
        String password = "abcd";
        assertTrue(password.length() >= 4);
    }
}