package io.sequenceforge.sequence.dto;

public record CounterStatusResponse(
        String resolvedKey,
        long currentValue,
        long maxValue,
        long remaining
) {}
