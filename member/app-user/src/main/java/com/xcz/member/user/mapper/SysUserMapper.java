package com.xcz.member.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.user.domain.SysUser;
import com.xcz.member.user.domain.dto.UserDto;
import com.xcz.member.user.domain.vo.UserInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户信息 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     * @param userName 用户名
     * @return 用户信息
     */
    SysUser selectUserByUserName(@Param("userName") String userName);


    /**
     * 查询用户角色列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectUserRoleIds(@Param("userId") Long userId);

    /**
     * 分页查询用户列表
     * @param page 分页对象
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<UserInfoVO> selectPageList(Page<UserInfoVO> page, @Param("dto") UserDto dto);
}
