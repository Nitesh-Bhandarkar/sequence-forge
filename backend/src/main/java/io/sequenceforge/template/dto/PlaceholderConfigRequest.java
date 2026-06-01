package io.sequenceforge.template.dto;

import io.sequenceforge.template.PlaceholderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceholderConfigRequest(
        @NotBlank(message = "Placeholder name is required") String placeholderName,
        @NotNull(message = "Placeholder type is required") PlaceholderType placeholderType,
        String dateFormat,
        String description,
        boolean isRequired
) {}
