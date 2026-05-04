package com.spendlens.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendlens.app.entities.UserProfile;
import com.spendlens.app.repository.UserProfileRepository;

public class UserProfileViewModel extends AndroidViewModel {

    private final UserProfileRepository repository;
    private final LiveData<UserProfile> profile;

    public UserProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new UserProfileRepository(application);
        profile = repository.getProfile();
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public void saveProfile(UserProfile p) {
        repository.saveProfile(p);
    }

    public void updateProfile(UserProfile p) {
        repository.updateProfile(p);
    }

    public boolean profileExists() {
        return repository.profileExists();
    }

    /**
     * Update only the budget — called from SettingsActivity.
     * Fetches current profile on background thread, patches budget, saves back.
     */
    public void updateBudget(double newMonthlyBudget) {
        com.spendlens.app.database.AppDatabase.dbExecutor.execute(() -> {
            UserProfile current = repository.getProfileSync();
            if (current != null) {
                current.monthlyBudget = newMonthlyBudget;
                repository.updateProfile(current);
            }
        });
    }

    /**
     * Update currency symbol — also refreshes PrefsManager cache so
     * Dashboard reads it instantly without a DB round-trip.
     */
    public void updateCurrency(String currency) {
        com.spendlens.app.database.AppDatabase.dbExecutor.execute(() -> {
            UserProfile current = repository.getProfileSync();
            if (current != null) {
                current.currency = currency;
                repository.updateProfile(current);
                com.spendlens.app.utils.PrefsManager
                        .getInstance(getApplication())
                        .setCurrency(currency);
            }
        });
    }

    /** Full reset — called by Settings > Reset App Data */
    public void deleteProfile() {
        repository.deleteAll();
    }
}
