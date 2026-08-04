package com.xcz.member.customer.domain.enums;

import com.xcz.commons.redis.extend.DatabaseEnum;
import com.xcz.commons.redis.utils.RedisUtil;
import org.redisson.api.RedissonClient;

/**
 * 小程序业务通用常量（OSS 路径、角色标识、Redis 客户端等）。
 */
public interface Constants {

    /***头像保存地址**/
    String AVATAR_DIRECTORY = "mobi/user/avatar/";
    /***店铺资料保存地址**/
    String SHOP_CERT_DIRECTORY = "mobi/shop/cert/";
    /***店铺商品图片保存地址（后接 shopId）**/
    String PRODUCT_IMAGE_DIRECTORY = "mobi/shop/product/";
    /***微信端用户角色名**/
    String USER_ROLE = "wx-app";
    /***客服工单消息中客服固定头像（与各端展示一致）**/
    String TICKET_STAFF_AVATAR_URL =
            "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

    /***使用的缓存数据库**/
    RedissonClient CACHE = RedisUtil.getRedisson(DatabaseEnum.DATABASE_1);

}
