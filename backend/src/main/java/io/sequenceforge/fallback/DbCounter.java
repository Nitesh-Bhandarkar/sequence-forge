package io.sequenceforge.fallback;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "db_counters")
public class DbCounter {

    @Id
    @Column(name = "resolved_key", length = 500)
    private String resolvedKey;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "counter_value", nullable = false)
    private long counterValue = 0L;

    @Column(name = "max_value", nullable = false)
    private long maxValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getResolvedKey() { return resolvedKey; }
    public void setResolvedKey(String resolvedKey) { this.resolvedKey = resolvedKey; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public long getCounterValue() { return counterValue; }
    public void setCounterValue(long counterValue) { this.counterValue = counterValue; }
    public long getMaxValue() { return maxValue; }
    public void setMaxValue(long maxValue) { this.maxValue = maxValue; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
