package com.spendlens.app.widget;

import android.content.Context;
import android.content.Intent;

/**
 * WidgetRefreshHelper
 *
 * Call WidgetRefreshHelper.refresh(context) from AddExpenseActivity
 * after successfully saving an expense. This triggers an immediate
 * widget update on the home screen.
 *
 * Usage in AddExpenseActivity.saveExpense():
 *     WidgetRefreshHelper.refresh(this);
 */
public class WidgetRefreshHelper {
    public static void refresh(Context context) {
        Intent intent = new Intent(SpendLensWidget.ACTION_WIDGET_UPDATE);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }
}