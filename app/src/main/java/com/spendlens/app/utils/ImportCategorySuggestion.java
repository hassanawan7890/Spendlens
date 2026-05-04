package com.spendlens.app.utils;

public class ImportCategorySuggestion {
    public final int categoryId;
    public final String categoryName;
    public final String sourceLabel;
    public final String confidenceLabel;
    public final String reason;
    public final boolean autoCategorized;

    public ImportCategorySuggestion(int categoryId,
                                    String categoryName,
                                    String sourceLabel,
                                    String confidenceLabel,
                                    String reason,
                                    boolean autoCategorized) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.sourceLabel = sourceLabel;
        this.confidenceLabel = confidenceLabel;
        this.reason = reason;
        this.autoCategorized = autoCategorized;
    }
}
