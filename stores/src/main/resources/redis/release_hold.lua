local hold_key = KEYS[1]

if redis.call("EXISTS", hold_key) == 0 then
    return -3
end

local entries = redis.call("HGETALL", hold_key)
local data = {}
for index = 1, #entries, 2 do
    data[entries[index]] = entries[index + 1]
end

local schedule_key = data["scheduleKey"] or ""
local schedule_qty = tonumber(data["scheduleQty"] or "0")
if schedule_key ~= "" and schedule_qty > 0 then
    redis.call("INCRBY", schedule_key, schedule_qty)
end

local goods_keys = data["goodsKeys"] or ""
local goods_qtys = data["goodsQtys"] or ""

if goods_keys ~= "" and goods_qtys ~= "" then
    local key_list = {}
    for token in string.gmatch(goods_keys, "[^|]+") do
        table.insert(key_list, token)
    end
    local qty_list = {}
    for qty in string.gmatch(goods_qtys, "[^|]+") do
        table.insert(qty_list, tonumber(qty))
    end
    for index = 1, math.min(#key_list, #qty_list) do
        redis.call("INCRBY", key_list[index], qty_list[index])
    end
end

redis.call("DEL", hold_key)
return 1
