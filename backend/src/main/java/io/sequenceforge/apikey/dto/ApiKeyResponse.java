package io.sequenceforge.apikey.dto;

import io.sequenceforge.apikey.ApiKey;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt
) {
    public static ApiKeyResponse from(ApiKey key) {
        return new ApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getIsActive(),
                key.getCreatedAt(),
                key.getLastUsedAt()
        );
    }
}
