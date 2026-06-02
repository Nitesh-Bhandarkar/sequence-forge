package io.sequenceforge.auth;

import io.sequenceforge.apikey.ApiKeyService;
import io.sequenceforge.config.CorsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final ApiKeyService apiKeyService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CorsConfig corsConfig;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(JwtService jwtService,
                          ApiKeyService apiKeyService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          CorsConfig corsConfig,
                          ClientRegistrationRepository clientRegistrationRepository) {
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.corsConfig = corsConfig;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    private OAuth2AuthorizationRequestResolver promptSelectAccountResolver(
            ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(
                builder -> builder.additionalParameters(p -> p.put("prompt", "select_account")));
        return resolver;
    }

    // Chain 1: sequence generation + counter peek — API key auth
    @Bean
    @Order(1)
    public SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {
        ApiKeyAuthFilter apiKeyAuthFilter = new ApiKeyAuthFilter(apiKeyService);
        return http
                .securityMatcher("/api/v1/sequences/generate", "/api/v1/sequences/counter")
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Chain 2: template management + API key management — JWT auth + OAuth2 login
    @Bean
    @Order(2)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService);
        return http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                // IF_REQUIRED allows OAuth2 to store state in a temporary session.
                // JWT auth itself is stateless (reads Bearer token per request).
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**", "/error", "/actuator/**", "/dev/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(promptSelectAccountResolver(clientRegistrationRepository))
                        )
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
