package com.spendlens.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.spendlens.app.R;
import com.spendlens.app.activities.HomeDashboardActivity;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.PrefsManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SpendLensWidget extends AppWidgetProvider {

    public static final String ACTION_WIDGET_UPDATE =
            "com.spendlens.app.widget.ACTION_WIDGET_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, manager, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_WIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName component  = new ComponentName(context, SpendLensWidget.class);
            int[] ids = manager.getAppWidgetIds(component);
            for (int id : ids) {
                updateWidget(context, manager, id);
            }
        }
    }

    public static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        AppDatabase.dbExecutor.execute(() -> {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(), R.layout.widget_spendlens);

            try {
                // Month start timestamp
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long monthStart = cal.getTimeInMillis();

                // Today start timestamp
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);
                long todayStart = todayCal.getTimeInMillis();
                long now        = System.currentTimeMillis();

                // Fetch totals using existing sync methods
                AppDatabase db       = AppDatabase.getInstance(context);
                double monthSpent    = db.expenseDao().getTotalBetweenSync(monthStart, now);
                double todaySpent    = db.expenseDao().getTotalBetweenSync(todayStart, now);
                UserProfile profile  = db.userProfileDao().getProfileSync();

                double budget   = (profile != null) ? profile.monthlyBudget : 0;
                String currency = (profile != null && profile.currency != null)
                        ? profile.currency
                        : PrefsManager.getInstance(context).getCurrency();

                double remaining = budget - monthSpent;
                String todayStr   = CurrencyUtils.formatSmart(currency, todaySpent);
                String remainStr  = (remaining >= 0)
                        ? CurrencyUtils.formatSmart(currency, remaining) + " left"
                        : "-" + CurrencyUtils.formatSmart(currency, Math.abs(remaining)) + " over";
                String budgetStr  = "of " + CurrencyUtils.formatSmart(currency, budget);
                String timeStr    = "Updated " +
                        new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());

                views.setTextViewText(R.id.tvWidgetToday,   todayStr);
                views.setTextViewText(R.id.tvWidgetRemain,  remainStr);
                views.setTextViewText(R.id.tvWidgetBudget,  budgetStr);
                views.setTextViewText(R.id.tvWidgetUpdated, timeStr);
                views.setTextColor(R.id.tvWidgetRemain,
                        remaining < 0 ? 0xFFFF453A : 0xFFF5C842);

            } catch (Exception e) {
                // Safe fallback — never show "Can't load widget"
                views.setTextViewText(R.id.tvWidgetToday,   "Tap to open");
                views.setTextViewText(R.id.tvWidgetRemain,  "SpendLens");
                views.setTextViewText(R.id.tvWidgetBudget,  "");
                views.setTextViewText(R.id.tvWidgetUpdated, e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 40))
                        : "error");
            }

            // Always set tap intent — even on error
            try {
                Intent launch = new Intent(context, HomeDashboardActivity.class);
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widgetRoot, pi);
            } catch (Exception ignored) {}

            manager.updateAppWidget(widgetId, views);
        });
    }
}