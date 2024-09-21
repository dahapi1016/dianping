package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 *
 *  服务类
 *
 * @author hapi
 * @since 2024-08-22
 */
public interface IShopService extends IService<Shop> {

    /**
    * 从Redis缓存中查询商铺信息，若存在则返回，否则加入缓存。
    * @param id         商铺ID
    */
    Result queryById(Long id);

    /**
     * 更新商铺信息
     * @param shop      新的商铺实体类
     */
    Result updateShopById(Shop shop);
}
