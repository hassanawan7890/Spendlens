package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.databinding.ActivitySetupProfileBinding;
import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.utils.ValidationUtils;
import com.spendlens.app.viewmodels.UserProfileViewModel;

public class SetupProfileActivity extends AppCompatActivity {

    private ActivitySetupProfileBinding binding;
    private UserProfileViewModel viewModel;
    private PrefsManager prefs;

    private String selectedCurrency = "RM";

    private static final String[] CURRENCY_SYMBOLS = {
            "RM", "$", "€", "£", "¥", "SGD", "IDR", "THB", "CAD"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        prefs = PrefsManager.getInstance(this);

        setupCurrencySpinner();
        setupKeyboardBehavior();
        setupClickListeners();
    }

    private void setupCurrencySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.currency_names, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCurrency.setAdapter(adapter);

        binding.spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCurrency = CURRENCY_SYMBOLS[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupKeyboardBehavior() {
        binding.editBudget.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSave();
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        binding.btnContinue.setOnClickListener(v -> attemptSave());
    }

    private void attemptSave() {
        String name      = getText(binding.editName);
        String budgetStr = getText(binding.editBudget);

        boolean nameOk = ValidationUtils.requireNonEmpty(
                binding.layoutName, name, getString(R.string.error_name_required));

        boolean budgetOk = ValidationUtils.requirePositiveAmount(
                binding.layoutBudget, budgetStr,
                getString(R.string.error_budget_required),
                getString(R.string.error_budget_invalid));

        if (!nameOk || !budgetOk) return;

        double budget = CurrencyUtils.parse(budgetStr);
        saveAndProceed(name.trim(), budget);
    }

    private void saveAndProceed(String name, double budget) {
        binding.btnContinue.setEnabled(false);

        UserProfile profile = new UserProfile(name, budget, 0.0, selectedCurrency);

        // Write directly on the DB executor thread, then navigate on the
        // main thread ONLY after Room has confirmed the write is complete.
        // This prevents the race condition where the process dies before
        // the async insert finishes.
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase.getInstance(this).userProfileDao().insert(profile);

            // Cache lightweight values in SharedPrefs
            prefs.setUserName(name);
            prefs.setCurrency(selectedCurrency);
            prefs.setOnboardingDone(true);

            // Navigate on main thread after DB write is confirmed
            runOnUiThread(() -> {
                Intent intent = new Intent(this, HomeDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right);
            });
        });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString() : "";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}