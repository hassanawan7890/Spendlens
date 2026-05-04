package com.spendlens.app.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.spendlens.app.R;
import com.spendlens.app.databinding.ActivityInsightsBinding;
import com.spendlens.app.insights.LeakDetector;
import com.spendlens.app.insights.PersonalityEngine;
import com.spendlens.app.insights.ReflectionEngine;
import com.spendlens.app.insights.RiskMeter;
import com.spendlens.app.repository.ExpenseRepository;
import com.spendlens.app.repository.UserProfileRepository;
import com.spendlens.app.utils.BudgetCalculator;
import com.spendlens.app.utils.DateUtils;
import com.spendlens.app.viewmodels.UserProfileViewModel;

public class InsightsActivity extends AppCompatActivity {

    private ActivityInsightsBinding binding;
    private UserProfileViewModel profileVm;
    private ExpenseRepository expenseRepo;
    private UserProfileRepository profileRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInsightsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileVm   = new ViewModelProvider(this).get(UserProfileViewModel.class);
        expenseRepo = new ExpenseRepository(getApplication());
        profileRepo = new UserProfileRepository(getApplication());

        binding.btnBack.setOnClickListener(v -> finish());

        // Run all insight engines on background thread
        profileVm.getProfile().observe(this, profile -> {
            if (profile == null) return;

            com.spendlens.app.database.AppDatabase.dbExecutor.execute(() -> {
                // 1. Leak detector
                String leakText = LeakDetector.detect(expenseRepo, profile.currency);

                // 2. Risk meter
                double spent = expenseRepo.getTotalThisMonthSync();
                int risk = BudgetCalculator.getAdjustedRiskLevel(
                        profile.monthlyBudget, spent,
                        DateUtils.getDaysElapsedThisMonth(), DateUtils.getTotalDaysThisMonth());
                String riskText = RiskMeter.getDescription(risk, profile.monthlyBudget, spent, profile.currency);

                // 3. Personality
                String[] personality = PersonalityEngine.classify(expenseRepo);

                // 4. Reflection
                String reflection = ReflectionEngine.generate(expenseRepo, profile.currency);

                // Update UI on main thread
                runOnUiThread(() -> {
                    binding.tvLeakResult.setText(leakText);
                    binding.tvRiskResult.setText(riskText);
                    highlightRiskSegment(risk);
                    binding.tvPersonalityName.setText(personality[0]);
                    binding.tvPersonalityDesc.setText(personality[1]);
                    binding.tvReflectionContent.setText(reflection);
                });
            });
        });
    }

    private void highlightRiskSegment(int risk) {
        int active   = getColor(R.color.green_600);
        int inactive = getColor(R.color.gray_100);
        int amber    = getColor(R.color.amber_400);
        int amberD   = getColor(R.color.amber_600);
        int red      = getColor(R.color.red_600);

        // Always light up Safe
        binding.riskSeg0.setBackgroundColor(active);
        binding.riskSeg1.setBackgroundColor(risk >= BudgetCalculator.RISK_MODERATE ? amber   : inactive);
        binding.riskSeg2.setBackgroundColor(risk >= BudgetCalculator.RISK_WARNING  ? amberD  : inactive);
        binding.riskSeg3.setBackgroundColor(risk >= BudgetCalculator.RISK_CRITICAL ? red     : inactive);
    }
}