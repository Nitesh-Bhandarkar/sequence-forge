package io.sequenceforge.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Tenant findOrCreateForUser(String email) {
        return tenantRepository.findByName(email)
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setName(email);
                    return tenantRepository.save(tenant);
                });
    }
}
