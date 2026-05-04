package com.spendlens.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.repository.StatementRepository;
import com.spendlens.app.utils.PrefsManager;

import java.io.InputStream;

public class UploadStatementActivity extends AppCompatActivity {

    private TextView tvFileName;
    private TextView tvStatus;
    private Button btnChooseFile;
    private Button btnImport;
    private ProgressBar progressBar;
    private LinearLayout layoutFileChosen;
    private Uri selectedFileUri;
    private String selectedFileName = "";
    private StatementRepository repo;
    private boolean aiImportReady;

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    selectedFileName = getFileName(selectedFileUri);
                    tvFileName.setText(selectedFileName);
                    layoutFileChosen.setVisibility(View.VISIBLE);
                    tvStatus.setText(aiImportReady
                            ? "File loaded. On-device AI will recognize the bank layout before preview."
                            : "File loaded. SpendLens will parse the rows and suggest categories.");
                    tvStatus.setTextColor(getColor(R.color.green_600));
                    btnImport.setEnabled(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_statement);

        repo = new StatementRepository(getApplication());
        aiImportReady = PrefsManager.getInstance(this).getAiConfig().canUseStatementImport();
        tvFileName = findViewById(R.id.tvFileName);
        tvStatus = findViewById(R.id.tvStatus);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnImport = findViewById(R.id.btnImport);
        progressBar = findViewById(R.id.progressBar);
        layoutFileChosen = findViewById(R.id.layoutFileChosen);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnImport.setEnabled(false);
        tvStatus.setText(aiImportReady
                ? "On-device AI is ready for unusual bank CSV or text layouts. Local parsing stays on as a fallback."
                : "Choose a CSV or text export from your bank to start the smart import flow.");

        btnBack.setOnClickListener(v -> finish());
        btnChooseFile.setOnClickListener(v -> openFilePicker());
        btnImport.setOnClickListener(v -> startImport());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel",
                "text/plain"
        });
        filePicker.launch(Intent.createChooser(intent, "Select Statement Export"));
    }

    private void startImport() {
        if (selectedFileUri == null) return;

        btnImport.setEnabled(false);
        btnChooseFile.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText(aiImportReady
                ? "Reading the file and letting on-device AI recognize the bank layout..."
                : "Reading transactions and building smart category suggestions...");
        tvStatus.setTextColor(getColor(R.color.gray_500));

        AppDatabase.dbExecutor.execute(() -> {
            int statementId = -1;
            try {
                InputStream stream = getContentResolver().openInputStream(selectedFileUri);
                if (stream != null) {
                    statementId = repo.importCsv(stream, selectedFileName);
                    stream.close();
                }
            } catch (Exception e) {
                final String error = e.getMessage();
                runOnUiThread(() -> showError("Error reading file: " + error));
                return;
            }

            if (statementId < 0) {
                runOnUiThread(() -> showError(
                        "Could not parse the file. Make sure it is a valid CSV or text statement export."
                ));
                return;
            }

            final int finalStatementId = statementId;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(this, ImportPreviewActivity.class);
                intent.putExtra("statement_id", finalStatementId);
                intent.putExtra("file_name", selectedFileName);
                startActivity(intent);
                finish();
            });
        });
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        btnImport.setEnabled(true);
        btnChooseFile.setEnabled(true);
        tvStatus.setText(message);
        tvStatus.setTextColor(getColor(R.color.red_600));
    }

    private String getFileName(Uri uri) {
        if (uri == null) return "unknown.csv";

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String displayName = cursor.getString(index);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        String path = uri.getLastPathSegment();
        if (path != null && path.contains("/")) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        return path != null ? path : "statement.csv";
    }
}
