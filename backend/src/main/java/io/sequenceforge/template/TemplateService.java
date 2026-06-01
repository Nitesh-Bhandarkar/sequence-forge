package io.sequenceforge.template;

import io.sequenceforge.common.TenantContext;
import io.sequenceforge.common.exception.InvalidTemplateException;
import io.sequenceforge.common.exception.TemplateNotFoundException;
import io.sequenceforge.placeholder.DateFormat;
import io.sequenceforge.template.dto.CreateTemplateRequest;
import io.sequenceforge.template.dto.PlaceholderConfigRequest;
import io.sequenceforge.template.dto.TemplateResponse;
import io.sequenceforge.template.dto.UpdateTemplateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        UUID tenantId = TenantContext.get();
        validateTemplate(request);

        Template template = new Template();
        template.setTenantId(tenantId);
        template.setName(request.name());
        template.setDescription(request.description());
        template.setFormatString(request.formatString());
        template.setMaxCounterValue(request.maxCounterValue());
        template.setCounterPadding(String.valueOf(request.maxCounterValue()).length());

        List<String> orderedNames = extractPlaceholderNames(request.formatString());
        for (PlaceholderConfigRequest pcr : request.placeholders()) {
            PlaceholderConfig config = new PlaceholderConfig();
            config.setTemplate(template);
            config.setPlaceholderName(pcr.placeholderName());
            config.setPlaceholderType(pcr.placeholderType());
            config.setDateFormat(pcr.dateFormat());
            config.setDescription(pcr.description());
            config.setIsRequired(pcr.isRequired());
            config.setSortOrder(orderedNames.indexOf(pcr.placeholderName()));
            template.getPlaceholderConfigs().add(config);
        }

        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public TemplateResponse updateTemplate(UUID templateId, UpdateTemplateRequest request) {
        UUID tenantId = TenantContext.get();
        Template template = templateRepository.findByIdAndTenantIdAndIsActiveTrue(templateId, tenantId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        if (request.name() != null) template.setName(request.name());
        if (request.description() != null) template.setDescription(request.description());
        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(UUID templateId) {
        UUID tenantId = TenantContext.get();
        Template template = templateRepository.findByIdAndTenantIdAndIsActiveTrue(templateId, tenantId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        return TemplateResponse.from(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates() {
        UUID tenantId = TenantContext.get();
        return templateRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(TemplateResponse::from)
                .toList();
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        UUID tenantId = TenantContext.get();
        Template template = templateRepository.findByIdAndTenantIdAndIsActiveTrue(templateId, tenantId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
        template.setIsActive(false);
        templateRepository.save(template);
    }

    public Template loadForGeneration(UUID templateId) {
        return templateRepository.findById(templateId)
                .filter(Template::getIsActive)
                .orElseThrow(() -> new TemplateNotFoundException(templateId));
    }

    private void validateTemplate(CreateTemplateRequest request) {
        List<String> placeholderNamesInFormat = extractPlaceholderNames(request.formatString());
        Set<String> configNames = new HashSet<>();
        long counterCount = 0;

        for (PlaceholderConfigRequest pcr : request.placeholders()) {
            configNames.add(pcr.placeholderName());
            if (pcr.placeholderType() == PlaceholderType.COUNTER) {
                counterCount++;
            }
            if (pcr.placeholderType() == PlaceholderType.DATE) {
                validateDateFormat(pcr.placeholderName(), pcr.dateFormat());
            }
        }

        if (counterCount != 1) {
            throw new InvalidTemplateException(
                    "Template must have exactly one COUNTER placeholder, found: " + counterCount);
        }

        for (String name : placeholderNamesInFormat) {
            if (!configNames.contains(name)) {
                throw new InvalidTemplateException(
                        "No config provided for placeholder {" + name + "} found in format string");
            }
        }

        for (String configName : configNames) {
            if (!placeholderNamesInFormat.contains(configName)) {
                throw new InvalidTemplateException(
                        "Placeholder config {" + configName + "} not found in format string");
            }
        }
    }

    private void validateDateFormat(String placeholderName, String dateFormat) {
        if (dateFormat == null || dateFormat.isBlank()) {
            throw new InvalidTemplateException(
                    "DATE placeholder {" + placeholderName + "} must have a dateFormat. " +
                    "Valid values: " + Arrays.toString(DateFormat.values()));
        }
        try {
            DateFormat.valueOf(dateFormat);
        } catch (IllegalArgumentException e) {
            throw new InvalidTemplateException(
                    "Unknown dateFormat '" + dateFormat + "' for placeholder {" + placeholderName + "}. " +
                    "Valid values: " + Arrays.toString(DateFormat.values()));
        }
    }

    public static List<String> extractPlaceholderNames(String formatString) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(formatString);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
