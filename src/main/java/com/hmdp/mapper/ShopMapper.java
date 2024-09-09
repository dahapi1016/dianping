package com.hmdp.mapper;

import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.ShopType;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopMapper extends BaseMapper<Shop> {

    @Select("SELECT * FROM tb_shop WHERE id = #{id}")
    Shop getShopById(Long id);

    @Update("UPDATE tb_shop SET name = #{name}, type_id = #{typeId}, images = #{images}," +
            " area = #{area}, address = #{address}, x = #{x}, y = #{y}, " +
            "avg_price = #{avgPrice}, sold = #{sold}, comments = #{comments}, " +
            "score = #{score}, open_hours = #{openHours}, update_time = #{updateTime} WHERE id = #{id}")
    void updateShopById(Shop shop);
}
