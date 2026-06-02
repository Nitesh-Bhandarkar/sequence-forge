package io.sequenceforge.ai.dto;

public record ClassifyPlaceholderResponse(
        String placeholderType,   // STATIC | DATE | COUNTER
        String dateFormat,        // non-null only when placeholderType == DATE
        String description,
        String reasoning
) {}
