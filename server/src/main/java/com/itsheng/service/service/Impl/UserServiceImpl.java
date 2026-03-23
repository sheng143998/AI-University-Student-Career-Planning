package com.itsheng.service.service.Impl;

import com.itsheng.common.constant.MessageConstant;
import com.itsheng.common.exception.AccountNotFoundException;
import com.itsheng.common.exception.PasswordErrorException;
import com.itsheng.pojo.dto.UserLoginDTO;
import com.itsheng.pojo.dto.UserRegisterDTO;
import com.itsheng.pojo.entity.User;
import com.itsheng.pojo.vo.UserRegisterVO;
import com.itsheng.service.mapper.UserMapper;
import com.itsheng.service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ser.jdk.NumberSerializer;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    @Override
    public UserRegisterVO register(UserRegisterDTO userRegisterDTO) {
        User user = new User();
        BeanUtils.copyProperties(userRegisterDTO,user);
        user.setCreatetime(LocalDateTime.now());
        userMapper.insert(user);
        UserRegisterVO userRegisterVO = new UserRegisterVO();
        BeanUtils.copyProperties(user,userRegisterVO);
        return userRegisterVO;
    }

    @Override
    public User login(UserLoginDTO userLoginDTO) {
      String username = userLoginDTO.getUsername();
      String password = userLoginDTO.getPassword();

      User user = userMapper.selectByusername(username);

      //账号不存在
      if(user==null)
      {
          throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
      }

      if(!user.getPassword().equals(password))
      {
         throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
      }

      return user;
    }
}
