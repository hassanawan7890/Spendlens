package com.spendlens.app.activities;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.spendlens.app.R;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.databinding.ActivityEditProfileBinding;
import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.utils.CurrencyUtils;
import com.spendlens.app.utils.PrefsManager;
import com.spendlens.app.utils.ValidationUtils;
import com.spendlens.app.viewmodels.UserProfileViewModel;

import java.util.Arrays;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private UserProfileViewModel viewModel;
    private PrefsManager prefs;

    private String selectedCurrency = "RM";
    private UserProfile currentProfile;

    // Must match currency_names array in strings.xml exactly — including CAD
    private static final String[] CURRENCY_SYMBOLS = {
            "RM", "$", "€", "£", "¥", "SGD", "IDR", "THB", "CAD"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        prefs = PrefsManager.getInstance(this);

        setupCurrencySpinner();
        setupKeyboardBehavior();
        setupClickListeners();
        observeProfile();
    }

    private void observeProfile() {
        viewModel.getProfile().observe(this, profile -> {
            if (profile == null) return;
            currentProfile = profile;

            binding.editName.setText(profile.name);
            binding.editBudget.setText(CurrencyUtils.formatPlain(profile.monthlyBudget));

            // Find the saved currency in the array and select it
            // indexOf returns -1 if not found — defaults to position 0 (RM)
            int currencyIndex = Arrays.asList(CURRENCY_SYMBOLS).indexOf(profile.currency);
            if (currencyIndex >= 0) {
                binding.spinnerCurrency.setSelection(currencyIndex);
                selectedCurrency = profile.currency; // sync field with loaded value
            }
        });
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
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> attemptSave());
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

        binding.btnSave.setEnabled(false);

        if (currentProfile == null) currentProfile = new UserProfile();

        currentProfile.name          = name.trim();
        currentProfile.monthlyBudget = CurrencyUtils.parse(budgetStr);
        currentProfile.currency      = selectedCurrency;

        // Write directly on the DB executor thread — navigate only after
        // Room confirms the write is complete, preventing data loss on close
        final UserProfile profileToSave = currentProfile;
        final String currencyToCache   = selectedCurrency;
        final String nameToCache       = name.trim();

        AppDatabase.dbExecutor.execute(() -> {
            // Use insert with REPLACE so it works whether the row exists or not
            AppDatabase.getInstance(this).userProfileDao().insert(profileToSave);

            // Update SharedPrefs cache after DB write is confirmed
            prefs.setUserName(nameToCache);
            prefs.setCurrency(currencyToCache);

            runOnUiThread(() -> {
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString() : "";
    }
}