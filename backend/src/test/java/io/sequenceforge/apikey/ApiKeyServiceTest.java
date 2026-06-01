package io.sequenceforge.apikey;

import io.sequenceforge.apikey.dto.ApiKeyCreatedResponse;
import io.sequenceforge.common.exception.ApiKeyNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    void createApiKey_generatesUniqueFormattedKey() {
        when(apiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyCreatedResponse response = apiKeyService.createApiKey(TENANT_ID, "My Key");

        assertThat(response.plainKey()).startsWith("sf_");
        assertThat(response.plainKey()).hasSize(67); // "sf_" + 64 hex chars
        assertThat(response.keyPrefix()).isEqualTo(response.plainKey().substring(0, 8));
        assertThat(response.name()).isEqualTo("My Key");
    }

    @Test
    void createApiKey_storesHashNotPlainKey() {
        when(apiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);

        ApiKeyCreatedResponse response = apiKeyService.createApiKey(TENANT_ID, "My Key");

        verify(apiKeyRepository).save(captor.capture());
        ApiKey saved = captor.getValue();
        assertThat(saved.getKeyHash()).isNotEqualTo(response.plainKey());
        assertThat(saved.getKeyHash()).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    void validateAndGetTenantId_returnsEmptyForUnknownKey() {
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any())).thenReturn(Optional.empty());

        Optional<UUID> result = apiKeyService.validateAndGetTenantId("sf_unknownkey");

        assertThat(result).isEmpty();
    }

    @Test
    void validateAndGetTenantId_returnsTenantIdAndUpdatesLastUsed() {
        ApiKey key = new ApiKey();
        key.setTenantId(TENANT_ID);
        when(apiKeyRepository.findByKeyHashAndIsActiveTrue(any())).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any())).thenReturn(key);

        Optional<UUID> result = apiKeyService.validateAndGetTenantId("sf_somekey");

        assertThat(result).contains(TENANT_ID);
        verify(apiKeyRepository).save(key);
        assertThat(key.getLastUsedAt()).isNotNull();
    }

    @Test
    void revokeApiKey_throwsWhenNotFound() {
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findByIdAndTenantId(keyId, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revokeApiKey(TENANT_ID, keyId))
                .isInstanceOf(ApiKeyNotFoundException.class);
    }

    @Test
    void revokeApiKey_deactivatesKey() {
        UUID keyId = UUID.randomUUID();
        ApiKey key = new ApiKey();
        key.setIsActive(true);
        when(apiKeyRepository.findByIdAndTenantId(keyId, TENANT_ID)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any())).thenReturn(key);

        apiKeyService.revokeApiKey(TENANT_ID, keyId);

        assertThat(key.getIsActive()).isFalse();
    }
}
