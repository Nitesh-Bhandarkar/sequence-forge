package io.sequenceforge.sequence.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GenerateSequenceResponse(
        String sequence,
        UUID templateId,
        String resolvedKey,
        long counterValue,
        LocalDateTime generatedAt
) {}
