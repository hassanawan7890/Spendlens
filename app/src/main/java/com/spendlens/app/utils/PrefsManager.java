package com.spendlens.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.spendlens.app.ai.AiConfig;

public class PrefsManager {

    private static final String PREFS_NAME = "spendlens_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_AI_ENABLED = "ai_enabled";
    private static final String KEY_AI_RUNTIME = "ai_runtime";
    private static final String KEY_AI_MODEL_PATH = "ai_model_path";
    private static final String KEY_AI_LORA_PATH = "ai_lora_path";
    private static final String KEY_AI_IMPORT_ENABLED = "ai_import_enabled";
    private static final String KEY_AI_COPILOT_ENABLED = "ai_copilot_enabled";
    private static final String KEY_AI_BUNDLED_SETUP_DONE = "ai_bundled_setup_done";

    private final SharedPreferences prefs;
    private static PrefsManager instance;

    private PrefsManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static PrefsManager getInstance(Context ctx) {
        if (instance == null) instance = new PrefsManager(ctx);
        return instance;
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public String getCurrency() {
        return prefs.getString(KEY_CURRENCY, "RM");
    }

    public void setCurrency(String currency) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getThemeMode() {
        return prefs.getString(KEY_THEME, "light");
    }

    public void setThemeMode(String mode) {
        prefs.edit().putString(KEY_THEME, mode).apply();
    }

    public boolean isDarkMode() {
        return "dark".equals(getThemeMode());
    }

    public void setAiEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AI_ENABLED, enabled).apply();
    }

    public boolean isAiEnabled() {
        return prefs.getBoolean(KEY_AI_ENABLED, false);
    }

    public void setAiRuntime(String runtime) {
        prefs.edit().putString(KEY_AI_RUNTIME, runtime != null ? runtime.trim() : "").apply();
    }

    public String getAiRuntime() {
        return prefs.getString(KEY_AI_RUNTIME, AiConfig.RUNTIME_MEDIAPIPE);
    }

    public void setAiModelPath(String modelPath) {
        prefs.edit().putString(KEY_AI_MODEL_PATH, modelPath != null ? modelPath.trim() : "").apply();
    }

    public String getAiModelPath() {
        return prefs.getString(KEY_AI_MODEL_PATH, "");
    }

    public void setAiLoraPath(String loraPath) {
        prefs.edit().putString(KEY_AI_LORA_PATH, loraPath != null ? loraPath.trim() : "").apply();
    }

    public String getAiLoraPath() {
        return prefs.getString(KEY_AI_LORA_PATH, "");
    }

    public void setAiStatementImportEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AI_IMPORT_ENABLED, enabled).apply();
    }

    public boolean isAiStatementImportEnabled() {
        return prefs.getBoolean(KEY_AI_IMPORT_ENABLED, true);
    }

    public void setAiBudgetCopilotEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AI_COPILOT_ENABLED, enabled).apply();
    }

    public boolean isAiBudgetCopilotEnabled() {
        return prefs.getBoolean(KEY_AI_COPILOT_ENABLED, true);
    }

    public boolean isAiBundledSetupDone() {
        return prefs.getBoolean(KEY_AI_BUNDLED_SETUP_DONE, false);
    }

    public void setAiBundledSetupDone(boolean done) {
        prefs.edit().putBoolean(KEY_AI_BUNDLED_SETUP_DONE, done).apply();
    }

    public AiConfig getAiConfig() {
        return new AiConfig(
                isAiEnabled(),
                getAiRuntime(),
                getAiModelPath(),
                getAiLoraPath(),
                isAiStatementImportEnabled(),
                isAiBudgetCopilotEnabled()
        );
    }

    public void saveAiConfig(AiConfig config) {
        setAiEnabled(config.enabled);
        setAiRuntime(config.runtime);
        setAiModelPath(config.modelPath);
        setAiLoraPath(config.loraPath);
        setAiStatementImportEnabled(config.statementImportEnabled);
        setAiBudgetCopilotEnabled(config.budgetCopilotEnabled);
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
