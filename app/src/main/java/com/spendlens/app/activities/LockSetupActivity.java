package com.spendlens.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

        import androidx.appcompat.app.AppCompatActivity;

import com.spendlens.app.R;
import com.spendlens.app.utils.AppLockManager;

/**
 * LockSetupActivity
 *
 * Shown when the user enables app lock from Settings.
 * Step 1: Choose lock type (PIN or Password)
 * Step 2: Enter and confirm the chosen credential
 */
public class LockSetupActivity extends AppCompatActivity {

    private AppLockManager lockManager;

    // Step tracking
    private boolean pinModeSelected = true;
    private String firstEntry = null;  // stores first entry for confirmation step
    private boolean isConfirmStep = false;

    // Views
    private TextView tvTitle, tvSubtitle, tvPinDisplay, tvError;
    private LinearLayout layoutTypeChooser;
    private LinearLayout layoutPinEntry;
    private LinearLayout layoutPasswordEntry;
    private android.widget.EditText editPassword;
    private android.widget.EditText editPasswordConfirm;
    private StringBuilder pinBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_setup);

        lockManager = AppLockManager.getInstance(this);

        tvTitle         = findViewById(R.id.tvLockSetupTitle);
        tvSubtitle      = findViewById(R.id.tvLockSetupSubtitle);
        tvPinDisplay    = findViewById(R.id.tvPinDisplay);
        tvError         = findViewById(R.id.tvLockError);
        layoutTypeChooser = findViewById(R.id.layoutTypeChooser);
        layoutPinEntry    = findViewById(R.id.layoutPinEntry);
        layoutPasswordEntry = findViewById(R.id.layoutPasswordEntry);
        editPassword        = findViewById(R.id.editPassword);
        editPasswordConfirm = findViewById(R.id.editPasswordConfirm);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Type chooser buttons
        findViewById(R.id.btnChoosePin).setOnClickListener(v -> {
            pinModeSelected = true;
            showPinSetup();
        });
        findViewById(R.id.btnChoosePassword).setOnClickListener(v -> {
            pinModeSelected = false;
            showPasswordSetup();
        });

        // PIN numpad
        int[] numIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int i = 0; i < numIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(numIds[i]).setOnClickListener(v -> onPinDigit(digit));
        }
        findViewById(R.id.btnPinDelete).setOnClickListener(v -> onPinDelete());

        // Password confirm button
        findViewById(R.id.btnSetPassword).setOnClickListener(v -> attemptSetPassword());
    }

    private void showPinSetup() {
        layoutTypeChooser.setVisibility(View.GONE);
        layoutPasswordEntry.setVisibility(View.GONE);
        layoutPinEntry.setVisibility(View.VISIBLE);
        tvTitle.setText("Set your PIN");
        tvSubtitle.setText("Enter a 4-digit PIN");
        pinBuffer.setLength(0);
        updatePinDisplay();
    }

    private void showPasswordSetup() {
        layoutTypeChooser.setVisibility(View.GONE);
        layoutPinEntry.setVisibility(View.GONE);
        layoutPasswordEntry.setVisibility(View.VISIBLE);
        tvTitle.setText("Set your password");
        tvSubtitle.setText("Choose a password to protect SpendLens");
    }

    // ── PIN logic ─────────────────────────────────────────────────────────

    private void onPinDigit(String digit) {
        if (pinBuffer.length() >= 6) return;
        pinBuffer.append(digit);
        updatePinDisplay();
        if (pinBuffer.length() == 4) {
            // Auto-advance at 4 digits
            new android.os.Handler().postDelayed(this::processPin, 200);
        }
    }

    private void onPinDelete() {
        if (pinBuffer.length() > 0) {
            pinBuffer.deleteCharAt(pinBuffer.length() - 1);
            updatePinDisplay();
        }
    }

    private void updatePinDisplay() {
        int len = pinBuffer.length();
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            dots.append(i < len ? "●  " : "○  ");
        }
        tvPinDisplay.setText(dots.toString().trim());
    }

    private void processPin() {
        String pin = pinBuffer.toString();
        if (!isConfirmStep) {
            // First entry — move to confirm
            firstEntry = pin;
            isConfirmStep = true;
            pinBuffer.setLength(0);
            updatePinDisplay();
            tvTitle.setText("Confirm your PIN");
            tvSubtitle.setText("Enter the same PIN again");
            tvError.setVisibility(View.GONE);
        } else {
            // Confirmation
            if (pin.equals(firstEntry)) {
                saveLock("pin", pin);
            } else {
                tvError.setText("PINs don\'t match. Try again.");
                tvError.setVisibility(View.VISIBLE);
                isConfirmStep = false;
                firstEntry = null;
                pinBuffer.setLength(0);
                updatePinDisplay();
                tvTitle.setText("Set your PIN");
                tvSubtitle.setText("Enter a 4-digit PIN");
            }
        }
    }

    // ── Password logic ────────────────────────────────────────────────────

    private void attemptSetPassword() {
        String pw  = editPassword.getText() != null ? editPassword.getText().toString() : "";
        String pw2 = editPasswordConfirm.getText() != null ? editPasswordConfirm.getText().toString() : "";

        if (pw.length() < 4) {
            tvError.setText("Password must be at least 4 characters.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        if (!pw.equals(pw2)) {
            tvError.setText("Passwords don\'t match. Try again.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        saveLock("password", pw);
    }

    // ── Save and finish ───────────────────────────────────────────────────

    private void saveLock(String type, String credential) {
        lockManager.setLockType(type);
        lockManager.saveCredential(credential);
        lockManager.setLockEnabled(true);
        Toast.makeText(this, "App lock enabled!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
