package io.sequenceforge.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.sequenceforge.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.parseToken(token);
            String tenantIdStr = claims.get("tenantId", String.class);
            String subject = claims.getSubject();
            if (tenantIdStr == null || subject == null) {
                sendError(response, "Invalid or expired token");
                return;
            }
            UUID tenantId = UUID.fromString(tenantIdStr);
            UUID userId = UUID.fromString(subject);

            TenantContext.set(tenantId);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId, null, List.of())
            );
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            sendError(response, "Invalid or expired token");
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\"}");
    }
}
