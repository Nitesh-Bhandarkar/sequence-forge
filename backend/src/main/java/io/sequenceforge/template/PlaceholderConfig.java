package io.sequenceforge.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "placeholder_configs")
public class PlaceholderConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "placeholder_name", nullable = false, length = 50)
    private String placeholderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "placeholder_type", nullable = false, length = 20)
    private PlaceholderType placeholderType;

    @Column(name = "date_format", length = 50)
    private String dateFormat;

    @Column
    private String description;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }
    public String getPlaceholderName() { return placeholderName; }
    public void setPlaceholderName(String placeholderName) { this.placeholderName = placeholderName; }
    public PlaceholderType getPlaceholderType() { return placeholderType; }
    public void setPlaceholderType(PlaceholderType placeholderType) { this.placeholderType = placeholderType; }
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
