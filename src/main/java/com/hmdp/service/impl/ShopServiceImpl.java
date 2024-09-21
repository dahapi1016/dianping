package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import jakarta.annotation.Resource;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopMapper shopMapper;

    private static final ThreadPoolExecutor CACHE_REBUILD_EXECUTOR = new ThreadPoolExecutor(20, 50,
            60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(50));

    @Override
    public Result queryById(Long id) {
        //Shop shop = queryShopWithLogicalExpire(id);

        //Shop shop = queryShopWithMutex(id);

        Shop shop = queryShopWithOutPassThrough(id);
        if(shop == null) {
            return Result.fail("查询失败！");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result updateShopById(Shop shop) {
        //判空
        if(shop.getId() == null) {
            return Result.fail("店铺ID不能为空！");
        }

        //先在数据库中修改，再删除缓存，降低线程安全问题造成的数据不一致风险
        shopMapper.updateShopById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    private Shop queryShopWithLogicalExpire(Long id) {
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //若结果为空则直接返回
        if(StrUtil.isBlank(shopJson)) {
            return null;
        }

        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = BeanUtil.toBean(redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        if(expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }

        String lockKey = LOCK_SHOP_KEY + id;
        boolean isSuccess = tryLock(lockKey);

        if(isSuccess) {
            shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
            redisData = JSONUtil.toBean(shopJson, RedisData.class);
            shop = BeanUtil.toBean(redisData.getData(), Shop.class);
            expireTime = redisData.getExpireTime();

            if(expireTime.isAfter(LocalDateTime.now())) {
                return shop;
            }

            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveShopToRedis(id, 3600L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    releaseLock(lockKey);
                }
            });
        }

        return shop;
    }

    private Shop queryShopWithMutex(Long id) {
        // 1. 从缓存中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        // 2. 缓存命中，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 3. 若缓存中存在空串，说明商铺不存在
        if (Objects.equals(shopJson, "")) {
            return null;
        }

        // 4. 尝试获取互斥锁，防止缓存击穿
        String lockKey = LOCK_SHOP_KEY + id; // 定义锁的key
        Shop shop;
        boolean isLock = false;

        try {
            int retries = 0; // 重试计数器
            while (!isLock && retries < 5) { // 尝试获取锁，最多重试10次
                isLock = tryLock(lockKey); // 尝试获取锁
                if (!isLock) {
                    retries++;
                    Thread.sleep(50); // 获取锁失败，休眠50毫秒后重试
                }
            }

            if (!isLock) {
                return null;
            }

            // 5. 成功获取锁后，再查一次缓存，防止其他线程已更新缓存
            shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
            if (StrUtil.isNotBlank(shopJson)) {
                return JSONUtil.toBean(shopJson, Shop.class);
            }
            if (Objects.equals(shopJson, "")) {
                return null;
            }

            // 6. 从数据库查询
            shop = shopMapper.getShopById(id);
            Thread.sleep(200); //模拟复杂查询耗费较长时间
            if (shop == null) {
                // 6.1 数据库中不存在，返回
                return null;
            }
            // 7. 将商铺数据存入缓存，并设置随机过期时间，避免缓存雪崩
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RandomUtil.randomLong(CACHE_SHOP_MIN_TTL, CACHE_SHOP_MAX_TTL), TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 8. 释放锁
            releaseLock(lockKey);
        }

        // 9. 返回商铺信息
        return shop;
    }

    private Shop queryShopWithOutPassThrough(Long id) {
        // 1. 从缓存中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        // 2. 缓存命中，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 3. 若缓存中存在空串，说明商铺不存在
        if (Objects.equals(shopJson, "")) {
            return null;
        }

        Shop shop = shopMapper.getShopById(id);

        if (shop == null) {
            // 6.1 数据库中不存在，缓存空值并返回
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

         // 7. 将商铺数据存入缓存，并设置随机过期时间，避免缓存雪崩
         stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RandomUtil.randomLong(CACHE_SHOP_MIN_TTL, CACHE_SHOP_MAX_TTL), TimeUnit.SECONDS);
        // 9. 返回商铺信息
        return shop;
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

    /**
     * 通过逻辑过期的方式获取商铺信息并缓存至Redis
     * @param id                商铺ID
     * @param expireSeconds     逻辑过期时间
     */
    public void saveShopToRedis (Long id, Long expireSeconds) {
        Shop shop = shopMapper.getShopById(id);
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        redisData.setData(shop);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
}
