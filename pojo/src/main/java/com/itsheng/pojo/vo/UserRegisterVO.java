package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterVO {
    private String username;
    private LocalDateTime createtime;
    private Integer sex;
    private String userimage;
}
