package com.spendlens.app.utils;

import com.spendlens.app.entities.Category;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KeywordCategorizer {

    private static final String FOOD = "Food & Drinks";
    private static final String TRANSPORT = "Transport";
    private static final String LEISURE = "Leisure";
    private static final String SUBSCRIPTIONS = "Subscriptions";
    private static final String HEALTH = "Health & Medical";
    private static final String EDUCATION = "Education";
    private static final String SHOPPING = "Shopping";

    private static final Map<String, String> RULES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<>();

    static {
        CATEGORY_ICONS.put(FOOD, "ic_food");
        CATEGORY_ICONS.put(TRANSPORT, "ic_transport");
        CATEGORY_ICONS.put(LEISURE, "ic_leisure");
        CATEGORY_ICONS.put(SUBSCRIPTIONS, "ic_subscription");
        CATEGORY_ICONS.put(HEALTH, "ic_health");
        CATEGORY_ICONS.put(EDUCATION, "ic_education");
        CATEGORY_ICONS.put(SHOPPING, "ic_shopping");
        CATEGORY_ICONS.put(CategoryDisplayUtils.DEFAULT_CATEGORY_NAME, CategoryDisplayUtils.DEFAULT_ICON_NAME);

        String[] food = {"tim hortons","timhortons","tims","starbucks","mcdonald","mcdonalds","burger king","subway","wendy","pizza hut","domino","a&w","harvey","dairy queen","popeyes","chipotle","five guys","freshii","booster juice","restaurant","cafe","coffee","donut","bakery","sushi","pizza","food","kitchen","grille","grill","diner","eatery","uber eats","doordash","skip the dishes","skipthedishes"};
        for (String keyword : food) RULES.put(keyword, FOOD);

        String[] trans = {"uber","lyft","taxi","presto","ttc","oc transpo","translink","via rail","go transit","greyhound","parking","impark","greenp","enterprise","budget rent","avis","hertz","petro","shell","esso","canadian tire gas","sunoco","husky","fuel","gas station","autoroute","407 etr"};
        for (String keyword : trans) RULES.put(keyword, TRANSPORT);

        String[] leis = {"steam","playstation","xbox","nintendo","cineplex","landmark cinema","cinema","movie","theatre","theater","ticketmaster","eventbrite","bar ","pub ","lounge","nightclub","bowling","escape room","arcade","golf","fitness","goodlife","planet fitness","ymca","spa","salon"};
        for (String keyword : leis) RULES.put(keyword, LEISURE);

        String[] subs = {"netflix","spotify","apple.com/bill","itunes","google play","youtube premium","disney+","disney plus","crave","amazon prime","hbo","paramount","microsoft 365","adobe","dropbox","icloud","1password","duolingo","headspace","calm"};
        for (String keyword : subs) RULES.put(keyword, SUBSCRIPTIONS);

        String[] health = {"shoppers drug","london drugs","pharmasave","rexall","pharmacy","medical","clinic","hospital","dentist","dental","optometry","physio","chiropractic","lab test","lifelabs"};
        for (String keyword : health) RULES.put(keyword, HEALTH);

        String[] edu = {"university","college","tuition","amazon books","indigo","chapters","mcmaster","uoft","udemy","coursera","skillshare","pluralsight"};
        for (String keyword : edu) RULES.put(keyword, EDUCATION);

        String[] shop = {"walmart","costco","canadian tire","home depot","ikea","winners","homesense","marshalls","zara","h&m","uniqlo","old navy","gap","roots","lululemon","sport chek","atmosphere","amazon","ebay","best buy","staples","dollarama","dollar tree","shopify","etsy","loblaws","no frills","food basics","freshco","sobeys","metro ","real canadian","valumart","t&t","whole foods","trader joe"};
        for (String keyword : shop) RULES.put(keyword, SHOPPING);
    }

    private KeywordCategorizer() {}

    public static int categorize(String description) {
        return categorize(description, null);
    }

    public static int categorize(String description, List<Category> categories) {
        ImportCategorySuggestion suggestion = categorizeDetailed(description, categories);
        return suggestion != null ? suggestion.categoryId : 8;
    }

    public static ImportCategorySuggestion categorizeDetailed(String description) {
        return categorizeDetailed(description, null);
    }

    public static ImportCategorySuggestion categorizeDetailed(String description, List<Category> categories) {
        if (description == null || description.trim().isEmpty()) return null;

        String lower = description.toLowerCase(Locale.US);
        String bestCategoryName = null;
        int bestScore = 0;
        String bestKeyword = null;

        for (Map.Entry<String, String> rule : RULES.entrySet()) {
            if (!lower.contains(rule.getKey())) continue;

            int score = getScore(rule.getKey());
            if (score > bestScore) {
                bestScore = score;
                bestCategoryName = rule.getValue();
                bestKeyword = rule.getKey().trim();
            }
        }

        if (bestCategoryName == null) return null;

        String confidence;
        if (bestScore >= 5) {
            confidence = "High confidence";
        } else if (bestScore >= 3) {
            confidence = "Medium confidence";
        } else {
            confidence = "Low confidence";
        }

        return new ImportCategorySuggestion(
                resolveCategoryId(bestCategoryName, categories),
                resolveCategoryName(bestCategoryName, categories),
                "Rules",
                confidence,
                "Matched keyword: \"" + bestKeyword + "\".",
                true
        );
    }

    public static int resolveCategoryId(String categoryName, List<Category> categories) {
        Category category = findBestCategory(categoryName, categories);
        if (category != null) return category.categoryId;
        return fallbackCategoryIdForName(categoryName);
    }

    public static String resolveCategoryName(String categoryName, List<Category> categories) {
        Category category = findBestCategory(categoryName, categories);
        if (category != null && category.categoryName != null && !category.categoryName.trim().isEmpty()) {
            return category.categoryName;
        }
        return categoryName != null ? categoryName : CategoryDisplayUtils.DEFAULT_CATEGORY_NAME;
    }

    private static int getScore(String keyword) {
        if (keyword.contains(" ")) return 5;
        if (keyword.length() >= 7) return 4;
        if (keyword.length() >= 5) return 3;
        return 2;
    }

    private static Category findBestCategory(String categoryName, List<Category> categories) {
        if (categories == null || categories.isEmpty()) return null;

        if (categoryName == null) {
            return findOthersCategory(categories);
        }

        String expectedIcon = CATEGORY_ICONS.get(categoryName);
        if (expectedIcon != null) {
            for (Category category : categories) {
                if (expectedIcon.equals(category.iconName)) {
                    return category;
                }
            }
        }

        for (Category category : categories) {
            if (category.categoryName != null
                    && category.categoryName.equalsIgnoreCase(categoryName)) {
                return category;
            }
        }

        if (CategoryDisplayUtils.DEFAULT_CATEGORY_NAME.equalsIgnoreCase(categoryName)) {
            return findOthersCategory(categories);
        }

        return null;
    }

    private static Category findOthersCategory(List<Category> categories) {
        for (Category category : categories) {
            if (CategoryDisplayUtils.DEFAULT_ICON_NAME.equals(category.iconName)) {
                return category;
            }
        }

        for (Category category : categories) {
            if (category.categoryName != null
                    && category.categoryName.equalsIgnoreCase(CategoryDisplayUtils.DEFAULT_CATEGORY_NAME)) {
                return category;
            }
        }

        return categories.isEmpty() ? null : categories.get(0);
    }

    private static int fallbackCategoryIdForName(String categoryName) {
        if (categoryName == null) return 8;

        switch (categoryName) {
            case FOOD: return 1;
            case TRANSPORT: return 2;
            case LEISURE: return 3;
            case SUBSCRIPTIONS: return 4;
            case HEALTH: return 5;
            case EDUCATION: return 6;
            case SHOPPING: return 7;
            default: return 8;
        }
    }

    public static String getCategoryName(int id) {
        switch (id) {
            case 1: return FOOD;
            case 2: return TRANSPORT;
            case 3: return LEISURE;
            case 4: return SUBSCRIPTIONS;
            case 5: return HEALTH;
            case 6: return EDUCATION;
            case 7: return SHOPPING;
            default: return CategoryDisplayUtils.DEFAULT_CATEGORY_NAME;
        }
    }
}
