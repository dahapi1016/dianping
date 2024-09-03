package com.hmdp.mapper;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, nick_name, icon FROM tb_user WHERE phone = #{phone}")
    UserDTO getUserByPhoneNumber(String phone);

    @Insert("INSERT INTO tb_user(phone, create_time, update_time, nick_name) VALUES (#{phone}, #{createTime}, #{createTime}, #{name})")
    void createUserWithPhone(String phone, LocalDateTime createTime, String name);
}
