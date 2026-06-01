package io.sequenceforge.sequence.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record GenerateSequenceRequest(
        @NotNull(message = "templateId is required") UUID templateId,
        Map<String, String> params
) {
    public GenerateSequenceRequest {
        params = params != null ? params : Collections.emptyMap();
    }
}
