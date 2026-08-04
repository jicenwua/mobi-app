package com.xcz.member.customer.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.customer.application.command.product.QueryProductCommand;
import com.xcz.member.customer.domain.vo.AdminProductRowVO;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 店铺商品 Mapper。
 */
@Mapper
public interface MobiShopProductMapper extends BaseMapper<MobiShopProduct> {

    /**
     * 管理后台分页：联表店铺名，按商品 ID 倒序。
     */
    Page<AdminProductRowVO> selectAdminProductPage(Page<AdminProductRowVO> page,
                                                   @Param("query") QueryProductCommand query);
}
