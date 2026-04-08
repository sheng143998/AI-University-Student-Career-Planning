package com.itsheng.service.service.Impl;

import com.itsheng.common.constant.MessageConstant;
import com.itsheng.common.exception.AccountNotFoundException;
import com.itsheng.common.exception.PasswordErrorException;
import com.itsheng.common.exception.UsernameExistException;
import com.itsheng.pojo.dto.UserDTO;
import com.itsheng.pojo.dto.UserLoginDTO;
import com.itsheng.pojo.dto.UserRegisterDTO;
import com.itsheng.pojo.entity.User;
import com.itsheng.pojo.vo.UserVO;
import com.itsheng.service.mapper.UserMapper;
import com.itsheng.service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO register(UserRegisterDTO userRegisterDTO) {
        // 获取要注册的用户名
        String username = userRegisterDTO.getName() != null ? userRegisterDTO.getName() : userRegisterDTO.getUsername();

        // 检查用户名是否已存在
        User existingUser = userMapper.selectByusername(username);
        if (existingUser != null) {
            throw new UsernameExistException(MessageConstant.USERNAME_EXIST);
        }

        User user = User.builder()
                .username(username)
                .password(userRegisterDTO.getPassword())
                .sex(userRegisterDTO.getSex())
                .userImage(userRegisterDTO.getUserImage())
                .createtime(LocalDateTime.now())
                .build();
        userMapper.insert(user);
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getUsername())
                .userImage(user.getUserImage())
                .sex(user.getSex())
                .build();
    }

    @Override
    public UserVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        User user = userMapper.selectByusername(username);

        // 账号不存在
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!user.getPassword().equals(password)) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getUsername())
                .userImage(user.getUserImage())
                .sex(user.getSex())
                .build();
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getUsername())
                .userImage(user.getUserImage())
                .sex(user.getSex())
                .build();
    }

    @Override
    public UserVO editUser(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 修改密码验证
        if (userDTO.getOldPassword() != null && userDTO.getNewPassword() != null) {
            if (!user.getPassword().equals(userDTO.getOldPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            user.setPassword(userDTO.getNewPassword());
        }

        // 修改昵称
        if (userDTO.getName() != null) {
            user.setUsername(userDTO.getName());
        }

        // 修改头像
        if (userDTO.getUserImage() != null) {
            user.setUserImage(userDTO.getUserImage());
        }

        // 修改性别
        if (userDTO.getSex() != null) {
            user.setSex(userDTO.getSex());
        }

        userMapper.update(user);

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getUsername())
                .userImage(user.getUserImage())
                .sex(user.getSex())
                .build();
    }

    @Override
    public void logout() {
        // 简单的退出登录，实际项目中可以在这里处理 token 黑名单等逻辑
        log.info("用户退出登录");
    }
}
