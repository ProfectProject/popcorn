local order_id = ARGV[1]
local popup_id = ARGV[2]
local schedule_id = ARGV[3]
local schedule_qty = tonumber(ARGV[4])
local goods_json = ARGV[5]
local goods_qtys = ARGV[6]
local ttl_ms = tonumber(ARGV[7])
local expires_at = ARGV[8]
local goods_count = tonumber(ARGV[9])

local hold_key = string.format("hold:{%s}:%s", popup_id, order_id)
local schedule_key = KEYS[1]

if redis.call("EXISTS", hold_key) == 1 then
    return -3
end

if redis.call("EXISTS", schedule_key) == 0 then
    return -2
end

local available_schedule = tonumber(redis.call("GET", schedule_key) or "0")
if available_schedule < schedule_qty then
    return -1
end

if goods_count ~= (#KEYS - 1) then
    return -4
end

local quant_map = {}
for index = 1, goods_count do
    local key = KEYS[index + 1]
    if redis.call("EXISTS", key) == 0 then
        return -2
    end
    local available = tonumber(redis.call("GET", key) or "0")
    local quantity = tonumber(ARGV[9 + index])
    if quantity == nil or quantity <= 0 then
        return -4
    end
    if available < quantity then
        return -1
    end
    quant_map[index] = quantity
end

redis.call("DECRBY", schedule_key, schedule_qty)
for index = 1, goods_count do
    redis.call("DECRBY", KEYS[index + 1], quant_map[index])
end

local goods_keys_concat = ""
if goods_count > 0 then
    goods_keys_concat = table.concat(KEYS, "|", 2, #KEYS)
end

redis.call("HMSET", hold_key,
        "type", "BOTH",
        "popupId", popup_id,
        "scheduleId", schedule_id,
        "scheduleQty", schedule_qty,
        "scheduleKey", schedule_key,
        "goodsJson", goods_json,
        "goodsKeys", goods_keys_concat,
        "goodsQtys", goods_qtys,
        "expiresAt", expires_at)
if ttl_ms ~= nil and ttl_ms > 0 then
    redis.call("PEXPIRE", hold_key, ttl_ms)
end

return 1
