package com.spendlens.app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendlens.app.entities.UserProfile;

@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);

    @Update
    void update(UserProfile profile);

    // userId is always 1 for this single-user app
    @Query("SELECT * FROM user_profile WHERE userId = 1 LIMIT 1")
    LiveData<UserProfile> getProfile();

    @Query("SELECT * FROM user_profile WHERE userId = 1 LIMIT 1")
    UserProfile getProfileSync();

    @Query("SELECT COUNT(*) FROM user_profile")
    int getCount();

    @Query("DELETE FROM user_profile")
    void deleteAll();
}
