package io.sequenceforge.common.exception;

public class CounterOverflowException extends RuntimeException {
    public CounterOverflowException(String redisKey, long maxValue) {
        super("Counter overflow for key [" + redisKey + "]: max allowed value is " + maxValue);
    }
}
