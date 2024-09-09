package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ShopTypeMapper shopTypeMapper;

    @Override
    public Result queryList() {
        String key = CACHE_SHOP_TYPE_KEY;
        String shopTypeJSON = stringRedisTemplate.opsForValue().get(key);

        //从Redis中获取到了商铺类型信息
        if(StrUtil.isNotBlank(shopTypeJSON)) {
            return Result.ok(JSONUtil.toList(shopTypeJSON, ShopType.class));
        }

        //从数据库中获取信息，并存入缓存
        List<ShopType> list = shopTypeMapper.getShopType();
        if(list == null) {
            return Result.fail("店铺类型列表为空！");
        }
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(list));
        return Result.ok(list);
    }
}
