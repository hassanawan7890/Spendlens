package com.spendlens.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.spendlens.app.dao.CategoryDao;
import com.spendlens.app.dao.CategorySnapshotDao;
import com.spendlens.app.dao.ExpenseDao;
import com.spendlens.app.dao.ImportedTransactionDao;
import com.spendlens.app.dao.MonthlySnapshotDao;
import com.spendlens.app.dao.StatementImportDao;
import com.spendlens.app.dao.UserProfileDao;
import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.CategorySnapshot;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.entities.ImportedTransaction;
import com.spendlens.app.entities.MonthlySnapshot;
import com.spendlens.app.entities.StatementImport;
import com.spendlens.app.entities.UserProfile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                Expense.class,
                Category.class,
                UserProfile.class,
                MonthlySnapshot.class,
                CategorySnapshot.class,
                StatementImport.class,
                ImportedTransaction.class
        },
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ExpenseDao expenseDao();
    public abstract CategoryDao categoryDao();
    public abstract UserProfileDao userProfileDao();
    public abstract MonthlySnapshotDao monthlySnapshotDao();
    public abstract CategorySnapshotDao categorySnapshotDao();
    public abstract StatementImportDao statementImportDao();
    public abstract ImportedTransactionDao importedTransactionDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `monthly_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,`month` INTEGER NOT NULL,`year` INTEGER NOT NULL,`plannedBudget` REAL NOT NULL,`totalSpent` REAL NOT NULL,`totalIncome` REAL NOT NULL,`savings` REAL NOT NULL,`createdAt` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `category_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,`snapshotId` INTEGER NOT NULL,`categoryName` TEXT,`iconName` TEXT,`totalSpent` REAL NOT NULL,`percentage` REAL NOT NULL,`transactionCount` INTEGER NOT NULL,FOREIGN KEY(`snapshotId`) REFERENCES `monthly_snapshots`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_snapshots_snapshotId` ON `category_snapshots` (`snapshotId`)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `statement_imports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,`fileName` TEXT,`month` INTEGER NOT NULL,`year` INTEGER NOT NULL,`importedAt` INTEGER NOT NULL,`sourceType` TEXT,`totalTransactions` INTEGER NOT NULL,`addedToExpenses` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `imported_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,`statementId` INTEGER NOT NULL,`date` INTEGER NOT NULL,`description` TEXT,`amount` REAL NOT NULL,`type` TEXT,`categoryId` INTEGER NOT NULL,`categoryName` TEXT,`isAutoCategorized` INTEGER NOT NULL,`addToExpenses` INTEGER NOT NULL,FOREIGN KEY(`statementId`) REFERENCES `statement_imports`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_imported_transactions_statementId` ON `imported_transactions` (`statementId`)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `imported_transactions` ADD COLUMN `suggestionSource` TEXT");
            db.execSQL("ALTER TABLE `imported_transactions` ADD COLUMN `confidenceLabel` TEXT");
            db.execSQL("ALTER TABLE `imported_transactions` ADD COLUMN `matchReason` TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "spendlens_db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    dbExecutor.execute(() -> {
                                        AppDatabase instance = INSTANCE;
                                        if (instance != null) {
                                            DatabaseSeeder.seedCategories(instance.categoryDao());
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
