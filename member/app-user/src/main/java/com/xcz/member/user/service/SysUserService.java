package com.xcz.member.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.member.user.domain.SysUser;
import com.xcz.member.user.domain.dto.RegisterDTO;
import com.xcz.member.user.domain.dto.UserDto;
import com.xcz.member.user.domain.dto.UserUpdateDto;
import com.xcz.member.user.domain.vo.UserInfoVO;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {


    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 根据用户名查询用户
     * @param userName 用户名
     * @return 用户信息
     */
    SysUser getUserByUserName(String userName);


    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUserInfo(UserUpdateDto user);

    /**
     * 分页查询用户列表
     * @param userDto 查询条件（包含分页参数）
     * @return 分页结果
     */
    Page<UserInfoVO> getPageList(UserDto userDto);

    /**
     * 新增用户
     * @param registerDTO   新增用户信息
     * @return  是否成功
     */
    boolean addUser(RegisterDTO registerDTO);
}
