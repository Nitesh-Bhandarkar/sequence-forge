package io.sequenceforge.sequence;

import io.sequenceforge.audit.AuditService;
import io.sequenceforge.common.TenantContext;
import io.sequenceforge.counter.CounterService;
import io.sequenceforge.placeholder.DatePlaceholderResolver;
import io.sequenceforge.placeholder.ResolverRegistry;
import io.sequenceforge.placeholder.StaticPlaceholderResolver;
import io.sequenceforge.sequence.dto.GenerateSequenceRequest;
import io.sequenceforge.sequence.dto.GenerateSequenceResponse;
import io.sequenceforge.template.*;
import io.sequenceforge.template.TemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorServiceTest {

    @Mock private TemplateService templateService;
    @Mock private CounterService counterService;
    @Mock private AuditService auditService;

    private SequenceGeneratorService service;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
        ResolverRegistry registry = new ResolverRegistry(
                List.of(new StaticPlaceholderResolver(), new DatePlaceholderResolver())
        );
        service = new SequenceGeneratorService(templateService, registry, counterService, auditService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void generate_invoiceSequence_returnsFormattedString() {
        Template template = invoiceTemplate();
        when(templateService.loadForGeneration(TEMPLATE_ID)).thenReturn(template);
        when(counterService.increment(anyString(), any(), any(), eq(9999L))).thenReturn(42L);

        GenerateSequenceResponse response = service.generate(
                new GenerateSequenceRequest(TEMPLATE_ID, Map.of("SS", "MH", "CC", "IN", "FY", "2627"))
        );

        assertThat(response.sequence()).isEqualTo("MH/IN/2627/0042");
        assertThat(response.counterValue()).isEqualTo(42L);
        assertThat(response.resolvedKey()).contains(TENANT_ID.toString());
        assertThat(response.resolvedKey()).contains("MH");
        verify(auditService).record(eq(TENANT_ID), eq(TEMPLATE_ID), anyString(), eq(42L), eq("MH/IN/2627/0042"), any());
    }

    @Test
    void generate_counterIsZeroPadded() {
        Template template = invoiceTemplate();
        when(templateService.loadForGeneration(TEMPLATE_ID)).thenReturn(template);
        when(counterService.increment(anyString(), any(), any(), eq(9999L))).thenReturn(1L);

        GenerateSequenceResponse response = service.generate(
                new GenerateSequenceRequest(TEMPLATE_ID, Map.of("SS", "KA", "CC", "IN", "FY", "2627"))
        );

        assertThat(response.sequence()).isEqualTo("KA/IN/2627/0001");
    }

    @Test
    void generate_differentParamsProduceDifferentRedisKeys() {
        Template template = invoiceTemplate();
        when(templateService.loadForGeneration(any())).thenReturn(template);
        when(counterService.increment(anyString(), any(), any(), anyLong())).thenReturn(1L);

        service.generate(new GenerateSequenceRequest(TEMPLATE_ID, Map.of("SS", "MH", "CC", "IN", "FY", "2627")));
        service.generate(new GenerateSequenceRequest(TEMPLATE_ID, Map.of("SS", "KA", "CC", "IN", "FY", "2627")));

        verify(counterService, times(2)).increment(anyString(), any(), any(), anyLong());
    }

    private Template invoiceTemplate() {
        Template t = new Template();
        t.setId(TEMPLATE_ID);
        t.setTenantId(TENANT_ID);
        t.setName("Invoice");
        t.setFormatString("{SS}/{CC}/{FY}/{SEQ}");
        t.setMaxCounterValue(9999L);
        t.setCounterPadding(4);
        t.setIsActive(true);

        t.getPlaceholderConfigs().addAll(List.of(
                placeholder(t, "SS", PlaceholderType.STATIC, null, 0),
                placeholder(t, "CC", PlaceholderType.STATIC, null, 1),
                placeholder(t, "FY", PlaceholderType.DATE, "FINANCIAL_YEAR", 2),
                placeholder(t, "SEQ", PlaceholderType.COUNTER, null, 3)
        ));
        return t;
    }

    private PlaceholderConfig placeholder(Template t, String name, PlaceholderType type, String dateFormat, int order) {
        PlaceholderConfig pc = new PlaceholderConfig();
        pc.setId(UUID.randomUUID());
        pc.setTemplate(t);
        pc.setPlaceholderName(name);
        pc.setPlaceholderType(type);
        pc.setDateFormat(dateFormat);
        pc.setIsRequired(true);
        pc.setSortOrder(order);
        return pc;
    }
}
