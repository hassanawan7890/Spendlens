package com.spendlens.app.ai;

import com.spendlens.app.utils.CsvParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiStatementImportService {

    private final AiGateway gateway;

    public AiStatementImportService(AiGateway gateway) {
        this.gateway = gateway;
    }

    public CsvParser.SchemaHint inferSchema(String fileName,
                                            String rawContent) throws AiServiceException {
        String systemPrompt = "You identify bank and card statement layouts for a budgeting app. "
                + "Return JSON only. No markdown. "
                + "Use this exact shape: "
                + "{\"delimiter\":\",\",\"hasHeader\":true,\"skipLeadingRows\":0,"
                + "\"dateColumnIndex\":0,\"descriptionColumnIndex\":1,\"amountColumnIndex\":2,"
                + "\"debitColumnIndex\":-1,\"creditColumnIndex\":-1,\"typeColumnIndex\":-1,"
                + "\"singleAmountDebitPositive\":false,"
                + "\"skipRowContains\":[\"opening balance\",\"closing balance\"],"
                + "\"notes\":\"short explanation\"}. "
                + "Rules: indexes are zero-based, use -1 for missing columns, delimiter must be one of "
                + "\", ;, tab, or |, and skipRowContains should stay lowercase and short. "
                + "Only describe the file layout. Do not output transactions.";

        String userPrompt = "File name: " + fileName + "\n"
                + "Sample rows from the statement:\n"
                + buildLayoutSample(rawContent);

        String response = gateway.generateText(systemPrompt, null, userPrompt, 0.1, 1600);
        return parseModelResponse(response);
    }

    static CsvParser.SchemaHint parseModelResponse(String rawResponse) throws AiServiceException {
        try {
            String jsonPayload = extractJsonPayload(rawResponse);
            JSONObject root = new JSONObject(jsonPayload);

            CsvParser.SchemaHint hint = new CsvParser.SchemaHint();
            hint.delimiter = normalizeDelimiter(root.optString("delimiter", ","));
            hint.hasHeader = root.optBoolean("hasHeader", true);
            hint.skipLeadingRows = Math.max(0, root.optInt("skipLeadingRows", 0));
            hint.dateColumnIndex = root.optInt("dateColumnIndex", 0);
            hint.descriptionColumnIndex = root.optInt("descriptionColumnIndex", 1);
            hint.amountColumnIndex = root.optInt("amountColumnIndex", 2);
            hint.debitColumnIndex = root.optInt("debitColumnIndex", -1);
            hint.creditColumnIndex = root.optInt("creditColumnIndex", -1);
            hint.typeColumnIndex = root.optInt("typeColumnIndex", -1);
            hint.singleAmountDebitPositive = root.optBoolean("singleAmountDebitPositive", false);
            hint.notes = root.optString(
                    "notes",
                    "On-device AI matched this statement layout before parsing the full file."
            ).trim();

            JSONArray skipRows = root.optJSONArray("skipRowContains");
            if (skipRows != null) {
                for (int i = 0; i < skipRows.length(); i++) {
                    String token = skipRows.optString(i, "").trim().toLowerCase(Locale.US);
                    if (!token.isEmpty()) {
                        hint.skipRowContains.add(token);
                    }
                }
            }

            return hint;
        } catch (Exception e) {
            throw new AiServiceException("Could not parse the on-device layout response.", e);
        }
    }

    static String extractJsonPayload(String rawResponse) {
        String trimmed = rawResponse != null ? rawResponse.trim() : "";
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence >= 0) {
                trimmed = trimmed.substring(0, closingFence);
            }
        }
        trimmed = trimmed.trim();

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim();
        }
        return trimmed;
    }

    private String buildLayoutSample(String rawContent) {
        if (rawContent == null) return "";

        String[] lines = rawContent.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder sample = new StringBuilder();
        int appended = 0;

        for (String rawLine : lines) {
            String line = rawLine.replace("\uFEFF", "").trim();
            if (line.isEmpty()) continue;

            sample.append(line).append('\n');
            appended++;
            if (appended >= 40 || sample.length() >= 4500) {
                break;
            }
        }

        return sample.toString().trim();
    }

    private static String normalizeDelimiter(String value) {
        String normalized = value != null ? value.trim().toLowerCase(Locale.US) : "";
        if (";".equals(normalized)) return ";";
        if ("|".equals(normalized)) return "|";
        if ("tab".equals(normalized) || "\\t".equals(normalized)) return "\t";
        return ",";
    }
}
