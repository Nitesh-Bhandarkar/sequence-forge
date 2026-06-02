package io.sequenceforge.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sequenceforge.ai.dto.ChatRequest;
import io.sequenceforge.ai.dto.ClassifyPlaceholderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String MODEL = "claude-opus-4-8";

    private static final String CLASSIFY_SYSTEM_PROMPT = """
            You are an expert at designing business sequence number templates.
            Classify a placeholder name into exactly one of three types:

            - STATIC: The caller provides this value at request time (e.g., state code, department, branch).
            - DATE:   The value is derived from a date (e.g., year, month, financial year, quarter).
            - COUNTER: The auto-incrementing unique sequence number (only one COUNTER per template).

            Available DATE format codes:
            FINANCIAL_YEAR (2627), FINANCIAL_YEAR_FULL (2026-27), FINANCIAL_QUARTER (FQ1-FQ4),
            YEAR_4 (2026), YEAR_2 (26), MONTH_2 (06), DAY_2 (01),
            QUARTER (Q1-Q4), HALF_YEAR (H1/H2), WEEK_OF_YEAR (01-53),
            YYYYMM (202606), YYYYMMDD (20260601)

            Respond with ONLY a valid JSON object — no markdown, no prose:
            {
              "placeholderType": "STATIC" | "DATE" | "COUNTER",
              "dateFormat": "<code or null>",
              "description": "<one-line description>",
              "reasoning": "<one sentence>"
            }
            """;

    private static final String CHAT_SYSTEM_PROMPT = """
            You are an expert assistant helping users build sequence number templates for business applications.

            Template syntax: use {PLACEHOLDER} for each field. Example: {SS}/{CC}/{FY}/{SEQ}

            Placeholder types:
            - STATIC:  caller provides value at runtime  →  {SS} = state code "MH"
            - DATE:    auto-resolved from date           →  {FY} = financial year "2627"
            - COUNTER: auto-incrementing unique number   →  {SEQ} = "0042"

            DATE format codes:
            FINANCIAL_YEAR→2627, FINANCIAL_YEAR_FULL→2026-27, FINANCIAL_QUARTER→FQ1,
            YEAR_4→2026, YEAR_2→26, MONTH_2→06, DAY_2→01,
            QUARTER→Q1, HALF_YEAR→H1, WEEK_OF_YEAR→01, YYYYMM→202606, YYYYMMDD→20260601

            Rules:
            - Every template must have exactly ONE COUNTER placeholder
            - Counter padding is inferred from maxCounterValue (e.g., 9999 → 4-digit "0042")
            - Counter resets implicitly when date values in the key change (e.g., new financial year = new counter)

            Be concise and practical. When suggesting templates, always show an example output.
            """;

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public AiService(AnthropicClient anthropicClient, ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
    }

    public ClassifyPlaceholderResponse classifyPlaceholder(String name, String context) {
        String userMessage = String.format(
                "Classify this placeholder: {%s}\nContext: %s",
                name,
                context != null && !context.isBlank() ? context : "business document sequence number"
        );

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(512L)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(CLASSIFY_SYSTEM_PROMPT)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()
                ))
                .addUserMessage(userMessage)
                .build();

        Message response = anthropicClient.messages().create(params);
        String json = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .findFirst()
                .orElse("{}");

        try {
            return objectMapper.readValue(json, ClassifyPlaceholderResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse classification JSON: {}", json, e);
            return new ClassifyPlaceholderResponse("STATIC", null, name + " value", "Could not parse response");
        }
    }

    public void streamChat(List<ChatRequest.ChatMessageDto> messages, SseEmitter emitter) {
        List<MessageParam> messageParams = messages.stream()
                .map(m -> MessageParam.builder()
                        .role(MessageParam.Role.of(m.role()))
                        .content(MessageParam.Content.ofString(m.content()))
                        .build())
                .toList();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(4096L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(CHAT_SYSTEM_PROMPT)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()
                ))
                .messages(messageParams)
                .build();

        try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
            stream.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(delta -> delta.delta().text().stream())
                    .forEach(textDelta -> {
                        try {
                            emitter.send(SseEmitter.event().data(textDelta.text()));
                        } catch (IOException e) {
                            throw new RuntimeException("SSE send failed", e);
                        }
                    });

            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
        } catch (Exception e) {
            log.error("Chat streaming failed", e);
            emitter.completeWithError(e);
        }
    }
}
