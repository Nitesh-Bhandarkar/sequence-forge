package io.sequenceforge.fallback;

import io.sequenceforge.common.exception.CounterOverflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DbCounterService {

    private static final Logger log = LoggerFactory.getLogger(DbCounterService.class);

    private final DbCounterRepository dbCounterRepository;

    public DbCounterService(DbCounterRepository dbCounterRepository) {
        this.dbCounterRepository = dbCounterRepository;
    }

    @Transactional
    public long incrementAndGet(String resolvedKey, UUID templateId, UUID tenantId, long maxValue) {
        // Upsert row first so the subsequent lock-read always finds it.
        // ON CONFLICT DO NOTHING makes concurrent first-inserts safe.
        dbCounterRepository.insertIfAbsent(resolvedKey, tenantId, templateId, maxValue);

        DbCounter counter = dbCounterRepository.findByResolvedKeyWithLock(resolvedKey)
                .orElseThrow(() -> new IllegalStateException("Counter row missing after upsert: " + resolvedKey));

        if (counter.getCounterValue() >= maxValue) {
            throw new CounterOverflowException(resolvedKey, maxValue);
        }

        counter.setCounterValue(counter.getCounterValue() + 1);
        counter.setUpdatedAt(LocalDateTime.now());
        dbCounterRepository.save(counter);

        log.debug("DB fallback counter incremented: key={} value={}", resolvedKey, counter.getCounterValue());
        return counter.getCounterValue();
    }

    @Transactional(readOnly = true)
    public long getCurrentValue(String resolvedKey) {
        return dbCounterRepository.findById(resolvedKey)
                .map(DbCounter::getCounterValue)
                .orElse(0L);
    }
}
