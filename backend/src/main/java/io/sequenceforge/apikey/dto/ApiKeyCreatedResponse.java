package io.sequenceforge.apikey.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// plainKey is included only in this creation response — it is never retrievable again.
public record ApiKeyCreatedResponse(
        UUID id,
        String name,
        String keyPrefix,
        String plainKey,
        LocalDateTime createdAt
) {}
