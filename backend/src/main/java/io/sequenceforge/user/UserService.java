package io.sequenceforge.user;

import io.sequenceforge.tenant.Tenant;
import io.sequenceforge.tenant.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantService tenantService;

    public UserService(UserRepository userRepository, TenantService tenantService) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public User findOrCreate(String email, String name, String provider, String subject) {
        return userRepository.findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> {
                    Tenant tenant = tenantService.findOrCreateForUser(email);
                    User user = new User();
                    user.setTenantId(tenant.getId());
                    user.setEmail(email);
                    user.setName(name);
                    user.setOauthProvider(provider);
                    user.setOauthSubject(subject);
                    return userRepository.save(user);
                });
    }
}
