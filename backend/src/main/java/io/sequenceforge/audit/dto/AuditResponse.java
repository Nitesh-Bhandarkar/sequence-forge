package io.sequenceforge.audit.dto;

import io.sequenceforge.audit.SequenceAudit;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        UUID templateId,
        String resolvedKey,
        long counterValue,
        String fullSequence,
        String requestParams,
        LocalDateTime generatedAt
) {
    public static AuditResponse from(SequenceAudit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getTemplateId(),
                audit.getResolvedKey(),
                audit.getCounterValue(),
                audit.getFullSequence(),
                audit.getRequestParams(),
                audit.getGeneratedAt()
        );
    }
}
