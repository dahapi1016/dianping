package com.hmdp.dto;

import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
