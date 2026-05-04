package com.spendlens.app.ai;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinanceScopeGuardTest {

    @Test
    public void acceptsBudgetQuestions() {
        assertTrue(FinanceScopeGuard.isFinanceQuestion(
                "How can I cut back my spending this month?"
        ));
    }

    @Test
    public void rejectsOffTopicQuestions() {
        assertFalse(FinanceScopeGuard.isFinanceQuestion(
                "Write me a sci-fi story about Mars."
        ));
    }
}
