package com.spendlens.app.ai;

import android.content.Context;

import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.utils.BudgetCalculator;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.DateUtils;

import java.util.List;
import java.util.Locale;

public class AiBudgetAssistantService {

    private final Context context;
    private final AiGateway gateway;

    public AiBudgetAssistantService(Context context, AiGateway gateway) {
        this.context = context.getApplicationContext();
        this.gateway = gateway;
    }

    public String ask(List<AiChatMessage> history, String question) throws AiServiceException {
        if (!FinanceScopeGuard.isFinanceQuestion(question)) {
            return FinanceScopeGuard.getRefusalMessage();
        }

        AppDatabase db = AppDatabase.getInstance(context);
        UserProfile profile = db.userProfileDao().getProfileSync();
        long[] monthRange = DateUtils.getCurrentMonthRange();
        long[] weekRange  = DateUtils.getCurrentWeekRange();
        long[] todayRange = DateUtils.getTodayRange();

        double monthlySpent  = db.expenseDao().getTotalBetweenSync(monthRange[0], monthRange[1]);
        double weeklySpent   = db.expenseDao().getTotalBetweenSync(weekRange[0],  weekRange[1]);
        double todaySpent    = db.expenseDao().getTotalBetweenSync(todayRange[0], todayRange[1]);
        List<CategorySummary> categories    = db.expenseDao().getCategorySummarySync(monthRange[0], monthRange[1]);
        List<Expense>         recentExpenses = db.expenseDao().getExpensesBetweenSync(monthRange[0], monthRange[1]);

        String currency      = profile != null && profile.currency != null ? profile.currency : "RM";
        double monthlyBudget = profile != null ? profile.monthlyBudget : 0.0;
        double weeklyBudget  = profile != null ? profile.weeklyBudget  : 0.0;
        double remaining     = monthlyBudget > 0 ? BudgetCalculator.getRemaining(monthlyBudget, monthlySpent) : 0.0;
        int    daysLeft      = DateUtils.getDaysRemainingThisMonth();

        // Answer factual questions directly from the database — fast and always accurate
        String factual = tryFactualAnswer(question, currency, monthlyBudget, weeklyBudget,
                monthlySpent, weeklySpent, todaySpent, remaining, daysLeft, categories, recentExpenses);
        if (factual != null) return factual;

        // Only reach AI for open-ended advice — keep the prompt short and focused
        String topCategory = (categories != null && !categories.isEmpty())
                ? categories.get(0).categoryName + " " + CurrencyUtils.formatSmart(currency, categories.get(0).totalAmount)
                : "no data";

        String systemData = "Budget " + CurrencyUtils.formatSmart(currency, monthlyBudget)
                + "/month, spent " + CurrencyUtils.formatSmart(currency, monthlySpent)
                + ", remaining " + CurrencyUtils.formatSmart(currency, remaining)
                + ", " + daysLeft + " days left, top category: " + topCategory + ".";

        return gateway.generateText(systemData, null, question, 0.4, 400);
    }

    private String tryFactualAnswer(String question, String currency, double monthlyBudget,
                                    double weeklyBudget, double monthlySpent, double weeklySpent,
                                    double todaySpent, double remaining, int daysLeft,
                                    List<CategorySummary> categories, List<Expense> recentExpenses) {
        String q = question.toLowerCase(Locale.US);

        // Budget amount
        if (has(q, "what is my budget", "my budget", "budget right now", "current budget", "budget amount", "budget set")) {
            String s = "Your monthly budget is " + CurrencyUtils.formatSmart(currency, monthlyBudget) + ".";
            if (weeklyBudget > 0) s += " Weekly budget: " + CurrencyUtils.formatSmart(currency, weeklyBudget) + ".";
            return s;
        }

        // Remaining / left
        if (has(q, "remaining", "how much left", "budget left", "money left", "left this month", "how much do i have")) {
            return "You have " + CurrencyUtils.formatSmart(currency, remaining)
                    + " remaining of your " + CurrencyUtils.formatSmart(currency, monthlyBudget)
                    + " monthly budget, with " + daysLeft + " days left this month.";
        }

        // Today
        if (has(q, "today", "spent today", "today spending")) {
            return "You spent " + CurrencyUtils.formatSmart(currency, todaySpent) + " today.";
        }

        // This week
        if (has(q, "this week", "week spending", "weekly spending", "spent this week")) {
            return "You spent " + CurrencyUtils.formatSmart(currency, weeklySpent) + " this week.";
        }

        // Total spent this month
        if (has(q, "how much have i spent", "how much did i spend", "total spent", "spent so far",
                "spending so far", "spend so far", "spend my budget", "spent this month", "spend on so far")) {
            StringBuilder sb = new StringBuilder("You've spent "
                    + CurrencyUtils.formatSmart(currency, monthlySpent)
                    + " this month out of your "
                    + CurrencyUtils.formatSmart(currency, monthlyBudget) + " budget.");
            if (categories != null && !categories.isEmpty()) {
                sb.append(" Top category: ").append(categories.get(0).categoryName)
                  .append(" at ").append(CurrencyUtils.formatSmart(currency, categories.get(0).totalAmount)).append(".");
            }
            return sb.toString();
        }

        // Where / categories breakdown
        if (has(q, "where", "categories", "what category", "which category", "breakdown")) {
            if (categories == null || categories.isEmpty()) {
                return "No spending categories recorded this month yet.";
            }
            StringBuilder sb = new StringBuilder("This month you spent "
                    + CurrencyUtils.formatSmart(currency, monthlySpent) + " across: ");
            int limit = Math.min(4, categories.size());
            for (int i = 0; i < limit; i++) {
                CategorySummary c = categories.get(i);
                sb.append(c.categoryName).append(" (")
                  .append(CurrencyUtils.formatSmart(currency, c.totalAmount)).append(")");
                if (i < limit - 1) sb.append(", ");
            }
            sb.append(".");
            return sb.toString();
        }

        // Expenses / transactions list
        if (has(q, "my expenses", "my transactions", "what did i buy", "what have i bought",
                "recent expenses", "list of expenses", "show expenses", "what are my expenses")) {
            if (recentExpenses == null || recentExpenses.isEmpty()) {
                return "No expenses recorded this month yet.";
            }
            StringBuilder sb = new StringBuilder("Your expenses this month:\n");
            int limit = Math.min(6, recentExpenses.size());
            for (int i = 0; i < limit; i++) {
                Expense e = recentExpenses.get(i);
                sb.append("• ").append(e.title)
                  .append(": ").append(CurrencyUtils.formatSmart(currency, e.amount)).append("\n");
            }
            return sb.toString().trim();
        }

        // Financial condition / overview
        if (has(q, "condition", "status", "overview", "financial situation", "how am i doing",
                "am i on track", "financial condition", "doing financially")) {
            double pct  = monthlyBudget > 0 ? (monthlySpent / monthlyBudget) * 100 : 0;
            String pace = pct >= 90 ? "over budget" : pct >= 70 ? "spending is high" : pct >= 40 ? "moderate pace" : "on track";
            return String.format(Locale.US,
                    "You've used %.0f%% of your %s budget — spent %s, %s remaining, %d days left. Pace: %s.",
                    pct,
                    CurrencyUtils.formatSmart(currency, monthlyBudget),
                    CurrencyUtils.formatSmart(currency, monthlySpent),
                    CurrencyUtils.formatSmart(currency, remaining),
                    daysLeft,
                    pace);
        }

        return null; // Let AI handle open-ended advice
    }

    private boolean has(String question, String... keywords) {
        for (String kw : keywords) {
            if (question.contains(kw)) return true;
        }
        return false;
    }
}
