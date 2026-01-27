local schedule_key = KEYS[1]
local order_id = ARGV[1]
local popup_id = ARGV[2]
local schedule_id = ARGV[3]
local schedule_qty = tonumber(ARGV[4])
local expires_at = ARGV[5]
local ttl_ms = tonumber(ARGV[6])

local hold_key = string.format("hold:{%s}:%s", popup_id, order_id)

if redis.call("EXISTS", hold_key) == 1 then
    return -3
end

if redis.call("EXISTS", schedule_key) == 0 then
    return -2
end

local available = tonumber(redis.call("GET", schedule_key) or "0")
if available < schedule_qty then
    return -1
end

redis.call("DECRBY", schedule_key, schedule_qty)
redis.call("HMSET", hold_key,
        "type", "SCHEDULE_ONLY",
        "popupId", popup_id,
        "scheduleId", schedule_id,
        "scheduleQty", schedule_qty,
        "scheduleKey", schedule_key,
        "goodsJson", "[]",
        "goodsKeys", "",
        "goodsQtys", "",
        "expiresAt", expires_at)
if ttl_ms ~= nil and ttl_ms > 0 then
    redis.call("PEXPIRE", hold_key, ttl_ms)
end

return 1
