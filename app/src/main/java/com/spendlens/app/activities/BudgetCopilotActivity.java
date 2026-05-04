package com.spendlens.app.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.spendlens.app.R;
import com.spendlens.app.ai.AiBudgetAssistantService;
import com.spendlens.app.ai.AiChatMessage;
import com.spendlens.app.ai.AiConfig;
import com.spendlens.app.ai.AiGateway;
import com.spendlens.app.ai.AiServiceException;
import com.spendlens.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.List;

public class BudgetCopilotActivity extends AppCompatActivity {

    private final List<AiChatMessage> history = new ArrayList<>();

    private PrefsManager prefs;
    private AiGateway gateway;
    private AiBudgetAssistantService assistantService;
    private LinearLayout conversationContainer;
    private ScrollView scrollConversation;
    private EditText editQuestion;
    private MaterialButton btnSend;
    private MaterialButton btnPromptOverspending;
    private MaterialButton btnPromptSave;
    private MaterialButton btnPromptCut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_copilot);

        prefs = PrefsManager.getInstance(this);
        conversationContainer = findViewById(R.id.conversationContainer);
        scrollConversation = findViewById(R.id.scrollConversation);
        editQuestion = findViewById(R.id.editCopilotQuestion);
        btnSend = findViewById(R.id.btnSendQuestion);
        btnPromptOverspending = findViewById(R.id.btnPromptOverspending);
        btnPromptSave = findViewById(R.id.btnPromptSave);
        btnPromptCut = findViewById(R.id.btnPromptCut);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendCurrentQuestion());
        btnPromptOverspending.setOnClickListener(v ->
                askPreset("Where am I overspending this month?"));
        btnPromptSave.setOnClickListener(v ->
                askPreset("How can I still save money before this month ends?"));
        btnPromptCut.setOnClickListener(v ->
                askPreset("If I need to cut back quickly, what should I reduce first?"));

        AiConfig config = prefs.getAiConfig();
        if (!config.canUseBudgetCopilot()) {
            Toast.makeText(this, "Load your on-device AI model first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        gateway = new AiGateway(this, config);
        assistantService = new AiBudgetAssistantService(this, gateway);

        addMessageBubble(
                "assistant",
                "Ask me about your budget, spending habits, or where to cut back. I run on-device and stay focused on your money data."
        );
    }

    private void askPreset(String question) {
        editQuestion.setText(question);
        editQuestion.setSelection(question.length());
        sendCurrentQuestion();
    }

    private void sendCurrentQuestion() {
        String question = editQuestion.getText() != null
                ? editQuestion.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(question)) return;

        addMessageBubble("user", question);
        editQuestion.setText("");
        btnSend.setEnabled(false);

        final TextView thinkingBubble = addMessageBubble("assistant", "Thinking...");
        new Thread(() -> {
            try {
                String answer = assistantService.ask(history, question);

                history.add(new AiChatMessage("user", question));
                history.add(new AiChatMessage("assistant", answer));

                runOnUiThread(() -> {
                    thinkingBubble.setText(answer);
                    scrollToBottom();
                });
            } catch (AiServiceException e) {
                runOnUiThread(() -> {
                    thinkingBubble.setText("I could not start the on-device model. Re-open AI settings and load a compatible .task model.");
                    scrollToBottom();
                });
            } finally {
                runOnUiThread(() -> btnSend.setEnabled(true));
            }
        }).start();
    }

    private TextView addMessageBubble(String role, String message) {
        boolean isUser = "user".equals(role);

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setPadding(0, 0, 0, dpToPx(10));
        row.setGravity(isUser ? Gravity.END : Gravity.START);

        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(14);
        bubble.setLineSpacing(0f, 1.2f);
        bubble.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        bubble.setTextColor(getColor(isUser ? R.color.dark_bg : R.color.text_primary));

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dpToPx(18));
        background.setColor(getColor(isUser ? R.color.gold_primary : R.color.dark_surface_2));
        bubble.setBackground(background);

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dpToPx(72), dpToPx(320)),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bubble.setLayoutParams(bubbleParams);

        row.addView(bubble);
        conversationContainer.addView(row);
        scrollToBottom();
        return bubble;
    }

    private void scrollToBottom() {
        scrollConversation.post(() -> scrollConversation.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gateway != null) {
            gateway.close();
        }
    }
}
