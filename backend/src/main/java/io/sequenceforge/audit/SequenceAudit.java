package io.sequenceforge.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sequence_audit", indexes = {
        @Index(name = "idx_audit_tenant_template", columnList = "tenant_id, template_id"),
        @Index(name = "idx_audit_generated_at", columnList = "generated_at")
})
public class SequenceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "resolved_key", nullable = false, length = 500)
    private String resolvedKey;

    @Column(name = "counter_value", nullable = false)
    private Long counterValue;

    @Column(name = "full_sequence", nullable = false, length = 500)
    private String fullSequence;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public String getResolvedKey() { return resolvedKey; }
    public void setResolvedKey(String resolvedKey) { this.resolvedKey = resolvedKey; }
    public Long getCounterValue() { return counterValue; }
    public void setCounterValue(Long counterValue) { this.counterValue = counterValue; }
    public String getFullSequence() { return fullSequence; }
    public void setFullSequence(String fullSequence) { this.fullSequence = fullSequence; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
