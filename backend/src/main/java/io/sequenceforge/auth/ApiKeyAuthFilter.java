package io.sequenceforge.auth;

import io.sequenceforge.apikey.ApiKeyService;
import io.sequenceforge.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            sendError(response, "Missing required header: X-API-Key");
            return;
        }

        Optional<UUID> tenantId = apiKeyService.validateAndGetTenantId(apiKey);
        if (tenantId.isEmpty()) {
            sendError(response, "Invalid or revoked API key");
            return;
        }

        TenantContext.set(tenantId.get());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\"}");
    }
}
