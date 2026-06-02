package io.sequenceforge.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassifyPlaceholderRequest(
        @NotBlank String placeholderName,
        String context
) {}
