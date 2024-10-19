--判断优惠券库存是否充足，用户是否已经下过单
local userId = ARGV[1]
local voucherId = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 检查库存是否充足，redis.call('get', stockKey)返回nil时，tonumber会返回nil，处理nil情况
if(tonumber(redis.call('get', stockKey) or 0) <= 0) then
    return 1
end

-- 检查用户是否已经下过单
if(redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 扣减库存并记录订单
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
return 0
