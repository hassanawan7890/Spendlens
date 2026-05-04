package com.spendlens.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.spendlens.app.R;
import com.spendlens.app.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewBinding generates typed bindings for <include> tags.
        // Access child views directly via the binding object — no findViewById needed.
        binding.feature1.featureText.setText(getString(R.string.welcome_feature_1));
        binding.feature2.featureText.setText(getString(R.string.welcome_feature_2));
        binding.feature3.featureText.setText(getString(R.string.welcome_feature_3));

        binding.btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupProfileActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}