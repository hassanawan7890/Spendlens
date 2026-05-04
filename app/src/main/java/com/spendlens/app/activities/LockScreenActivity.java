package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.spendlens.app.R;
import com.spendlens.app.utils.AppLockManager;

public class LockScreenActivity extends AppCompatActivity {

    private AppLockManager lockManager;
    private final StringBuilder pinBuffer = new StringBuilder();

    private TextView tvPinDisplay, tvError, tvLockType;
    private LinearLayout layoutPinEntry, layoutPasswordEntry;
    private EditText editPasswordUnlock;
    private int failedAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        lockManager         = AppLockManager.getInstance(this);
        tvPinDisplay        = findViewById(R.id.tvPinDisplay);
        tvError             = findViewById(R.id.tvLockError);
        tvLockType          = findViewById(R.id.tvLockType);
        layoutPinEntry      = findViewById(R.id.layoutPinEntry);
        layoutPasswordEntry = findViewById(R.id.layoutPasswordEntry);
        editPasswordUnlock  = findViewById(R.id.editPasswordUnlock);

        // Block back button — lock cannot be bypassed
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { /* blocked */ }
        });

        if (lockManager.isPinMode()) {
            layoutPinEntry.setVisibility(View.VISIBLE);
            layoutPasswordEntry.setVisibility(View.GONE);
            tvLockType.setText("Enter your PIN");
            setupNumpad();
        } else {
            layoutPinEntry.setVisibility(View.GONE);
            layoutPasswordEntry.setVisibility(View.VISIBLE);
            tvLockType.setText("Enter your password");
            findViewById(R.id.btnUnlockPassword)
                    .setOnClickListener(v -> attemptPasswordUnlock());
        }
    }

    private void setupNumpad() {
        int[] numIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };
        for (int i = 0; i < numIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(numIds[i]).setOnClickListener(v -> onPinDigit(digit));
        }
        findViewById(R.id.btnPinDelete).setOnClickListener(v -> onPinDelete());
        updatePinDisplay();
    }

    private void onPinDigit(String digit) {
        if (pinBuffer.length() >= 6) return;
        pinBuffer.append(digit);
        updatePinDisplay();
        if (pinBuffer.length() == 4) {
            new Handler().postDelayed(this::attemptPinUnlock, 200);
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
        for (int i = 0; i < 4; i++) dots.append(i < len ? "\u25CF  " : "\u25CB  ");
        tvPinDisplay.setText(dots.toString().trim());
    }

    private void attemptPinUnlock() {
        if (lockManager.verify(pinBuffer.toString())) {
            unlockSuccess();
        } else {
            failedAttempts++;
            pinBuffer.setLength(0);
            updatePinDisplay();
            tvError.setText("Incorrect PIN. " + getRemainingAttemptsMsg());
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void attemptPasswordUnlock() {
        String entered = editPasswordUnlock.getText() != null
                ? editPasswordUnlock.getText().toString() : "";
        if (lockManager.verify(entered)) {
            unlockSuccess();
        } else {
            failedAttempts++;
            editPasswordUnlock.setText("");
            tvError.setText("Incorrect password. " + getRemainingAttemptsMsg());
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private String getRemainingAttemptsMsg() {
        if (failedAttempts >= 3) return "Too many attempts.";
        int left = 3 - failedAttempts;
        return left + " attempt" + (left == 1 ? "" : "s") + " remaining.";
    }

    private void unlockSuccess() {
        lockManager.clearBackgroundTime();
        tvError.setVisibility(View.GONE);
        // Launch Dashboard with a clean back stack
        Intent intent = new Intent(this, HomeDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
