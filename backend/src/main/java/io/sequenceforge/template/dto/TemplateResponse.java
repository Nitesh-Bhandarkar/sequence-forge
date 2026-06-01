package io.sequenceforge.template.dto;

import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import io.sequenceforge.template.Template;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        String description,
        String formatString,
        Long maxCounterValue,
        Integer counterPadding,
        List<PlaceholderConfigResponse> placeholders,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record PlaceholderConfigResponse(
            UUID id,
            String placeholderName,
            PlaceholderType placeholderType,
            String dateFormat,
            String description,
            boolean isRequired,
            int sortOrder
    ) {
        public static PlaceholderConfigResponse from(PlaceholderConfig config) {
            return new PlaceholderConfigResponse(
                    config.getId(),
                    config.getPlaceholderName(),
                    config.getPlaceholderType(),
                    config.getDateFormat(),
                    config.getDescription(),
                    config.getIsRequired(),
                    config.getSortOrder()
            );
        }
    }

    public static TemplateResponse from(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getFormatString(),
                template.getMaxCounterValue(),
                template.getCounterPadding(),
                template.getPlaceholderConfigs().stream()
                        .map(PlaceholderConfigResponse::from)
                        .toList(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
