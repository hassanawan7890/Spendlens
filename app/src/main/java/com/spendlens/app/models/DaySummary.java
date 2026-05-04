package com.spendlens.app.models;

// Result class for daily spending summary (Weekly Reflection: highest day)
public class DaySummary {
    public long dayBucket;    // date / 86400000 — multiply back to get day start
    public double totalAmount;
    public int count;

    public long getDayStartTimestamp() {
        return dayBucket * 86400000L;
    }
}
