package io.sequenceforge.redis;

import io.sequenceforge.common.exception.CounterOverflowException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LuaScriptRunner {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> counterIncrementScript;

    public LuaScriptRunner(RedisTemplate<String, String> redisTemplate,
                           RedisScript<Long> counterIncrementScript) {
        this.redisTemplate = redisTemplate;
        this.counterIncrementScript = counterIncrementScript;
    }

    public long getCurrentValue(String redisKey) {
        String value = redisTemplate.opsForValue().get(redisKey);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public long incrementAndGet(String redisKey, long maxCounterValue) {
        try {
            Long result = redisTemplate.execute(
                    counterIncrementScript,
                    List.of(redisKey),
                    String.valueOf(maxCounterValue)
            );
            if (result == null) {
                throw new IllegalStateException("Null response from Redis counter script for key: " + redisKey);
            }
            return result;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("SEQUENCE_OVERFLOW")) {
                throw new CounterOverflowException(redisKey, maxCounterValue);
            }
            throw e;
        }
    }
}
