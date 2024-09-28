package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private CacheUtils cacheUtils;

    @Override
    public Result queryById(Long id) {

        Shop shop = cacheUtils.getWithOutPassingThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById,
                RandomUtil.randomLong(CACHE_SHOP_MIN_TTL, CACHE_SHOP_MAX_TTL), TimeUnit.SECONDS);
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

        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        shopMapper.updateShopById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

//    private Shop queryShopWithMutex(Long id) {
//        // 1. 从缓存中获取商铺信息
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//
//        // 2. 缓存命中，直接返回
//        if (StrUtil.isNotBlank(shopJson)) {
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        // 3. 若缓存中存在空串，说明商铺不存在
//        if (Objects.equals(shopJson, "")) {
//            return null;
//        }
//
//        // 4. 尝试获取互斥锁，防止缓存击穿
//        String lockKey = LOCK_SHOP_KEY + id; // 定义锁的key
//        Shop shop;
//        boolean isLock = false;
//
//        try {
//            int retries = 0; // 重试计数器
//            while (!isLock && retries < 5) { // 尝试获取锁，最多重试10次
//                isLock = tryLock(lockKey); // 尝试获取锁
//                if (!isLock) {
//                    retries++;
//                    Thread.sleep(50); // 获取锁失败，休眠50毫秒后重试
//                }
//            }
//
//            if (!isLock) {
//                return null;
//            }
//
//            // 5. 成功获取锁后，再查一次缓存，防止其他线程已更新缓存
//            shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//            if (StrUtil.isNotBlank(shopJson)) {
//                return JSONUtil.toBean(shopJson, Shop.class);
//            }
//            if (Objects.equals(shopJson, "")) {
//                return null;
//            }
//
//            // 6. 从数据库查询
//            shop = shopMapper.getShopById(id);
//            Thread.sleep(200); //模拟复杂查询耗费较长时间
//            if (shop == null) {
//                // 6.1 数据库中不存在，返回
//                return null;
//            }
//            // 7. 将商铺数据存入缓存，并设置随机过期时间，避免缓存雪崩
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), RandomUtil.randomLong(CACHE_SHOP_MIN_TTL, CACHE_SHOP_MAX_TTL), TimeUnit.SECONDS);
//
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            // 8. 释放锁
//            releaseLock(lockKey);
//        }
//
//        // 9. 返回商铺信息
//        return shop;
//    }
//
//    /**
//     * 尝试通过set nx获取互斥锁
//     * @param key   互斥锁的key
//     * @return      获取是否成功
//     */
//    private boolean tryLock(String key) {
//        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", 10, TimeUnit.SECONDS));
//    }
//
//    /**
//     * 流程结束后释放锁
//     * @param key   互斥锁的key
//     */
//    private void releaseLock(String key) {
//        stringRedisTemplate.delete(key);
//    }

}
