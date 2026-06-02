package io.sequenceforge.ai.dto;

import java.util.List;

public record ChatRequest(
        List<ChatMessageDto> messages
) {
    public record ChatMessageDto(
            String role,     // "user" | "assistant"
            String content
    ) {}
}
