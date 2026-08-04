package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小程序用户 Mapper。
 */
@Mapper
public interface MobiUserMapper extends BaseMapper<MobiUser> {
}
