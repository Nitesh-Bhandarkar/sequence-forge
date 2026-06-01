package io.sequenceforge.template.dto;

// Only name and description are mutable — changing formatString would invalidate existing Redis counters.
public record UpdateTemplateRequest(
        String name,
        String description
) {}
