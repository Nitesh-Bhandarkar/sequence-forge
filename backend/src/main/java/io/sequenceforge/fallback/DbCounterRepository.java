package io.sequenceforge.fallback;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DbCounterRepository extends JpaRepository<DbCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DbCounter c WHERE c.resolvedKey = :key")
    Optional<DbCounter> findByResolvedKeyWithLock(@Param("key") String key);

    // Idempotent row creation — concurrent calls are safe due to ON CONFLICT DO NOTHING.
    @Modifying
    @Query(value = "INSERT INTO db_counters (resolved_key, tenant_id, template_id, counter_value, max_value, updated_at) " +
                   "VALUES (:key, :tenantId, :templateId, 0, :maxValue, now()) ON CONFLICT (resolved_key) DO NOTHING",
           nativeQuery = true)
    void insertIfAbsent(@Param("key") String key,
                        @Param("tenantId") UUID tenantId,
                        @Param("templateId") UUID templateId,
                        @Param("maxValue") long maxValue);
}
