package io.sequenceforge.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    List<Template> findByTenantIdAndIsActiveTrue(UUID tenantId);

    Optional<Template> findByIdAndTenantIdAndIsActiveTrue(UUID id, UUID tenantId);
}
