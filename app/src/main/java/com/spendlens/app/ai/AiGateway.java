package com.spendlens.app.ai;

import android.content.Context;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.util.List;

public class AiGateway implements AutoCloseable {

    private final Context context;
    private final AiConfig config;
    private LlmInference inference;

    public AiGateway(Context context, AiConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
    }

    public synchronized String generateText(String systemPrompt,
                                            List<AiChatMessage> history,
                                            String userPrompt,
                                            double temperature,
                                            int maxTokens) throws AiServiceException {
        if (!config.isConfigured()) {
            throw new AiServiceException("Load a compatible on-device model first.");
        }

        try {
            LlmInference llm = ensureInference(maxTokens);
            String prompt = buildPrompt(systemPrompt, history, userPrompt);
            String response = llm.generateResponse(prompt);
            if (response == null || response.trim().isEmpty()) {
                throw new AiServiceException("The on-device model returned an empty response.");
            }
            return cleanResponse(response);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException(
                    "Could not run the on-device model. Re-import a compatible .task model in AI settings.",
                    e
            );
        }
    }

    private LlmInference ensureInference(int maxTokens) throws AiServiceException {
        if (inference != null) {
            return inference;
        }

        try {
            String resolvedModelPath = AiModelStore.resolveModelPath(context, config);
            LlmInference.LlmInferenceOptions.Builder builder = LlmInference.LlmInferenceOptions
                    .builder()
                    .setModelPath(resolvedModelPath)
                    .setMaxTokens(Math.max(1024, Math.min(maxTokens, 4096)))
                    .setMaxTopK(32);

            inference = LlmInference.createFromOptions(context, builder.build());
            return inference;
        } catch (Exception e) {
            throw new AiServiceException("Could not load the on-device AI model.", e);
        }
    }

    private String buildPrompt(String systemPrompt,
                               List<AiChatMessage> history,
                               String userPrompt) {
        StringBuilder builder = new StringBuilder();

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            builder.append(systemPrompt.trim()).append("\n\n");
        }

        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 8);
            for (int i = start; i < history.size(); i++) {
                AiChatMessage message = history.get(i);
                if (message == null || message.content == null || message.content.trim().isEmpty()) {
                    continue;
                }
                String role = message.role != null ? message.role.trim().toLowerCase() : "user";
                builder.append("assistant".equals(role) ? "A: " : "Q: ")
                        .append(message.content.trim())
                        .append("\n");
            }
            builder.append("\n");
        }

        builder.append("Q: ")
                .append(userPrompt != null ? userPrompt.trim() : "")
                .append("\nA:");

        return builder.toString();
    }

    private String cleanResponse(String raw) {
        if (raw == null) return "";
        String cleaned = raw;

        // Strip HTML-like tags and common model artifacts
        cleaned = cleaned.replaceAll("</?completion>", "");
        cleaned = cleaned.replaceAll("<[a-zA-Z_/][^>]{0,40}>", "");

        // If the model echoed the prompt back, take only the part after the last "A:"
        int lastAMarker = cleaned.lastIndexOf("\nA:");
        if (lastAMarker >= 0) {
            cleaned = cleaned.substring(lastAMarker + 3);
        }

        // Strip lines that look like echoed data or instructions
        String[] lines = cleaned.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String l = line.trim();
            if (l.startsWith("Budget data:")) continue;
            if (l.startsWith("Q:")) continue;
            if (l.isEmpty() && result.length() == 0) continue;
            result.append(l).append("\n");
        }

        cleaned = result.toString().trim();
        if (cleaned.isEmpty()) return raw.trim();
        return cleaned;
    }

    @Override
    public synchronized void close() {
        if (inference != null) {
            inference.close();
            inference = null;
        }
    }
}
