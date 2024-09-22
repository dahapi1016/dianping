package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.service.impl.ShopServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;

@Slf4j
@Component
public class CacheUtils {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 20,
            60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(50));

    public CacheUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit) {
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plus(time, timeUnit.toChronoUnit()));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <T, ID> T getWithOutPassingThrough(String prefix, ID id, Class<T> tClass, Function<ID, T> dbFallBack, Long time,
                                              TimeUnit timeUnit) {
        String key = prefix + StrUtil.toString(id);
        String beanJson =  stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(beanJson)) {
            return JSONUtil.toBean(beanJson, tClass);
        }
        if(Objects.equals(beanJson, "")) { //缓存空值，直接返回null
            return null;
        }

        T t = dbFallBack.apply(id);
        if(t == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        this.set(key, t, time, timeUnit);
        return t;
    }

    public <T, ID> T getWithLogicalExpire(String lockPrefix, String idPrefix, ID id, Class<T> type, Function<ID, T> dbFallBack, Long time,
                                          TimeUnit timeUnit) {
        String key = idPrefix + StrUtil.toString(id);
        String beanJson =  stringRedisTemplate.opsForValue().get(key);

        if(StrUtil.isBlank(beanJson)) {
            return null;
        }

        RedisData redisData = JSONUtil.toBean(beanJson, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        T t = BeanUtil.toBean(redisData.getData(), type);

        if(expireTime.isAfter(LocalDateTime.now())) {
            return JSONUtil.toBean(beanJson, type);
        }

        boolean isSuccessTryingLock = this.tryLock(lockPrefix + id);
        if(isSuccessTryingLock) {
            threadPoolExecutor.submit(() -> {
                try {
                    T data = dbFallBack.apply(id);
                    this.setWithLogicalExpire(idPrefix + id, data, time, timeUnit);
                } catch (Exception e){
                    throw new RuntimeException(e);
                } finally {
                    this.releaseLock(lockPrefix + id);
                }
            });
        }

        return t;
    }

    /**
     * 尝试通过set nx获取互斥锁
     * @param key   互斥锁的key
     * @return      获取是否成功
     */
    private boolean tryLock(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", 10, TimeUnit.SECONDS));
    }

    /**
     * 流程结束后释放锁
     * @param key   互斥锁的key
     */
    private void releaseLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
