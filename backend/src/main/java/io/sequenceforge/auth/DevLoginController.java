package io.sequenceforge.auth;

import io.sequenceforge.tenant.Tenant;
import io.sequenceforge.tenant.TenantRepository;
import io.sequenceforge.user.User;
import io.sequenceforge.user.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Only active when app.dev-login.enabled=true — never expose in production.
@RestController
@RequestMapping("/dev")
@ConditionalOnProperty(name = "app.dev-login.enabled", havingValue = "true")
public class DevLoginController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public DevLoginController(TenantRepository tenantRepository,
                              UserRepository userRepository,
                              JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> devLogin(
            @RequestParam(defaultValue = "dev@sequenceforge.io") String email) {

        Tenant tenant = tenantRepository.findByName(email).orElseGet(() -> {
            Tenant t = new Tenant();
            t.setName(email);
            return tenantRepository.save(t);
        });

        User user = userRepository.findByOauthProviderAndOauthSubject("dev", email).orElseGet(() -> {
            User u = new User();
            u.setTenantId(tenant.getId());
            u.setEmail(email);
            u.setName("Dev User");
            u.setOauthProvider("dev");
            u.setOauthSubject(email);
            return userRepository.save(u);
        });

        String token = jwtService.generateToken(user.getId(), tenant.getId(), email);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "tenantId", tenant.getId().toString()
        ));
    }
}
