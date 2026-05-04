package com.spendlens.app.ai;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiStatementImportServiceTest {

    @Test
    public void extractJsonPayload_removesMarkdownFences() {
        String payload = "```json\n{\"detectedMonth\":4}\n```";

        assertEquals("{\"detectedMonth\":4}", AiStatementImportService.extractJsonPayload(payload));
    }

    @Test
    public void aiConfig_acceptsExistingLocalModel() throws Exception {
        File modelFile = File.createTempFile("gemma-test", ".task");
        modelFile.deleteOnExit();

        AiConfig config = new AiConfig(
                true,
                AiConfig.RUNTIME_MEDIAPIPE,
                modelFile.getAbsolutePath(),
                "",
                true,
                true
        );

        assertTrue(config.isConfigured());
        assertTrue(config.canUseStatementImport());
        assertTrue(config.canUseBudgetCopilot());
    }

    @Test
    public void aiConfig_requiresModelFile() {
        AiConfig config = new AiConfig(
                true,
                AiConfig.RUNTIME_MEDIAPIPE,
                "",
                "",
                true,
                true
        );

        assertFalse(config.isConfigured());
        assertFalse(config.canUseStatementImport());
        assertFalse(config.canUseBudgetCopilot());
    }
}
