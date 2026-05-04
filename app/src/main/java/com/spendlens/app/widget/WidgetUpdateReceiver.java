package com.spendlens.app.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * WidgetUpdateReceiver
 *
 * Receives the ACTION_WIDGET_UPDATE broadcast sent by AddExpenseActivity
 * after a new expense is saved. Forces an immediate widget refresh so the
 * home screen shows the latest spending without waiting for the 30-min tick.
 */
public class WidgetUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component  = new ComponentName(context, SpendLensWidget.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            SpendLensWidget.updateWidget(context, manager, id);
        }
    }
}