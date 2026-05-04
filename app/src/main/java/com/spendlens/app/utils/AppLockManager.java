package com.spendlens.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;

/**
 * AppLockManager
 *
 * Manages the app lock state:
 *  - Whether lock is enabled
 *  - Lock type: "pin" or "password"
 *  - Stores hashed credential in SharedPreferences (never plaintext)
 *  - Tracks when the app was backgrounded
 */
public class AppLockManager {

    private static final String PREFS_NAME  = "spendlens_lock";
    private static final String KEY_ENABLED = "lock_enabled";
    private static final String KEY_TYPE    = "lock_type";   // "pin" or "password"
    private static final String KEY_HASH    = "lock_hash";   // SHA-256 hash
    private static final String KEY_BG_TIME = "bg_timestamp";

    private static AppLockManager instance;
    private final SharedPreferences prefs;

    private AppLockManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static AppLockManager getInstance(Context context) {
        if (instance == null) instance = new AppLockManager(context);
        return instance;
    }

    // ── Lock enabled state ────────────────────────────────────────────────

    public boolean isLockEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    // ── Lock type ─────────────────────────────────────────────────────────

    public String getLockType() {
        return prefs.getString(KEY_TYPE, "pin");
    }

    public void setLockType(String type) {
        prefs.edit().putString(KEY_TYPE, type).apply();
    }

    public boolean isPinMode() {
        return "pin".equals(getLockType());
    }

    // ── Credential setup ──────────────────────────────────────────────────

    public void saveCredential(String raw) {
        prefs.edit().putString(KEY_HASH, hash(raw)).apply();
    }

    public boolean verify(String entered) {
        String stored = prefs.getString(KEY_HASH, null);
        if (stored == null) return false;
        return stored.equals(hash(entered));
    }

    public boolean hasCredential() {
        return prefs.getString(KEY_HASH, null) != null;
    }

    public void clearCredential() {
        prefs.edit()
                .remove(KEY_HASH)
                .remove(KEY_ENABLED)
                .remove(KEY_TYPE)
                .remove(KEY_BG_TIME)
                .apply();
    }

    // ── Background time ───────────────────────────────────────────────────

    public void recordBackgroundTime() {
        prefs.edit().putLong(KEY_BG_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Returns true whenever the app is reopened from background
     * and lock is enabled — no timeout, immediate lock every time.
     */
    public boolean shouldShowLock() {
        if (!isLockEnabled() || !hasCredential()) return false;
        // Lock every time the app comes back from background
        long bgTime = prefs.getLong(KEY_BG_TIME, 0);
        return bgTime > 0;
    }

    public void clearBackgroundTime() {
        prefs.edit().remove(KEY_BG_TIME).apply();
    }

    // ── Hashing ───────────────────────────────────────────────────────────

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
}
