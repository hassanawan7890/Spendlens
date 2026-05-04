package com.spendlens.app.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "expenses",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "categoryId",
                childColumns = "categoryId",
                onDelete = ForeignKey.SET_DEFAULT
        ),
        indices = {@Index("categoryId"), @Index("date")}
)
public class Expense {

    @PrimaryKey(autoGenerate = true)
    public int expenseId;

    public String title;          // e.g. "McDonald's"
    public double amount;         // always positive, stored in local currency
    public int categoryId;        // FK → Category
    public long date;             // Unix timestamp in milliseconds
    public String note;           // nullable, optional
    public String moodTag;        // Need / Want / Impulse / Social / Subscription / Emergency
    public String paymentMethod;  // Cash / Card / eWallet — nullable

    // ── Constructors ──────────────────────────────────────────────────────────

    public Expense() {}

    @Ignore
    public Expense(String title, double amount, int categoryId, long date,
                   String note, String moodTag, String paymentMethod) {
        this.title = title;
        this.amount = amount;
        this.categoryId = categoryId;
        this.date = date;
        this.note = note;
        this.moodTag = moodTag;
        this.paymentMethod = paymentMethod;
    }

    // ── Mood tag constants ────────────────────────────────────────────────────

    public static final String MOOD_NEED         = "Need";
    public static final String MOOD_WANT         = "Want";
    public static final String MOOD_IMPULSE      = "Impulse";
    public static final String MOOD_SOCIAL       = "Social";
    public static final String MOOD_SUBSCRIPTION = "Subscription";
    public static final String MOOD_EMERGENCY    = "Emergency";

    public static final String[] ALL_MOODS = {
            MOOD_NEED, MOOD_WANT, MOOD_IMPULSE,
            MOOD_SOCIAL, MOOD_SUBSCRIPTION, MOOD_EMERGENCY
    };

    // ── Payment method constants ──────────────────────────────────────────────

    public static final String PAY_CASH   = "Cash";
    public static final String PAY_CARD   = "Card";
    public static final String PAY_EWALLET = "eWallet";
}
