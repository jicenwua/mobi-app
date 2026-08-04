package com.xcz.member.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.utils.SecurityUtils;
import com.xcz.member.user.domain.SysMenu;
import com.xcz.member.user.domain.SysRole;
import com.xcz.member.user.domain.SysUser;
import com.xcz.member.user.domain.dto.RegisterDTO;
import com.xcz.member.user.domain.dto.UserDto;
import com.xcz.member.user.domain.dto.UserUpdateDto;
import com.xcz.member.user.domain.vo.UserInfoVO;
import com.xcz.member.user.mapper.SysUserMapper;
import com.xcz.member.user.service.SysMenuService;
import com.xcz.member.user.service.SysRoleService;
import com.xcz.member.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleService roleService;
    private final SysMenuService menuService;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);

        // 获取角色列表
        List<String> roles = getUserRoles(userId);
        userInfoVO.setRoles(roles);


        // 获取菜单列表
        List<SysMenu> menus = menuService.getMenusByUserId(userId);
        userInfoVO.setMenus(menuService.buildMenuTree(menus));
        // 获取权限列表
        userInfoVO.setPermissions(menus.stream().map(SysMenu::getPerms).filter(StringUtils::isNotEmpty).collect(Collectors.toList()));
        return userInfoVO;
    }

    /**
     * 获取用户角色标识列表
     *
     * @param userId 用户id
     * @return 角色标识列表
     */
    private List<String> getUserRoles(Long userId) {
        List<SysRole> roles = roleService.getRolesByUserId(userId);
        if (CollectionUtils.isEmpty(roles)) {
            return new ArrayList<>();
        }
        return roles.stream()
                .map(SysRole::getRoleKey)
                .collect(Collectors.toList());
    }

    @Override
    public SysUser getUserByUserName(String userName) {
        return getBaseMapper().selectUserByUserName(userName);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserUpdateDto userUpdateDto) {
        if (userUpdateDto.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }

        // 校验用户是否存在
        SysUser existUser = getById(userUpdateDto.getUserId());
        if (existUser == null) {
            throw new ServiceException("用户不存在");
        }

        try {
            //如果修改密码，则进行加密
            if (StringUtils.isNotEmpty(userUpdateDto.getPassword())) {
                userUpdateDto.setPassword(passwordEncoder.encode(userUpdateDto.getPassword()));
            }

            boolean updateRole = userUpdateDto.getRoleIds() != null && !userUpdateDto.getRoleIds().isEmpty();

            //构建更新sql - 只更新非空字段
            LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SysUser::getUserId, userUpdateDto.getUserId())
                    .set(StringUtils.isNotEmpty(userUpdateDto.getNickname()),
                            SysUser::getNickName, userUpdateDto.getNickname())
                    .set(StringUtils.isNotEmpty(userUpdateDto.getEmail()),
                            SysUser::getEmail, userUpdateDto.getEmail())
                    .set(StringUtils.isNotEmpty(userUpdateDto.getPhonenumber()),
                            SysUser::getPhonenumber, userUpdateDto.getPhonenumber())
                    .set(userUpdateDto.getSex() != null,
                            SysUser::getSex, userUpdateDto.getSex())
                    .set(userUpdateDto.getStatus() != null,
                            SysUser::getStatus, userUpdateDto.getStatus())
                    .set(StringUtils.isNotEmpty(userUpdateDto.getRemark()),
                            SysUser::getRemark, userUpdateDto.getRemark())
                    .set(StringUtils.isNotEmpty(userUpdateDto.getPassword()),
                            SysUser::getPassword, userUpdateDto.getPassword())
            ;

            // 判断是否有需要更新的字段
            if (StringUtils.isNotEmpty(wrapper.getSqlSet()) || updateRole) {
                wrapper.set(SysUser::getUpdateBy, SecurityUtils.getUserId());
                update(wrapper);
            }

            // 更新角色关系（空列表表示清空所有角色）
            if (updateRole) {
                roleService.updateUserRole(userUpdateDto.getUserId(), userUpdateDto.getRoleIds());
            }

            return true;
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            throw new ServiceException("更新用户信息失败");
        }
    }

    @Override
    public Page<UserInfoVO> getPageList(UserDto userDto) {
        Page<UserInfoVO> page = new Page<>(userDto.getPageNum(), userDto.getPageSize());
        return sysUserMapper.selectPageList(page, userDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUser(RegisterDTO registerDTO) {
        if (registerDTO.getUserName() == null) {
            throw new ServiceException("用户名不能为空");
        }
        if (registerDTO.getUserName().length() < 6) {
            throw new ServiceException("用户名长度不能小于6");
        }
        if (registerDTO.getPassword() == null) {
            throw new ServiceException("密码不能为空");
        }
        if (registerDTO.getPassword().length() < 6) {
            throw new ServiceException("密码长度不能小于6");
        }
        if (registerDTO.getNickName() == null) {
            throw new ServiceException("昵称不能为空");
        }
        if (registerDTO.getRoleIds() == null || registerDTO.getRoleIds().isEmpty()) {
            throw new ServiceException("请选择角色");
        }
        registerDTO.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        SysUser newUser = SysUser.builder()
                .userName(registerDTO.getUserName())
                .nickName(registerDTO.getNickName())
                .password(registerDTO.getPassword())
                .email(registerDTO.getEmail())
                .phonenumber(registerDTO.getPhonenumber())
                .sex(registerDTO.getSex())
                .status("0")
                .build();
        newUser.setCreateBy(SecurityUtils.getUsername());
        newUser.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(newUser);
        roleService.updateUserRole(newUser.getUserId(), registerDTO.getRoleIds());
        return true;
    }
}
