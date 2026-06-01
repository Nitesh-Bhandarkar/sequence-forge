package io.sequenceforge.template;

import io.sequenceforge.common.TenantContext;
import io.sequenceforge.common.exception.InvalidTemplateException;
import io.sequenceforge.template.dto.CreateTemplateRequest;
import io.sequenceforge.template.dto.PlaceholderConfigRequest;
import io.sequenceforge.template.dto.TemplateResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateService templateService;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createTemplate_savesAndReturnsResponse() {
        CreateTemplateRequest request = invoiceTemplateRequest();
        Template saved = buildSavedTemplate(request);
        when(templateRepository.save(any(Template.class))).thenReturn(saved);

        TemplateResponse response = templateService.createTemplate(request);

        assertThat(response.name()).isEqualTo("Invoice");
        assertThat(response.formatString()).isEqualTo("{SS}/{CC}/{FY}/{SEQ}");
        assertThat(response.counterPadding()).isEqualTo(4);
        assertThat(response.placeholders()).hasSize(4);
    }

    @Test
    void createTemplate_failsWhenNoCounterPlaceholder() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Bad Template", null, "{SS}/{CC}",
                9999L,
                List.of(
                        new PlaceholderConfigRequest("SS", PlaceholderType.STATIC, null, null, true),
                        new PlaceholderConfigRequest("CC", PlaceholderType.STATIC, null, null, true)
                )
        );
        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("exactly one COUNTER");
    }

    @Test
    void createTemplate_failsWhenMultipleCounters() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Bad Template", null, "{SEQ1}/{SEQ2}",
                9999L,
                List.of(
                        new PlaceholderConfigRequest("SEQ1", PlaceholderType.COUNTER, null, null, true),
                        new PlaceholderConfigRequest("SEQ2", PlaceholderType.COUNTER, null, null, true)
                )
        );
        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("exactly one COUNTER");
    }

    @Test
    void createTemplate_failsWhenDatePlaceholderMissingDateFormat() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Bad Template", null, "{FY}/{SEQ}",
                9999L,
                List.of(
                        new PlaceholderConfigRequest("FY", PlaceholderType.DATE, null, null, true),
                        new PlaceholderConfigRequest("SEQ", PlaceholderType.COUNTER, null, null, true)
                )
        );
        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("dateFormat");
    }

    @Test
    void createTemplate_failsWhenConfigNameNotInFormatString() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Bad Template", null, "{SEQ}",
                9999L,
                List.of(
                        new PlaceholderConfigRequest("SEQ", PlaceholderType.COUNTER, null, null, true),
                        new PlaceholderConfigRequest("UNKNOWN", PlaceholderType.STATIC, null, null, true)
                )
        );
        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void createTemplate_failsWhenDateFormatIsUnknown() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Bad Template", null, "{FY}/{SEQ}",
                9999L,
                List.of(
                        new PlaceholderConfigRequest("FY", PlaceholderType.DATE, "INVALID_FORMAT", null, true),
                        new PlaceholderConfigRequest("SEQ", PlaceholderType.COUNTER, null, null, true)
                )
        );
        assertThatThrownBy(() -> templateService.createTemplate(request))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("INVALID_FORMAT");
    }

    @Test
    void extractPlaceholderNames_parsesCorrectly() {
        List<String> names = TemplateService.extractPlaceholderNames("{SS}/{CC}/{FY}/{SEQ}");
        assertThat(names).containsExactly("SS", "CC", "FY", "SEQ");
    }

    private CreateTemplateRequest invoiceTemplateRequest() {
        return new CreateTemplateRequest(
                "Invoice", "Invoice number template",
                "{SS}/{CC}/{FY}/{SEQ}", 9999L,
                List.of(
                        new PlaceholderConfigRequest("SS", PlaceholderType.STATIC, null, "State code", true),
                        new PlaceholderConfigRequest("CC", PlaceholderType.STATIC, null, "Country code", true),
                        new PlaceholderConfigRequest("FY", PlaceholderType.DATE, "FINANCIAL_YEAR", "Financial year", true),
                        new PlaceholderConfigRequest("SEQ", PlaceholderType.COUNTER, null, "Sequence", true)
                )
        );
    }

    private Template buildSavedTemplate(CreateTemplateRequest request) {
        Template t = new Template();
        t.setTenantId(TENANT_ID);
        t.setName(request.name());
        t.setDescription(request.description());
        t.setFormatString(request.formatString());
        t.setMaxCounterValue(request.maxCounterValue());
        t.setCounterPadding(String.valueOf(request.maxCounterValue()).length());
        t.setIsActive(true);
        List<String> order = TemplateService.extractPlaceholderNames(request.formatString());
        for (PlaceholderConfigRequest pcr : request.placeholders()) {
            PlaceholderConfig pc = new PlaceholderConfig();
            pc.setTemplate(t);
            pc.setPlaceholderName(pcr.placeholderName());
            pc.setPlaceholderType(pcr.placeholderType());
            pc.setDateFormat(pcr.dateFormat());
            pc.setDescription(pcr.description());
            pc.setIsRequired(pcr.isRequired());
            pc.setSortOrder(order.indexOf(pcr.placeholderName()));
            t.getPlaceholderConfigs().add(pc);
        }
        return t;
    }
}
