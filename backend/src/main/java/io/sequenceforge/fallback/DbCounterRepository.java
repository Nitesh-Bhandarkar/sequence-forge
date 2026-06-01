package io.sequenceforge.fallback;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DbCounterRepository extends JpaRepository<DbCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DbCounter c WHERE c.resolvedKey = :key")
    Optional<DbCounter> findByResolvedKeyWithLock(@Param("key") String key);
}
