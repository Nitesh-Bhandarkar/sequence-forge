package io.sequenceforge.auth;

import io.sequenceforge.user.User;
import io.sequenceforge.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.auth.frontend-redirect-url:}")
    private String frontendRedirectUrl;

    private final JwtService jwtService;
    private final UserService userService;

    public OAuth2SuccessHandler(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String email = resolveEmail(oAuth2User, provider);
        String name = oAuth2User.getAttribute("name");
        String subject = oAuth2User.getName();

        User user = userService.findOrCreate(email, name, provider, subject);
        String token = jwtService.generateToken(user.getId(), user.getTenantId(), email);

        if (frontendRedirectUrl != null && !frontendRedirectUrl.isBlank()) {
            getRedirectStrategy().sendRedirect(request, response,
                    frontendRedirectUrl + "?token=" + token + "&tenantId=" + user.getTenantId());
        } else {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(
                    "{\"token\":\"" + token + "\",\"tenantId\":\"" + user.getTenantId() + "\"}"
            );
        }
    }

    private String resolveEmail(OAuth2User user, String provider) {
        String email = user.getAttribute("email");
        if (email == null || email.isBlank()) {
            // GitHub may not expose email; fall back to login@provider
            String login = user.getAttribute("login");
            email = (login != null ? login : user.getName()) + "@" + provider + ".oauth";
        }
        return email;
    }
}
