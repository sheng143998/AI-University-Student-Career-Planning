package com.itsheng.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private Integer sex;
    private String userImage;

    // 用于修改密码
    private String oldPassword;
    private String newPassword;

    // 用于修改昵称
    private String name;
}
