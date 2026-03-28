package com.itsheng.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDTO {
    private String username;
    private String password;
    private String name;
    private Integer sex;
    private String userImage;
}
