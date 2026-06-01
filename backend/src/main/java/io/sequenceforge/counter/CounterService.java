package io.sequenceforge.counter;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.sequenceforge.fallback.DbCounterService;
import io.sequenceforge.redis.LuaScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CounterService {

    private static final Logger log = LoggerFactory.getLogger(CounterService.class);
    private static final String CB_NAME = "redis-counter";

    private final LuaScriptRunner luaScriptRunner;
    private final DbCounterService dbCounterService;

    public CounterService(LuaScriptRunner luaScriptRunner, DbCounterService dbCounterService) {
        this.luaScriptRunner = luaScriptRunner;
        this.dbCounterService = dbCounterService;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "dbFallbackIncrement")
    public long increment(String redisKey, UUID templateId, UUID tenantId, long maxValue) {
        return luaScriptRunner.incrementAndGet(redisKey, maxValue);
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "dbFallbackPeek")
    public long peek(String redisKey) {
        return luaScriptRunner.getCurrentValue(redisKey);
    }

    // Called by Resilience4j via reflection when the circuit opens or Redis throws.
    // Signature must match increment() parameters + Throwable at the end.
    @SuppressWarnings("unused")
    private long dbFallbackIncrement(String redisKey, UUID templateId, UUID tenantId,
                                     long maxValue, Throwable cause) {
        log.warn("Redis circuit open — falling back to DB counter: key={}, cause={}",
                redisKey, cause.getMessage());
        return dbCounterService.incrementAndGet(redisKey, templateId, tenantId, maxValue);
    }

    // Called by Resilience4j via reflection when the circuit opens or Redis throws.
    @SuppressWarnings("unused")
    private long dbFallbackPeek(String redisKey, Throwable cause) {
        log.warn("Redis circuit open — falling back to DB counter peek: key={}", redisKey);
        return dbCounterService.getCurrentValue(redisKey);
    }
}
