package io.sequenceforge.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    // 32-byte base64 key (44 chars with padding)
    private static final String TEST_SECRET = "c2VxdWVuY2UtZm9yZ2UtZGV2LXNlY3JldC1rZXktbG9jYWwtb25seQ==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);
    }

    @Test
    void generateAndParse_roundtrip() {
        String token = jwtService.generateToken(USER_ID, TENANT_ID, "user@example.com");
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(TENANT_ID.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
    }

    @Test
    void parseToken_throwsOnTamperedToken() {
        String token = jwtService.generateToken(USER_ID, TENANT_ID, "user@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(Exception.class);
    }
}
