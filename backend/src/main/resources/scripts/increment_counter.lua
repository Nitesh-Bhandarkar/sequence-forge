-- Atomically increment counter and guard against overflow.
-- KEYS[1]: Redis key for the counter
-- ARGV[1]: max allowed value (inclusive)
-- Returns the new counter value, or error reply SEQUENCE_OVERFLOW if max exceeded.
local key = KEYS[1]
local max_val = tonumber(ARGV[1])
local current = redis.call('INCR', key)
if current > max_val then
    redis.call('DECR', key)
    return redis.error_reply('SEQUENCE_OVERFLOW')
end
return current
