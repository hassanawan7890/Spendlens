package com.spendlens.app.ai;

import java.io.File;

public class AiConfig {

    public static final String RUNTIME_MEDIAPIPE = "mediapipe";
    public static final String BUNDLED_MODEL_SENTINEL = "@bundled_spendlens_default";

    public final boolean enabled;
    public final String runtime;
    public final String modelPath;
    public final String loraPath;
    public final boolean statementImportEnabled;
    public final boolean budgetCopilotEnabled;

    public AiConfig(boolean enabled,
                    String runtime,
                    String modelPath,
                    String loraPath,
                    boolean statementImportEnabled,
                    boolean budgetCopilotEnabled) {
        this.enabled = enabled;
        this.runtime = runtime != null && !runtime.trim().isEmpty()
                ? runtime.trim()
                : RUNTIME_MEDIAPIPE;
        this.modelPath = modelPath != null ? modelPath.trim() : "";
        this.loraPath = loraPath != null ? loraPath.trim() : "";
        this.statementImportEnabled = statementImportEnabled;
        this.budgetCopilotEnabled = budgetCopilotEnabled;
    }

    public boolean isConfigured() {
        return isBundledModel() || hasFile(modelPath);
    }

    public boolean hasLora() {
        return hasFile(loraPath);
    }

    public boolean isBundledModel() {
        return BUNDLED_MODEL_SENTINEL.equals(modelPath);
    }

    public boolean canUseStatementImport() {
        return enabled && statementImportEnabled && isConfigured();
    }

    public boolean canUseBudgetCopilot() {
        return enabled && budgetCopilotEnabled && isConfigured();
    }

    public String getModelFileName() {
        if (modelPath.isEmpty()) return "";
        return new File(modelPath).getName();
    }

    public String getLoraFileName() {
        if (loraPath.isEmpty()) return "";
        return new File(loraPath).getName();
    }

    private boolean hasFile(String path) {
        return !path.isEmpty() && new File(path).exists();
    }
}
