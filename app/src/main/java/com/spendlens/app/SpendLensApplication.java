package com.spendlens.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;

import com.spendlens.app.ai.AiModelStore;
import com.spendlens.app.utils.AppLockManager;
import com.spendlens.app.utils.PrefsManager;

public class SpendLensApplication extends Application {

    private int activeActivities = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        PrefsManager prefs = PrefsManager.getInstance(this);

        String mode = prefs.getThemeMode();
        AppCompatDelegate.setDefaultNightMode(
                "light".equals(mode)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES
        );

        AiModelStore.ensureBundledModelConfigured(this, prefs);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, Bundle b) {}

            @Override
            public void onActivityStarted(Activity a) {
                activeActivities++;
            }

            @Override
            public void onActivityResumed(Activity a) {}

            @Override
            public void onActivityPaused(Activity a) {}

            @Override
            public void onActivityStopped(Activity a) {
                activeActivities--;
                if (activeActivities <= 0) {
                    AppLockManager.getInstance(getApplicationContext()).recordBackgroundTime();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {}

            @Override
            public void onActivityDestroyed(Activity a) {}
        });
    }
}
