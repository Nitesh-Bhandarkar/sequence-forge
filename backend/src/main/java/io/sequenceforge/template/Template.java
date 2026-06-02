package io.sequenceforge.template;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "templates")
public class Template implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "format_string", nullable = false, length = 500)
    private String formatString;

    @Column(name = "max_counter_value", nullable = false)
    private Long maxCounterValue;

    @Column(name = "counter_padding", nullable = false)
    private Integer counterPadding;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<PlaceholderConfig> placeholderConfigs = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFormatString() { return formatString; }
    public void setFormatString(String formatString) { this.formatString = formatString; }
    public Long getMaxCounterValue() { return maxCounterValue; }
    public void setMaxCounterValue(Long maxCounterValue) { this.maxCounterValue = maxCounterValue; }
    public Integer getCounterPadding() { return counterPadding; }
    public void setCounterPadding(Integer counterPadding) { this.counterPadding = counterPadding; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<PlaceholderConfig> getPlaceholderConfigs() { return placeholderConfigs; }
    public void setPlaceholderConfigs(List<PlaceholderConfig> placeholderConfigs) { this.placeholderConfigs = placeholderConfigs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
