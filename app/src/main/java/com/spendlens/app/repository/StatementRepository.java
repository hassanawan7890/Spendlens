package com.spendlens.app.repository;

import android.app.Application;

import com.spendlens.app.ai.AiConfig;
import com.spendlens.app.ai.AiGateway;
import com.spendlens.app.ai.AiServiceException;
import com.spendlens.app.ai.AiStatementImportService;
import com.spendlens.app.dao.ImportedTransactionDao;
import com.spendlens.app.dao.StatementImportDao;
import com.spendlens.app.database.AppDatabase;
import com.spendlens.app.database.DatabaseSeeder;
import com.spendlens.app.entities.Category;
import com.spendlens.app.entities.Expense;
import com.spendlens.app.entities.ImportedTransaction;
import com.spendlens.app.entities.StatementImport;
import com.spendlens.app.utils.AdaptiveStatementCategorizer;
import com.spendlens.app.utils.CsvParser;
import com.spendlens.app.utils.ImportCategorySuggestion;
import com.spendlens.app.utils.PrefsManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StatementRepository {

    private final Application application;
    private final StatementImportDao importDao;
    private final ImportedTransactionDao txDao;
    private final AppDatabase db;

    public StatementRepository(Application application) {
        this.application = application;
        db = AppDatabase.getInstance(application);
        importDao = db.statementImportDao();
        txDao = db.importedTransactionDao();
    }

    public int importCsv(InputStream stream, String fileName) {
        String rawContent = readAll(stream);
        if (rawContent == null || rawContent.trim().isEmpty()) return -1;

        List<Category> categories = db.categoryDao().getAllCategoriesSync();
        if (categories == null || categories.isEmpty()) {
            DatabaseSeeder.seedCategories(db.categoryDao());
            categories = db.categoryDao().getAllCategoriesSync();
        }

        AiConfig aiConfig = PrefsManager.getInstance(application).getAiConfig();
        if (aiConfig.canUseStatementImport()) {
            try {
                return importWithAi(fileName, rawContent, categories, aiConfig);
            } catch (AiServiceException ignored) {
                // Fall back to the local parser and categorizer if the AI request fails.
            }
        }

        return importWithLocalParser(fileName, rawContent, categories);
    }

    private int importWithAi(String fileName,
                             String rawContent,
                             List<Category> categories,
                             AiConfig aiConfig) throws AiServiceException {
        try (AiGateway gateway = new AiGateway(application, aiConfig)) {
            AiStatementImportService service = new AiStatementImportService(gateway);
            CsvParser.SchemaHint hint = service.inferSchema(fileName, rawContent);
            CsvParser.ParseResult result = CsvParser.parse(rawContent, hint);
            if (result.error != null || result.transactions.isEmpty()) {
                throw new AiServiceException("On-device AI could not normalize this statement layout.");
            }

            List<Expense> expenseHistory = db.expenseDao().getAllExpensesSync();
            StatementImport statementImport = new StatementImport(
                    fileName,
                    result.detectedMonth,
                    result.detectedYear,
                    "on-device-ai",
                    result.transactions.size()
            );
            long statementId = importDao.insert(statementImport);

            List<ImportedTransaction> rows = new ArrayList<>();
            for (CsvParser.ParsedTransaction tx : result.transactions) {
                ImportCategorySuggestion suggestion = AdaptiveStatementCategorizer.categorize(
                        tx.description,
                        expenseHistory,
                        categories
                );
                rows.add(new ImportedTransaction(
                        (int) statementId,
                        tx.dateMs,
                        tx.description,
                        tx.amount,
                        tx.type,
                        suggestion.categoryId,
                        suggestion.categoryName,
                        suggestion.autoCategorized,
                        "AI",
                        suggestion.confidenceLabel,
                        buildAiReason(hint.notes, suggestion)
                ));
            }

            txDao.insertAll(rows);
            return (int) statementId;
        }
    }

    private int importWithLocalParser(String fileName,
                                      String rawContent,
                                      List<Category> categories) {
        CsvParser.ParseResult result = CsvParser.parse(rawContent);
        if (result.error != null || result.transactions.isEmpty()) return -1;

        List<Expense> expenseHistory = db.expenseDao().getAllExpensesSync();

        StatementImport statementImport = new StatementImport(
                fileName,
                result.detectedMonth,
                result.detectedYear,
                "csv",
                result.transactions.size()
        );
        long statementId = importDao.insert(statementImport);

        List<ImportedTransaction> rows = new ArrayList<>();
        for (CsvParser.ParsedTransaction tx : result.transactions) {
            ImportCategorySuggestion suggestion = AdaptiveStatementCategorizer.categorize(
                    tx.description,
                    expenseHistory,
                    categories
            );
            rows.add(new ImportedTransaction(
                    (int) statementId,
                    tx.dateMs,
                    tx.description,
                    tx.amount,
                    tx.type,
                    suggestion.categoryId,
                    suggestion.categoryName,
                    suggestion.autoCategorized,
                    suggestion.sourceLabel,
                    suggestion.confidenceLabel,
                    suggestion.reason
            ));
        }

        txDao.insertAll(rows);
        return (int) statementId;
    }

    private String readAll(InputStream stream) {
        if (stream == null) return null;

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public List<ImportedTransaction> getTransactionsForStatement(int statementId) {
        return txDao.getForStatementSync(statementId);
    }

    public void updateTransaction(ImportedTransaction tx) {
        txDao.update(tx);
    }

    public int confirmImport(int statementId) {
        List<ImportedTransaction> confirmed = txDao.getConfirmedForStatementSync(statementId);
        int addedCount = 0;

        for (ImportedTransaction tx : confirmed) {
            if (!"debit".equals(tx.type)) continue;

            Expense expense = new Expense(
                    tx.description,
                    tx.amount,
                    tx.categoryId,
                    tx.date,
                    "Imported from statement",
                    Expense.MOOD_NEED,
                    Expense.PAY_CARD
            );
            db.expenseDao().insert(expense);
            addedCount++;
        }

        StatementImport statementImport = importDao.getByIdSync(statementId);
        if (statementImport != null) {
            statementImport.addedToExpenses = addedCount;
            importDao.update(statementImport);
        }
        return addedCount;
    }

    public void deleteStatement(int statementId) {
        importDao.deleteById(statementId);
    }

    private String buildAiReason(String layoutReason, ImportCategorySuggestion suggestion) {
        StringBuilder builder = new StringBuilder();
        builder.append("On-device AI matched the bank layout");
        if (layoutReason != null && !layoutReason.trim().isEmpty()) {
            builder.append(" (").append(layoutReason.trim()).append(")");
        }
        builder.append(". ");

        if (suggestion != null && suggestion.reason != null && !suggestion.reason.trim().isEmpty()) {
            builder.append("Category suggestion: ").append(suggestion.reason.trim());
        } else {
            builder.append("Category suggestion comes from your existing SpendLens history.");
        }
        return builder.toString();
    }
}
