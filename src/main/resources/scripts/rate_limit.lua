-- Count one attempt in a fixed window that starts at the first attempt.
-- KEYS[1] = the counter key, ARGV[1] = window length in seconds
-- Returns the number of attempts in the current window (1 = first attempt).
-- INCR and EXPIRE run together, so a counter can never exist without an expiry
-- (otherwise a crash in between could block the user forever).

local count = redis.call('INCR', KEYS[1])
if count == 1 then
  redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return count
