package io.sequenceforge.audit;

import io.sequenceforge.audit.dto.AuditResponse;
import io.sequenceforge.common.ApiResponse;
import io.sequenceforge.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final SequenceAuditRepository auditRepository;

    public AuditController(SequenceAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @GetMapping
    public ApiResponse<Page<AuditResponse>> getAuditLog(
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = TenantContext.get();
        PageRequest pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());

        Page<AuditResponse> result;
        if (templateId != null && from != null && to != null) {
            result = auditRepository
                    .findByTenantIdAndTemplateIdAndGeneratedAtBetween(tenantId, templateId, from, to, pageable)
                    .map(AuditResponse::from);
        } else {
            result = auditRepository.findByTenantId(tenantId, pageable).map(AuditResponse::from);
        }

        return ApiResponse.ok(result);
    }
}
