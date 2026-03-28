package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String name;
    private String userImage;
    private Integer sex;
}
