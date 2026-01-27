local order_id = ARGV[1]
local popup_id = ARGV[2]
local ttl_ms = tonumber(ARGV[3])
local expires_at = ARGV[4]
local goods_json = ARGV[5]
local goods_qtys = ARGV[6]
local goods_count = tonumber(ARGV[7])

local hold_key = string.format("hold:{%s}:%s", popup_id, order_id)

if redis.call("EXISTS", hold_key) == 1 then
    return -3
end

if goods_count ~= #KEYS then
    return -4
end

local quant_map = {}
for index = 1, goods_count do
    local key = KEYS[index]
    if redis.call("EXISTS", key) == 0 then
        return -2
    end
    local available = tonumber(redis.call("GET", key) or "0")
    local quantity = tonumber(ARGV[7 + index])
    if quantity == nil or quantity <= 0 then
        return -4
    end
    if available < quantity then
        return -1
    end
    quant_map[index] = quantity
end

for index = 1, goods_count do
    redis.call("DECRBY", KEYS[index], quant_map[index])
end

redis.call("HMSET", hold_key,
        "type", "GOODS_ONLY",
        "popupId", popup_id,
        "scheduleId", "",
        "scheduleQty", 0,
        "scheduleKey", "",
        "goodsJson", goods_json,
        "goodsKeys", table.concat(KEYS, "|"),
        "goodsQtys", goods_qtys,
        "expiresAt", expires_at)
if ttl_ms ~= nil and ttl_ms > 0 then
    redis.call("PEXPIRE", hold_key, ttl_ms)
end

return 1
