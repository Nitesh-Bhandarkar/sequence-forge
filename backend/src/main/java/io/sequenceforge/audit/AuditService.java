package io.sequenceforge.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final SequenceAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditService(SequenceAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @Async("auditExecutor")
    public void record(UUID tenantId, UUID templateId, String resolvedKey,
                       long counterValue, String fullSequence, Map<String, String> params) {
        try {
            SequenceAudit audit = new SequenceAudit();
            audit.setTenantId(tenantId);
            audit.setTemplateId(templateId);
            audit.setResolvedKey(resolvedKey);
            audit.setCounterValue(counterValue);
            audit.setFullSequence(fullSequence);
            audit.setRequestParams(objectMapper.writeValueAsString(params));
            audit.setGeneratedAt(LocalDateTime.now());
            auditRepository.save(audit);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit params for sequence: {}", fullSequence, e);
        } catch (Exception e) {
            log.error("Failed to persist audit record for sequence: {}", fullSequence, e);
        }
    }
}
