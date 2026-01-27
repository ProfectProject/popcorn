local order_hold_key = KEYS[1]
local entries = redis.call("HGETALL", order_hold_key)

if entries == nil or #entries == 0 then
    return 0
end

for index = 1, #entries, 2 do
    local key = entries[index]
    local quantity = tonumber(entries[index + 1])
    if key ~= nil and quantity ~= nil and quantity > 0 then
        redis.call("INCRBY", key, quantity)
    end
end

redis.call("DEL", order_hold_key)
return 1
