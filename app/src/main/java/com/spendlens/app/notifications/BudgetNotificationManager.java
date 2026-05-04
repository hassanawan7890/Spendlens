package com.spendlens.app.notifications;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.spendlens.app.R;
import com.spendlens.app.activities.HomeDashboardActivity;
import com.spendlens.app.utils.CurrencyUtils;

@SuppressLint("MissingPermission")
public class BudgetNotificationManager {

    private static final String CHANNEL_ID       = "spendlens_budget_alerts";
    private static final String CHANNEL_NAME     = "Budget Alerts";
    private static final String CHANNEL_DESC     = "Notifies when spending reaches budget thresholds";
    private static final int    NOTIF_ID_WARNING  = 1001;
    private static final int    NOTIF_ID_CRITICAL = 1002;
    private static final String PREFS_NAME         = "spendlens_notif_prefs";
    private static final String KEY_NOTIF_ON       = "notifications_enabled";
    private static final String KEY_WARNED_MONTH   = "warned_month";
    private static final String KEY_CRITICAL_MONTH = "critical_month";
    private static final double WARNING_THRESHOLD_PERCENT = 80.0;

    private final Context context;
    private final SharedPreferences prefs;

    public BudgetNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs   = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        createNotificationChannel();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_ON, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_ON, enabled).apply();
    }

    public void checkAndNotify(double spent, double budget, String currency) {
        if (!isNotificationsEnabled()) return;
        if (budget <= 0) return;
        if (!hasPermission()) return;

        double percent      = (spent / budget) * 100.0;
        String currentMonth = getCurrentMonthKey();

        if (percent >= 100.0) {
            String last = prefs.getString(KEY_CRITICAL_MONTH, "");
            if (!currentMonth.equals(last)) {
                sendCritical(spent, budget, currency);
                prefs.edit()
                        .putString(KEY_CRITICAL_MONTH, currentMonth)
                        .putString(KEY_WARNED_MONTH, currentMonth)
                        .apply();
            }
        } else if (percent >= WARNING_THRESHOLD_PERCENT) {
            String last = prefs.getString(KEY_WARNED_MONTH, "");
            if (!currentMonth.equals(last)) {
                sendWarning(percent, budget, currency);
                prefs.edit().putString(KEY_WARNED_MONTH, currentMonth).apply();
            }
        }
    }

    public void resetNotificationState() {
        prefs.edit().remove(KEY_WARNED_MONTH).remove(KEY_CRITICAL_MONTH).apply();
    }

    private void sendWarning(double percent, double budget, String currency) {
        String title = "Heads up — budget running low";
        String body  = "You have used " + (int) percent + "% of your "
                + CurrencyUtils.formatSmart(currency, budget) + " monthly budget.";
        notify(NOTIF_ID_WARNING, R.drawable.ic_notif_warning,
                title, body, NotificationCompat.PRIORITY_DEFAULT);
    }

    private void sendCritical(double spent, double budget, String currency) {
        double over  = spent - budget;
        String title = "Budget exceeded";
        String body  = "You are " + CurrencyUtils.formatSmart(currency, over)
                + " over your " + CurrencyUtils.formatSmart(currency, budget)
                + " monthly budget.";
        notify(NOTIF_ID_CRITICAL, R.drawable.ic_notif_critical,
                title, body, NotificationCompat.PRIORITY_HIGH);
    }

    private void notify(int id, int icon, String title, String body, int priority) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(getDashboardIntent());
        NotificationManagerCompat.from(context).notify(id, b.build());
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private PendingIntent getDashboardIntent() {
        Intent i = new Intent(context, HomeDashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(CHANNEL_DESC);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private String getCurrentMonthKey() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-"
                + String.format(java.util.Locale.US, "%02d",
                cal.get(java.util.Calendar.MONTH) + 1);
    }
}
