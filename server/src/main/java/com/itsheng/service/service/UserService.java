package com.itsheng.service.service;

import com.itsheng.pojo.dto.UserLoginDTO;
import com.itsheng.pojo.dto.UserRegisterDTO;
import com.itsheng.pojo.entity.User;
import com.itsheng.pojo.vo.UserRegisterVO;

public interface UserService {
    UserRegisterVO register(UserRegisterDTO userRegisterDTO);

    User login(UserLoginDTO userLoginDTO);
}
