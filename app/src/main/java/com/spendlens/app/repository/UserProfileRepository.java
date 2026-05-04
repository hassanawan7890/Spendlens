package com.spendlens.app.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendlens.app.dao.UserProfileDao;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.entities.UserProfile;

public class UserProfileRepository {

    private final UserProfileDao dao;

    public UserProfileRepository(Application application) {
        dao = AppDatabase.getInstance(application).userProfileDao();
    }

    /**
     * Insert profile — uses REPLACE conflict strategy so it works as
     * both insert (first launch) and update (subsequent saves).
     * Always call this instead of updateProfile for critical saves.
     */
    public void saveProfile(UserProfile profile) {
        AppDatabase.dbExecutor.execute(() -> dao.insert(profile));
    }

    /**
     * Update profile — also uses insert with REPLACE as a safety net.
     * dao.update() silently fails if the row doesn't exist for any reason.
     * dao.insert() with OnConflictStrategy.REPLACE handles both cases.
     */
    public void updateProfile(UserProfile profile) {
        AppDatabase.dbExecutor.execute(() -> dao.insert(profile));
    }

    public LiveData<UserProfile> getProfile() {
        return dao.getProfile();
    }

    public UserProfile getProfileSync() {
        return dao.getProfileSync();
    }

    public boolean profileExists() {
        return dao.getCount() > 0;
    }

    public void deleteAll() {
        AppDatabase.dbExecutor.execute(dao::deleteAll);
    }
}