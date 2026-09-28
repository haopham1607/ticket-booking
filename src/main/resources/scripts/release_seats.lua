-- Release seats, but only the ones this user actually holds (compare-and-delete).
-- KEYS    = hold keys to release
-- ARGV[1] = user id
-- Returns how many holds were released.

local released = 0
for i, key in ipairs(KEYS) do
  if redis.call('GET', key) == ARGV[1] then
    redis.call('DEL', key)
    released = released + 1
  end
end
return released
