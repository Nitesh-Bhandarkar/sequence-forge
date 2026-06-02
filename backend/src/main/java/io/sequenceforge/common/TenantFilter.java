package io.sequenceforge.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// Dev-convenience filter: reads X-Tenant-ID header when no auth filter has set TenantContext.
// In production flows (Phase 2+), JwtAuthFilter or ApiKeyAuthFilter set TenantContext first,
// so this filter becomes a no-op on those paths.
@Component
public class TenantFilter extends OncePerRequestFilter {

    static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/") || path.startsWith("/login/")
                || path.startsWith("/actuator") || path.startsWith("/dev/") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip if an auth filter already set the tenant (JWT or API key path)
        if (TenantContext.get() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader == null || tenantHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"Missing required header: X-Tenant-ID\"}");
            return;
        }
        try {
            TenantContext.set(UUID.fromString(tenantHeader));
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid X-Tenant-ID: must be a valid UUID\"}");
        } finally {
            TenantContext.clear();
        }
    }
}
