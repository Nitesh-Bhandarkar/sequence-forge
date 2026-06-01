package io.sequenceforge.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTemplateRequest(
        @NotBlank(message = "Template name is required") String name,
        String description,
        @NotBlank(message = "Format string is required") String formatString,
        @NotNull @Min(value = 1, message = "maxCounterValue must be at least 1") Long maxCounterValue,
        @NotEmpty(message = "At least one placeholder config is required") @Valid List<PlaceholderConfigRequest> placeholders
) {}
