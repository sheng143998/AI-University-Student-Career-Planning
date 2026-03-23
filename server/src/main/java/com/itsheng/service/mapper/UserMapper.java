package com.itsheng.service.mapper;
import com.itsheng.pojo.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("insert into users(user_name,user_password,sex,user_image) values(#{username},#{password},#{sex},#{userimage})")
    void insert(User user);

    @Select("select id, user_name as username, user_password as password, sex, user_image as userimage, create_time as createtime from users where user_name=#{username}")
    User selectByusername(@Param("username") String username);
}
