package io.sequenceforge.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SequenceAuditRepository extends JpaRepository<SequenceAudit, UUID> {

    Page<SequenceAudit> findByTenantIdAndTemplateIdAndGeneratedAtBetween(
            UUID tenantId, UUID templateId,
            LocalDateTime from, LocalDateTime to,
            Pageable pageable);

    Page<SequenceAudit> findByTenantId(UUID tenantId, Pageable pageable);
}
