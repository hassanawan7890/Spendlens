package com.spendlens.app.utils;

public final class CategoryDisplayUtils {

    public static final String DEFAULT_CATEGORY_NAME = "Others";
    public static final String DEFAULT_ICON_NAME = "ic_others";

    private CategoryDisplayUtils() {}

    public static String getEmojiForIconName(String iconName) {
        if (iconName == null) return "\uD83D\uDCE6";

        switch (iconName) {
            case "ic_food":         return "\uD83C\uDF54";
            case "ic_transport":    return "\uD83D\uDE8C";
            case "ic_leisure":      return "\uD83C\uDFAE";
            case "ic_subscription": return "\uD83D\uDD04";
            case "ic_health":       return "\uD83D\uDC8A";
            case "ic_education":    return "\uD83D\uDCDA";
            case "ic_shopping":     return "\uD83D\uDED2";
            default:                return "\uD83D\uDCE6";
        }
    }
}
