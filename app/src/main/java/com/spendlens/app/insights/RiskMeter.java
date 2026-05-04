package com.spendlens.app.insights;

import com.spendlens.app.utils.BudgetCalculator;
import com.spendlens.app.utils.CurrencyUtils;

public class RiskMeter {

    public static String getDescription(int riskLevel, double budget, double spent, String currency) {
        double remaining = BudgetCalculator.getRemaining(budget, spent);
        String rem = CurrencyUtils.formatSmart(currency, remaining);
        switch (riskLevel) {
            case BudgetCalculator.RISK_CRITICAL:
                return "Critical — you have only " + rem + " left. Stop non-essential spending immediately.";
            case BudgetCalculator.RISK_WARNING:
                return "Warning — " + rem + " remaining. Limit discretionary spending for the rest of the month.";
            case BudgetCalculator.RISK_MODERATE:
                return "Moderate — " + rem + " remaining. You're on track but keep an eye on food and leisure.";
            default:
                return "Safe — " + rem + " remaining. You're managing your budget well this month.";
        }
    }
}