package com.spendlens.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CsvParser {

    public static class SchemaHint {
        public String delimiter = ",";
        public boolean hasHeader = true;
        public int skipLeadingRows = 0;
        public int dateColumnIndex = 0;
        public int descriptionColumnIndex = 2;
        public int amountColumnIndex = 4;
        public int debitColumnIndex = -1;
        public int creditColumnIndex = -1;
        public int typeColumnIndex = -1;
        public boolean singleAmountDebitPositive = false;
        public List<String> skipRowContains = new ArrayList<>();
        public String notes = "";
    }

    public static class ParsedTransaction {
        public long dateMs;
        public String description;
        public double amount;
        public String type;
        public int detectedMonth;
        public int detectedYear;
    }

    public static class ParseResult {
        public List<ParsedTransaction> transactions = new ArrayList<>();
        public int detectedMonth;
        public int detectedYear;
        public String error;
    }

    private static final String[] DATE_FORMATS = {
            "M/d/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MMM dd, yyyy", "MM-dd-yyyy"
    };

    public static ParseResult parse(InputStream inputStream) {
        StringBuilder raw = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                raw.append(line).append('\n');
            }
        } catch (IOException e) {
            ParseResult result = new ParseResult();
            result.error = "Failed to read file: " + e.getMessage();
            return result;
        }

        return parse(raw.toString());
    }

    public static ParseResult parse(String rawContent) {
        return parse(rawContent, null);
    }

    public static ParseResult parse(String rawContent, SchemaHint schemaHint) {
        ParseResult result = new ParseResult();

        if (rawContent == null || rawContent.trim().isEmpty()) {
            result.error = "File is empty.";
            return result;
        }

        try {
            List<String[]> rows = new ArrayList<>();
            String[] lines = rawContent.replace("\r\n", "\n").replace('\r', '\n').split("\n");
            SchemaHint resolvedHint = schemaHint != null ? schemaHint : new SchemaHint();
            char delimiter = resolveDelimiter(lines, schemaHint != null ? resolvedHint : null);
            boolean firstContentLine = true;
            int contentLineIndex = 0;
            int dateCol = resolvedHint.dateColumnIndex;
            int descCol = resolvedHint.descriptionColumnIndex;
            int amountCol = resolvedHint.amountColumnIndex;
            int debitCol = resolvedHint.debitColumnIndex;
            int creditCol = resolvedHint.creditColumnIndex;
            int typeCol = resolvedHint.typeColumnIndex;
            boolean usedSingleAmountColumn = false;
            int debitRows = 0;
            int creditRows = 0;

            for (String rawLine : lines) {
                String line = rawLine.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                if (shouldSkipLine(line, resolvedHint.skipRowContains)) continue;
                if (contentLineIndex++ < resolvedHint.skipLeadingRows) continue;

                String[] cols = splitDelimitedLine(line, delimiter);
                if (firstContentLine && resolvedHint.hasHeader) {
                    firstContentLine = false;
                    if (schemaHint == null) {
                        int[] detectedColumns = detectColumnsFromHeader(cols);
                        dateCol = detectedColumns[0];
                        descCol = detectedColumns[1];
                        amountCol = detectedColumns[2];
                        debitCol = detectedColumns[3];
                        creditCol = detectedColumns[4];
                        typeCol = detectedColumns[5];
                    }
                    continue;
                }

                firstContentLine = false;
                rows.add(cols);
            }

            Map<Integer, Integer> monthCount = new HashMap<>();
            for (String[] row : rows) {
                if (row.length < 2) continue;

                long dateMs = parseDateValue(safeGet(row, dateCol));
                if (dateMs <= 0) continue;

                String desc = safeGet(row, descCol);
                if (desc.isEmpty()) continue;

                double amount = 0;
                String type = "debit";
                if (debitCol >= 0 && creditCol >= 0) {
                    String debitValue = safeGet(row, debitCol);
                    String creditValue = safeGet(row, creditCol);
                    if (!debitValue.isEmpty()) {
                        amount = parseAmount(debitValue);
                        type = "debit";
                    } else if (!creditValue.isEmpty()) {
                        amount = parseAmount(creditValue);
                        type = "credit";
                    }
                } else {
                    usedSingleAmountColumn = true;
                    double raw = parseAmount(safeGet(row, amountCol));
                    String typeHint = safeGet(row, typeCol).toLowerCase(Locale.US);
                    if (isDebitType(typeHint)) {
                        amount = Math.abs(raw);
                        type = "debit";
                    } else if (isCreditType(typeHint)) {
                        amount = Math.abs(raw);
                        type = "credit";
                    } else if (resolvedHint.singleAmountDebitPositive) {
                        amount = Math.abs(raw);
                        type = "debit";
                    } else if (raw < 0) {
                        amount = Math.abs(raw);
                        type = "debit";
                    } else {
                        amount = raw;
                        type = "credit";
                    }
                }

                if (amount <= 0) continue;

                ParsedTransaction tx = new ParsedTransaction();
                tx.dateMs = dateMs;
                tx.description = desc.trim();
                tx.amount = amount;
                tx.type = type;

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(dateMs);
                tx.detectedMonth = cal.get(Calendar.MONTH) + 1;
                tx.detectedYear = cal.get(Calendar.YEAR);

                result.transactions.add(tx);
                monthCount.merge(tx.detectedMonth, 1, Integer::sum);
                if ("debit".equals(type)) debitRows++;
                if ("credit".equals(type)) creditRows++;
            }

            if (usedSingleAmountColumn
                    && typeCol < 0
                    && debitRows == 0
                    && creditRows > 0
                    && !resolvedHint.singleAmountDebitPositive) {
                for (ParsedTransaction tx : result.transactions) {
                    tx.type = "debit";
                }
            }

            int bestMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
            int bestYear = Calendar.getInstance().get(Calendar.YEAR);
            int bestCount = 0;
            for (ParsedTransaction tx : result.transactions) {
                int count = monthCount.getOrDefault(tx.detectedMonth, 0);
                if (count > bestCount) {
                    bestCount = count;
                    bestMonth = tx.detectedMonth;
                    bestYear = tx.detectedYear;
                }
            }
            result.detectedMonth = bestMonth;
            result.detectedYear = bestYear;
        } catch (Exception e) {
            result.error = "Failed to parse file: " + e.getMessage();
        }

        return result;
    }

    private static boolean isDebitType(String typeHint) {
        return "debit".equals(typeHint)
                || "dr".equals(typeHint)
                || "purchase".equals(typeHint)
                || "withdrawal".equals(typeHint)
                || "withdrawl".equals(typeHint)
                || "payment".equals(typeHint);
    }

    private static boolean isCreditType(String typeHint) {
        return "credit".equals(typeHint)
                || "cr".equals(typeHint)
                || "deposit".equals(typeHint)
                || "refund".equals(typeHint)
                || "income".equals(typeHint);
    }

    public static long parseDateValue(String value) {
        if (value == null || value.isEmpty()) return -1;
        value = value.replace("\"", "").trim();
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                sdf.setLenient(false);
                Date date = sdf.parse(value);
                if (date != null) return date.getTime();
            } catch (ParseException ignored) {
            }
        }
        return -1;
    }

    private static double parseAmount(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        String cleaned = raw.replace("\"", "")
                .replace("$", "")
                .replace(",", "")
                .replace(" ", "")
                .replace("\u2212", "-")
                .replace("(", "-")
                .replace(")", "")
                .trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String safeGet(String[] arr, int idx) {
        if (idx < 0 || idx >= arr.length) return "";
        return arr[idx].replace("\"", "").trim();
    }

    private static int[] detectColumnsFromHeader(String[] cols) {
        int dateCol = 0;
        int descCol = 2;
        int amountCol = 4;
        int debitCol = -1;
        int creditCol = -1;
        int typeCol = -1;

        for (int i = 0; i < cols.length; i++) {
            String header = cols[i].toLowerCase(Locale.US).replace("\"", "").trim();
            if (header.contains("date")) {
                dateCol = i;
            } else if (header.contains("description")
                    || header.contains("name")
                    || header.contains("memo")
                    || header.contains("narrative")
                    || header.contains("details")
                    || header.contains("merchant")
                    || header.contains("payee")) {
                descCol = i;
            } else if (header.equals("amount")
                    || header.equals("amt")
                    || header.contains("transaction amount")
                    || header.contains("statement amount")) {
                amountCol = i;
            } else if (header.contains("debit")
                    || header.contains("withdrawl")
                    || header.contains("withdrawal")
                    || header.contains("outflow")
                    || header.contains("charge")) {
                debitCol = i;
            } else if (header.contains("credit")
                    || header.contains("deposit")
                    || header.contains("refund")
                    || header.contains("inflow")) {
                creditCol = i;
            } else if (header.equals("type")
                    || header.contains("transaction type")
                    || header.contains("debit/credit")
                    || header.contains("dr/cr")) {
                typeCol = i;
            }
        }

        return new int[]{dateCol, descCol, amountCol, debitCol, creditCol, typeCol};
    }

    private static char resolveDelimiter(String[] lines, SchemaHint hint) {
        if (hint != null && hint.delimiter != null && !hint.delimiter.isEmpty()) {
            return "\t".equals(hint.delimiter) ? '\t' : hint.delimiter.charAt(0);
        }

        char[] candidates = new char[]{',', ';', '\t', '|'};
        int bestScore = -1;
        char bestDelimiter = ',';

        for (char candidate : candidates) {
            int score = 0;
            for (String rawLine : lines) {
                String line = rawLine != null ? rawLine.trim() : "";
                if (line.isEmpty()) continue;
                score += countDelimiter(line, candidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestDelimiter = candidate;
                }
                if (score > 8) break;
            }
        }
        return bestDelimiter;
    }

    private static int countDelimiter(String line, char delimiter) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                count++;
            }
        }
        return count;
    }

    private static boolean shouldSkipLine(String line, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return false;
        String normalized = line.toLowerCase(Locale.US);
        for (String token : tokens) {
            if (token != null && !token.trim().isEmpty() && normalized.contains(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String[] splitDelimitedLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }
}
