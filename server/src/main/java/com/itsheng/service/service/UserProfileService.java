package com.itsheng.service.service;

import com.itsheng.pojo.dto.UserProfileUpdateDTO;
import com.itsheng.pojo.vo.UserProfileDetailVO;
import com.itsheng.pojo.vo.UserProfileVO;

/**
 * 用户档案 Service 接口
 */
public interface UserProfileService {

    /**
     * 获取个人档案概览
     * @return 档案概览 VO
     */
    UserProfileVO getProfile();

    /**
     * 更新个人档案概览
     * @param dto 更新参数
     * @return 是否更新成功
     */
    boolean updateProfile(UserProfileUpdateDTO dto);

    /**
     * 获取详细档案
     * @return 详细档案 VO
     */
    UserProfileDetailVO getProfileDetail();
}
