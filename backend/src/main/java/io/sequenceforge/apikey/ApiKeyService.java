package io.sequenceforge.apikey;

import io.sequenceforge.apikey.dto.ApiKeyCreatedResponse;
import io.sequenceforge.apikey.dto.ApiKeyResponse;
import io.sequenceforge.common.exception.ApiKeyNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX_FORMAT = "sf_";
    private static final int DISPLAY_PREFIX_LENGTH = 8;

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public ApiKeyCreatedResponse createApiKey(UUID tenantId, String name) {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String plainKey = KEY_PREFIX_FORMAT + HexFormat.of().formatHex(randomBytes);
        String keyHash = sha256(plainKey);
        String keyPrefix = plainKey.substring(0, DISPLAY_PREFIX_LENGTH);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(name);
        apiKeyRepository.save(apiKey);

        return new ApiKeyCreatedResponse(apiKey.getId(), name, keyPrefix, plainKey, apiKey.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID tenantId) {
        return apiKeyRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revokeApiKey(UUID tenantId, UUID keyId) {
        ApiKey key = apiKeyRepository.findByIdAndTenantId(keyId, tenantId)
                .orElseThrow(() -> new ApiKeyNotFoundException(keyId));
        key.setIsActive(false);
        apiKeyRepository.save(key);
    }

    // Used by ApiKeyAuthFilter — validates the raw key and returns tenant ID if valid
    public Optional<UUID> validateAndGetTenantId(String plainKey) {
        String keyHash = sha256(plainKey);
        return apiKeyRepository.findByKeyHashAndIsActiveTrue(keyHash)
                .map(key -> {
                    key.setLastUsedAt(java.time.LocalDateTime.now());
                    apiKeyRepository.save(key);
                    return key.getTenantId();
                });
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
