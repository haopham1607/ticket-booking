-- Hold several seats for one user, all or nothing.
-- KEYS    = one hold key per seat, e.g. hold:4:101
-- ARGV[1] = user id, ARGV[2] = hold time in seconds
-- Returns 1 if every seat is now held by this user, 0 if any seat is held by someone else.

-- Pass 1: check every seat before changing anything
for i, key in ipairs(KEYS) do
  local owner = redis.call('GET', key)
  if owner and owner ~= ARGV[1] then
    return 0
  end
end

-- Pass 2: every seat is free or already ours; hold them all and reset the timer
for i, key in ipairs(KEYS) do
  redis.call('SET', key, ARGV[1], 'EX', ARGV[2])
end
return 1
