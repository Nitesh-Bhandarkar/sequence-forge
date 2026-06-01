package io.sequenceforge.apikey.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(
        @NotBlank(message = "API key name is required") String name
) {}
