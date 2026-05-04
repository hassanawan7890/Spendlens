package com.spendlens.app.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendlens.app.entities.Expense;
import com.spendlens.app.models.CategorySummary;
import com.spendlens.app.models.DaySummary;
import com.spendlens.app.models.MoodSummary;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("DELETE FROM expenses")
    void deleteAll();

    @Query("UPDATE expenses SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    void reassignCategory(int oldCategoryId, int newCategoryId);

    @Query("SELECT * FROM expenses WHERE expenseId = :id")
    LiveData<Expense> getById(int id);

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses();

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    List<Expense> getAllExpensesSync();

    @Query("SELECT * FROM expenses WHERE title LIKE '%' || :query || '%' ORDER BY date DESC")
    LiveData<List<Expense>> searchExpenses(String query);

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    LiveData<List<Expense>> getByCategory(int categoryId);

    @Query("SELECT * FROM expenses WHERE moodTag = :mood ORDER BY date DESC")
    LiveData<List<Expense>> getByMood(String mood);

    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date BETWEEN :from AND :to")
    LiveData<Double> getTotalBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date BETWEEN :from AND :to")
    double getTotalBetweenSync(long from, long to);

    @Query("SELECT c.categoryName, c.iconName, SUM(e.amount) AS totalAmount, COUNT(e.expenseId) AS count " +
            "FROM expenses e " +
            "INNER JOIN categories c ON e.categoryId = c.categoryId " +
            "WHERE e.date BETWEEN :from AND :to " +
            "GROUP BY e.categoryId " +
            "ORDER BY totalAmount DESC")
    LiveData<List<CategorySummary>> getCategorySummary(long from, long to);

    @Query("SELECT c.categoryName, c.iconName, SUM(e.amount) AS totalAmount, COUNT(e.expenseId) AS count " +
            "FROM expenses e " +
            "INNER JOIN categories c ON e.categoryId = c.categoryId " +
            "WHERE e.date BETWEEN :from AND :to " +
            "GROUP BY e.categoryId " +
            "ORDER BY totalAmount DESC")
    List<CategorySummary> getCategorySummarySync(long from, long to);

    @Query("SELECT moodTag, COUNT(*) AS count, SUM(amount) AS totalAmount " +
            "FROM expenses " +
            "WHERE date BETWEEN :from AND :to " +
            "GROUP BY moodTag " +
            "ORDER BY count DESC")
    LiveData<List<MoodSummary>> getMoodSummary(long from, long to);

    @Query("SELECT c.categoryName, c.iconName, COUNT(e.expenseId) AS count, SUM(e.amount) AS totalAmount " +
            "FROM expenses e " +
            "INNER JOIN categories c ON e.categoryId = c.categoryId " +
            "WHERE e.date BETWEEN :from AND :to " +
            "GROUP BY e.categoryId " +
            "HAVING COUNT(e.expenseId) >= :minCount " +
            "ORDER BY count DESC")
    List<CategorySummary> getLeakCandidatesSync(long from, long to, int minCount);

    @Query("SELECT date / 86400000 AS dayBucket, SUM(amount) AS totalAmount, COUNT(*) AS count " +
            "FROM expenses " +
            "WHERE date BETWEEN :from AND :to " +
            "GROUP BY dayBucket " +
            "ORDER BY totalAmount DESC " +
            "LIMIT 7")
    List<DaySummary> getDailySummarySync(long from, long to);

    @Query("SELECT moodTag, COUNT(*) AS count, SUM(amount) AS totalAmount " +
            "FROM expenses " +
            "WHERE date BETWEEN :from AND :to " +
            "GROUP BY moodTag")
    List<MoodSummary> getMoodSummarySync(long from, long to);

    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :from AND :to")
    LiveData<Integer> getCountBetween(long from, long to);

    @Query("SELECT COUNT(*) FROM expenses")
    int getTotalCount();

    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    List<Expense> getExpensesBetweenSync(long from, long to);
}
