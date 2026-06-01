package io.sequenceforge.fallback;

import io.sequenceforge.common.exception.CounterOverflowException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbCounterServiceTest {

    @Mock
    private DbCounterRepository dbCounterRepository;

    @InjectMocks
    private DbCounterService dbCounterService;

    private static final String KEY = "seq:tenant:template:MH:IN:2627";
    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final long MAX = 9999L;

    @Test
    void incrementAndGet_createsCounterAndReturns1OnFirstCall() {
        when(dbCounterRepository.findByResolvedKeyWithLock(KEY)).thenReturn(Optional.empty());
        when(dbCounterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        long result = dbCounterService.incrementAndGet(KEY, TEMPLATE_ID, TENANT_ID, MAX);

        assertThat(result).isEqualTo(1L);
        ArgumentCaptor<DbCounter> captor = ArgumentCaptor.forClass(DbCounter.class);
        verify(dbCounterRepository, times(2)).save(captor.capture());
        DbCounter saved = captor.getAllValues().get(1);
        assertThat(saved.getCounterValue()).isEqualTo(1L);
        assertThat(saved.getMaxValue()).isEqualTo(MAX);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void incrementAndGet_incrementsExistingCounter() {
        DbCounter existing = counter(41L, MAX);
        when(dbCounterRepository.findByResolvedKeyWithLock(KEY)).thenReturn(Optional.of(existing));
        when(dbCounterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        long result = dbCounterService.incrementAndGet(KEY, TEMPLATE_ID, TENANT_ID, MAX);

        assertThat(result).isEqualTo(42L);
        assertThat(existing.getCounterValue()).isEqualTo(42L);
    }

    @Test
    void incrementAndGet_throwsOverflowWhenAtMax() {
        when(dbCounterRepository.findByResolvedKeyWithLock(KEY)).thenReturn(Optional.of(counter(MAX, MAX)));

        assertThatThrownBy(() -> dbCounterService.incrementAndGet(KEY, TEMPLATE_ID, TENANT_ID, MAX))
                .isInstanceOf(CounterOverflowException.class);
        verify(dbCounterRepository, never()).save(any());
    }

    @Test
    void getCurrentValue_returns0WhenCounterAbsent() {
        when(dbCounterRepository.findById(KEY)).thenReturn(Optional.empty());

        assertThat(dbCounterService.getCurrentValue(KEY)).isZero();
    }

    @Test
    void getCurrentValue_returnsStoredValue() {
        when(dbCounterRepository.findById(KEY)).thenReturn(Optional.of(counter(77L, MAX)));

        assertThat(dbCounterService.getCurrentValue(KEY)).isEqualTo(77L);
    }

    private DbCounter counter(long value, long max) {
        DbCounter c = new DbCounter();
        c.setResolvedKey(KEY);
        c.setCounterValue(value);
        c.setMaxValue(max);
        c.setTenantId(TENANT_ID);
        c.setTemplateId(TEMPLATE_ID);
        return c;
    }
}
