package io.sequenceforge.sequence;

import io.sequenceforge.audit.AuditService;
import io.sequenceforge.common.TenantContext;
import io.sequenceforge.placeholder.ResolverRegistry;
import io.sequenceforge.redis.LuaScriptRunner;
import io.sequenceforge.sequence.dto.CounterStatusResponse;
import io.sequenceforge.sequence.dto.GenerateSequenceRequest;
import io.sequenceforge.sequence.dto.GenerateSequenceResponse;
import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import io.sequenceforge.template.Template;
import io.sequenceforge.template.TemplateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SequenceGeneratorService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    private final TemplateService templateService;
    private final ResolverRegistry resolverRegistry;
    private final LuaScriptRunner luaScriptRunner;
    private final AuditService auditService;

    public SequenceGeneratorService(TemplateService templateService,
                                    ResolverRegistry resolverRegistry,
                                    LuaScriptRunner luaScriptRunner,
                                    AuditService auditService) {
        this.templateService = templateService;
        this.resolverRegistry = resolverRegistry;
        this.luaScriptRunner = luaScriptRunner;
        this.auditService = auditService;
    }

    public GenerateSequenceResponse generate(GenerateSequenceRequest request) {
        UUID tenantId = TenantContext.get();
        Template template = templateService.loadForGeneration(request.templateId());

        Map<String, String> resolvedValues = resolveNonCounterPlaceholders(template, request.params());
        String redisKey = buildRedisKey(tenantId, template, resolvedValues);

        long counterValue = luaScriptRunner.incrementAndGet(redisKey, template.getMaxCounterValue());

        String formattedCounter = String.format("%0" + template.getCounterPadding() + "d", counterValue);
        String sequence = buildSequence(template.getFormatString(), resolvedValues, formattedCounter);

        auditService.record(tenantId, template.getId(), redisKey, counterValue, sequence, request.params());

        return new GenerateSequenceResponse(sequence, template.getId(), redisKey, counterValue, LocalDateTime.now());
    }

    public CounterStatusResponse peekCounter(UUID templateId, Map<String, String> params) {
        UUID tenantId = TenantContext.get();
        Template template = templateService.loadForGeneration(templateId);
        Map<String, String> resolvedValues = resolveNonCounterPlaceholders(template, params);
        String redisKey = buildRedisKey(tenantId, template, resolvedValues);
        long current = luaScriptRunner.getCurrentValue(redisKey);
        return new CounterStatusResponse(redisKey, current, template.getMaxCounterValue(),
                template.getMaxCounterValue() - current);
    }

    private Map<String, String> resolveNonCounterPlaceholders(Template template, Map<String, String> params) {
        // LinkedHashMap preserves sort_order insertion order for deterministic Redis key building
        Map<String, String> resolved = new LinkedHashMap<>();
        for (PlaceholderConfig config : template.getPlaceholderConfigs()) {
            if (config.getPlaceholderType() == PlaceholderType.COUNTER) {
                continue;
            }
            String value = resolverRegistry.get(config.getPlaceholderType()).resolve(config, params);
            resolved.put(config.getPlaceholderName(), value);
        }
        return resolved;
    }

    private String buildRedisKey(UUID tenantId, Template template, Map<String, String> resolvedValues) {
        List<String> parts = new ArrayList<>();
        parts.add("seq");
        parts.add(tenantId.toString());
        parts.add(template.getId().toString());
        parts.addAll(resolvedValues.values());
        return String.join(":", parts);
    }

    private String buildSequence(String formatString, Map<String, String> resolvedValues, String formattedCounter) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(formatString);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            // Counter placeholder is absent from resolvedValues — falls back to the formatted counter
            String value = resolvedValues.getOrDefault(name, formattedCounter);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
