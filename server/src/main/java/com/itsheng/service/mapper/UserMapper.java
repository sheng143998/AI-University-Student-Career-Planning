package com.itsheng.service.mapper;
import com.itsheng.pojo.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("insert into users(user_name,user_password,sex,user_image) values(#{username},#{password},#{sex},#{userImage})")
    void insert(User user);

    @Select("select id, user_name as username, user_password as password, sex, user_image as userImage, create_time as createtime from users where user_name=#{username}")
    User selectByusername(@Param("username") String username);

    @Select("select id, user_name as username, user_password as password, sex, user_image as userImage, create_time as createtime from users where id=#{id}")
    User selectById(@Param("id") Long id);

    @Update("UPDATE users SET user_name = #{username}, user_password = #{password}, user_image = #{userImage}, sex = #{sex} WHERE id = #{id}")
    void update(User user);

}
