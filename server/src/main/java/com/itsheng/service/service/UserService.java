package com.itsheng.service.service;

import com.itsheng.pojo.dto.UserDTO;
import com.itsheng.pojo.dto.UserLoginDTO;
import com.itsheng.pojo.dto.UserRegisterDTO;
import com.itsheng.pojo.entity.User;
import com.itsheng.pojo.vo.UserVO;

public interface UserService {
    UserVO register(UserRegisterDTO userRegisterDTO);

    UserVO login(UserLoginDTO userLoginDTO);

    UserVO getUserInfo(Long userId);

    UserVO editUser(Long userId, UserDTO userDTO);

    void logout();
}
