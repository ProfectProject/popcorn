local availability_key = KEYS[1]
local order_hold_key = ARGV[1]
local quantity = tonumber(ARGV[2])
local ttl_ms = tonumber(ARGV[3])

if quantity == nil or quantity <= 0 then
    return 0
end

local available = tonumber(redis.call("GET", availability_key) or "0")
if available < quantity then
    return 0
end

redis.call("DECRBY", availability_key, quantity)
redis.call("HINCRBY", order_hold_key, availability_key, quantity)

if ttl_ms ~= nil and ttl_ms > 0 then
    redis.call("PEXPIRE", order_hold_key, ttl_ms)
end

return 1
