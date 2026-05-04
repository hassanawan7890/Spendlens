package com.spendlens.app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.spendlens.app.R;
import com.spendlens.app.ai.AiConfig;
import com.spendlens.app.ai.AiModelStore;
import com.spendlens.app.utils.PrefsManager;

public class AiSettingsActivity extends AppCompatActivity {

    private PrefsManager prefs;
    private SwitchMaterial switchAiEnabled;
    private SwitchMaterial switchStatementImport;
    private SwitchMaterial switchBudgetCopilot;
    private MaterialButton btnSave;
    private MaterialButton btnSelectModel;
    private MaterialButton btnClearModel;
    private MaterialButton btnSelectLora;
    private MaterialButton btnClearLora;
    private TextView tvRuntimeValue;
    private TextView tvModelStatus;
    private TextView tvLoraStatus;
    private TextView tvSetupStatus;
    private ProgressBar progressAiSetup;
    private String modelPath = "";

    private final ActivityResultLauncher<String[]> modelPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importSelectedFile(uri, true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_settings);

        prefs = PrefsManager.getInstance(this);
        switchAiEnabled = findViewById(R.id.switchAiEnabled);
        switchStatementImport = findViewById(R.id.switchStatementImport);
        switchBudgetCopilot = findViewById(R.id.switchBudgetCopilot);
        btnSave = findViewById(R.id.btnSaveAiSettings);
        btnSelectModel = findViewById(R.id.btnSelectModel);
        btnClearModel = findViewById(R.id.btnClearModel);
        btnSelectLora = findViewById(R.id.btnSelectLora);
        btnClearLora = findViewById(R.id.btnClearLora);
        tvRuntimeValue = findViewById(R.id.tvRuntimeValue);
        tvModelStatus = findViewById(R.id.tvModelStatus);
        tvLoraStatus = findViewById(R.id.tvLoraStatus);
        tvSetupStatus = findViewById(R.id.tvSetupStatus);
        progressAiSetup = findViewById(R.id.progressAiSetup);
        ImageButton btnBack = findViewById(R.id.btnBack);

        bindConfig(prefs.getAiConfig());

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveConfig());
        btnSelectModel.setOnClickListener(v -> modelPicker.launch(new String[]{"*/*"}));
        btnClearModel.setOnClickListener(v -> clearSelection(true));
    }

    private void bindConfig(AiConfig config) {
        switchAiEnabled.setChecked(config.enabled);
        switchStatementImport.setChecked(config.statementImportEnabled);
        switchBudgetCopilot.setChecked(config.budgetCopilotEnabled);
        modelPath = config.modelPath;
        tvRuntimeValue.setText("Gemma via Google MediaPipe");
        refreshFileSummary();
    }

    private void saveConfig() {
        AiConfig config = new AiConfig(
                switchAiEnabled.isChecked(),
                AiConfig.RUNTIME_MEDIAPIPE,
                modelPath,
                "",
                switchStatementImport.isChecked(),
                switchBudgetCopilot.isChecked()
        );
        prefs.saveAiConfig(config);

        if (config.enabled && !config.isConfigured()) {
            Toast.makeText(
                    this,
                    "On-device AI was enabled, but a compatible .task model is still missing.",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(this, "On-device AI settings saved", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void importSelectedFile(Uri uri, boolean isModelFile) {
        setBusy(true, isModelFile
                ? "Copying model into SpendLens..."
                : "Copying LoRA adapter into SpendLens...");

        new Thread(() -> {
            try {
                String copiedPath = AiModelStore.importFromUri(
                        this,
                        uri,
                        isModelFile ? "primary-model" : "lora-adapter"
                );

                runOnUiThread(() -> {
                    if (isModelFile) {
                        modelPath = copiedPath;
                    }
                    refreshFileSummary();
                    setBusy(false, isModelFile
                            ? "Model ready. Save to enable local AI."
                            : "Model ready.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false, "Could not import that file.");
                    Toast.makeText(
                            this,
                            "Import failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void clearSelection(boolean isModelFile) {
        if (isModelFile) {
            AiModelStore.deleteManagedFile(modelPath);
            if (AiModelStore.isBundledModelReference(modelPath)) {
                prefs.setAiBundledSetupDone(false);
            }
            modelPath = "";
        }
        refreshFileSummary();
    }

    private void refreshFileSummary() {
        boolean hasModel = new AiConfig(
                true,
                AiConfig.RUNTIME_MEDIAPIPE,
                modelPath,
                "",
                true,
                true
        ).isConfigured();

        tvModelStatus.setText(hasModel
                ? (AiModelStore.isBundledModelReference(modelPath)
                    ? "Bundled Gemma 3 270M default"
                    : AiModelStore.getDisplayName(modelPath))
                : "No local model loaded");
        tvLoraStatus.setText("LoRA adapters are staged for a later update. Start with the base model first.");
        tvSetupStatus.setText(hasModel
                ? "SpendLens is ready to run import help and budget chat on this device."
                : (AiModelStore.bundledModelAssetExists(this)
                    ? "A bundled Gemma model is included locally and should auto-configure on first launch."
                    : "Load a MediaPipe-compatible .task model. Recommended starter: Gemma 3 270M or 1B."));
        btnClearModel.setEnabled(!modelPath.isEmpty());
        btnSelectLora.setEnabled(false);
        btnClearLora.setEnabled(false);
    }

    private void setBusy(boolean busy, String status) {
        progressAiSetup.setVisibility(busy ? View.VISIBLE : View.GONE);
        tvSetupStatus.setText(status);
        switchAiEnabled.setEnabled(!busy);
        switchStatementImport.setEnabled(!busy);
        switchBudgetCopilot.setEnabled(!busy);
        btnSelectModel.setEnabled(!busy);
        btnSelectLora.setEnabled(false);
        btnClearModel.setEnabled(!busy && !modelPath.isEmpty());
        btnClearLora.setEnabled(false);
        btnSave.setEnabled(!busy);
    }
}
