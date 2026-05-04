package com.spendlens.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.repository.SnapshotRepository;
import com.spendlens.app.utils.AppLockManager;
import com.spendlens.app.utils.PrefsManager;

import java.util.Calendar;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        AppDatabase.dbExecutor.execute(() -> {
            boolean profileExists =
                    AppDatabase.getInstance(this).userProfileDao().getCount() > 0;
            AppLockManager lock = AppLockManager.getInstance(this);

            if (profileExists) {
                // Auto-save last month snapshot if not already saved
                autoSaveLastMonthSnapshot();
            }

            runOnUiThread(() -> {
                Intent intent;
                if (!profileExists) {
                    PrefsManager.getInstance(this).setOnboardingDone(false);
                    intent = new Intent(this, WelcomeActivity.class);
                } else if (lock.shouldShowLock()) {
                    intent = new Intent(this, LockScreenActivity.class);
                } else {
                    PrefsManager.getInstance(this).setOnboardingDone(true);
                    intent = new Intent(this, HomeDashboardActivity.class);
                }
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        });
    }

    /**
     * Checks if last month has a saved snapshot — if not, auto-generates one.
     * This ensures past months are captured even if the user never visited
     * Budget History manually.
     *
     * Runs on background thread (called from dbExecutor).
     */
    private void autoSaveLastMonthSnapshot() {
        try {
            Calendar cal = Calendar.getInstance();

            // Step back to last month
            cal.add(Calendar.MONTH, -1);
            int lastMonth = cal.get(Calendar.MONTH) + 1; // 1-12
            int lastYear  = cal.get(Calendar.YEAR);

            AppDatabase db = AppDatabase.getInstance(this);

            // Check if snapshot already exists for last month
            com.spendlens.app.entities.MonthlySnapshot existing =
                    db.monthlySnapshotDao().getSnapshotForMonth(lastMonth, lastYear);

            if (existing == null) {
                // No snapshot yet — generate one now
                com.spendlens.app.entities.UserProfile profile =
                        db.userProfileDao().getProfileSync();
                double budget = profile != null ? profile.monthlyBudget : 0;

                SnapshotRepository repo = new SnapshotRepository(getApplication());
                repo.generateSnapshot(lastMonth, lastYear, budget);
            }
        } catch (Exception e) {
            // Silently ignore — snapshot is non-critical
        }
    }
}