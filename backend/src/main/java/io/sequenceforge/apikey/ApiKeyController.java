package io.sequenceforge.apikey;

import io.sequenceforge.apikey.dto.ApiKeyCreatedResponse;
import io.sequenceforge.apikey.dto.ApiKeyResponse;
import io.sequenceforge.apikey.dto.CreateApiKeyRequest;
import io.sequenceforge.common.ApiResponse;
import io.sequenceforge.common.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apikeys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiKeyCreatedResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        return ApiResponse.ok(apiKeyService.createApiKey(TenantContext.get(), request.name()));
    }

    @GetMapping
    public ApiResponse<List<ApiKeyResponse>> listApiKeys() {
        return ApiResponse.ok(apiKeyService.listApiKeys(TenantContext.get()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiKey(@PathVariable UUID id) {
        apiKeyService.revokeApiKey(TenantContext.get(), id);
    }
}
