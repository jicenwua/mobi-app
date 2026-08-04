/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : min_app

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 14/07/2026 17:54:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for mobi_user_coupons
-- ----------------------------
DROP TABLE IF EXISTS `mobi_user_coupons`;
CREATE TABLE `mobi_user_coupons`  (
  `user_coupon_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `template_id` bigint NOT NULL COMMENT '关联模板ID',
  `status` tinyint(1) NULL DEFAULT 0 COMMENT '0-未使用 1-已使用 2-已过期',
  `used_time` datetime NULL DEFAULT NULL,
  `receive_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`user_coupon_id`) USING BTREE,
  INDEX `idx_user_shop_status`(`user_id` ASC, `shop_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户持有折扣券表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for mobi_user
-- ----------------------------
DROP TABLE IF EXISTS `mobi_user`;
CREATE TABLE `mobi_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内部全局唯一ID',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '小程序OpenID',
  `unionid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信开放平台ID',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付密码',
  `phone` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定手机号',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '微信用户' COMMENT '昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '/static/default_avatar.png' COMMENT 'OSS头像地址',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `login_ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录ip',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `idx_openid`(`openid` ASC) USING BTREE,
  UNIQUE INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_unionid`(`unionid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_ticket_message
-- ----------------------------
DROP TABLE IF EXISTS `mobi_ticket_message`;
CREATE TABLE `mobi_ticket_message`  (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `sender_type` tinyint NOT NULL COMMENT '1-用户 2-客服',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '接收方是否已读：0-未读 1-已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单消息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_ticket
-- ----------------------------
DROP TABLE IF EXISTS `mobi_ticket`;
CREATE TABLE `mobi_ticket`  (
  `ticket_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `user_id` bigint NOT NULL COMMENT '提交用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单标题',
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '初始问题描述',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待处理 1-处理中 2-已完成',
  `assigned_staff_id` bigint NULL DEFAULT NULL COMMENT '后台处理人员ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ticket_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status_staff`(`status` ASC, `assigned_staff_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服工单' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for mobi_shop_user
-- ----------------------------
DROP TABLE IF EXISTS `mobi_shop_user`;
CREATE TABLE `mobi_shop_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` int NOT NULL COMMENT '用户角色:1.店长，2.店员，3.顾客',
  `last_enter_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后进入时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_shop_user`(`shop_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_shop`(`user_id` ASC, `shop_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺用户权限' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for mobi_shop_product_category
-- ----------------------------
DROP TABLE IF EXISTS `mobi_shop_product_category`;
CREATE TABLE `mobi_shop_product_category`  (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `shop_id` bigint NOT NULL COMMENT '所属店铺ID',
  `category_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '展示顺序，越小越靠前',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`category_id`) USING BTREE,
  INDEX `idx_category_shop`(`shop_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺商品分类表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for mobi_shop_product
-- ----------------------------
DROP TABLE IF EXISTS `mobi_shop_product`;
CREATE TABLE `mobi_shop_product`  (
  `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品唯一流水ID(主键)',
  `shop_id` bigint NOT NULL COMMENT '所属店铺ID',
  `category_id` bigint NULL DEFAULT NULL COMMENT '商品分类ID',
  `product_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品描述',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品图片',
  `price` decimal(10, 2) NOT NULL COMMENT '商品价格',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '售卖状态：0-售完 1-出售中',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存数量',
  `sold_count` int NOT NULL DEFAULT 0 COMMENT '已出售数量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '商品创建时间',
  PRIMARY KEY (`product_id`) USING BTREE,
  INDEX `idx_shop`(`shop_id` ASC) USING BTREE,
  INDEX `idx_pro_shop`(`product_id` ASC, `shop_id` ASC) USING BTREE,
  INDEX `idx_product_category`(`shop_id` ASC, `category_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺商品信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_shop
-- ----------------------------
DROP TABLE IF EXISTS `mobi_shop`;
CREATE TABLE `mobi_shop`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父店铺ID（0为总店/独立店）',
  `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '店铺名称',
  `shop_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '店铺标识',
  `ratio` int NOT NULL COMMENT '积分兑换比例（1元 * 比率）不可更改',
  `picture` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '轮播图',
  `category_id` int NOT NULL COMMENT '所属行业类目ID',
  `province` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '省',
  `city` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '市',
  `district` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区/县',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `legal_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '法人姓名',
  `id_card_no` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '法人身份证号',
  `business_license_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '统一社会信用代码',
  `id_card_front` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证正面OSS路径',
  `id_card_back` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证背面OSS路径',
  `business_license_pic` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '营业执照图片路径',
  `shop_exterior` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '店铺外景图',
  `shop_interior` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '店铺内景图',
  `audit_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-通过, 2-驳回',
  `audit_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核不通过的原因',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `audit_id` bigint NULL DEFAULT NULL COMMENT '审核者ID',
  `is_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-正常',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-存在, 1-删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_shop_name`(`shop_name` ASC) USING BTREE,
  INDEX `idx_audit_status`(`audit_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_role
-- ----------------------------
DROP TABLE IF EXISTS `mobi_role`;
CREATE TABLE `mobi_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_key` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '权限key',
  `role_description` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限描述',
  `status` tinyint NULL DEFAULT 1 COMMENT '是否开启',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '修改者',
  `del_flag` int NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '微信用户权限' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mobi_role
-- ----------------------------
INSERT INTO `mobi_role` VALUES (14, 'wx:shop:list', '店铺导航栏展示', 1, '2026-06-03 15:23:24', NULL, '2026-06-12 09:20:00', 1, 0);
INSERT INTO `mobi_role` VALUES (15, 'wx:shop:add', '添加店铺、开店申请', 0, '2026-06-03 15:23:24', NULL, '2026-07-07 09:19:32', 1, 0);
INSERT INTO `mobi_role` VALUES (16, 'wx:user', '会员 Tab、店内详情、顾客读接口', 1, '2026-06-03 15:23:24', NULL, '2026-06-11 15:26:15', NULL, 0);
INSERT INTO `mobi_role` VALUES (17, 'wx:user:query', '搜索/预览店铺、店铺邀请码', 0, '2026-06-03 15:23:24', NULL, '2026-07-07 09:40:34', 1, 0);
INSERT INTO `mobi_role` VALUES (18, 'wx:user:enter', '加入店铺', 0, '2026-06-03 15:23:24', NULL, '2026-07-07 09:40:39', 1, 0);

-- ----------------------------
-- Table structure for mobi_receipt
-- ----------------------------
DROP TABLE IF EXISTS `mobi_receipt`;
CREATE TABLE `mobi_receipt`  (
  `receipt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '小票流水ID(主键)',
  `log_id` bigint NOT NULL COMMENT '订单id',
  `product_id` bigint NULL DEFAULT NULL COMMENT '商品ID（下单快照）',
  `product_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '购买的商品',
  `user_id` bigint NOT NULL COMMENT '购买的用户ID',
  `count` int NOT NULL COMMENT '购买数量',
  `price` decimal(10, 2) NOT NULL COMMENT '商品单价',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '小票生成时间/购买时间',
  PRIMARY KEY (`receipt_id`) USING BTREE,
  INDEX `idx_user_receipt`(`user_id` ASC) USING BTREE,
  INDEX `idx_product_receipt`(`product_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户消费小票明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_points_log
-- ----------------------------
DROP TABLE IF EXISTS `mobi_points_log`;
CREATE TABLE `mobi_points_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `coupon_id` bigint NULL DEFAULT NULL COMMENT '关联折扣券ID',
  `action_type` tinyint(1) NOT NULL COMMENT '1-增加 2-消耗',
  `pre_base_points` decimal(10, 2) NOT NULL COMMENT '变更前基础积分',
  `pre_bonus_points` decimal(10, 2) NOT NULL COMMENT '变更前赠送积分',
  `change_base` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '基础积分变动值',
  `change_bonus` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '赠送积分变动值',
  `after_base_points` decimal(10, 2) NOT NULL COMMENT '变更后基础积分',
  `after_bonus_points` decimal(10, 2) NOT NULL COMMENT '变更后赠送积分',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1-消费成功 2-订单取消 3-订单过期 4-积分充值',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注（后台调整等）',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '幂等请求ID（客户端UUID）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `consume_time` datetime NULL DEFAULT NULL COMMENT '核销时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  UNIQUE INDEX `uk_request_id`(`request_id` ASC) USING BTREE,
  INDEX `idx_user_shop_time`(`user_id` ASC, `shop_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分变动流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mobi_points_log
-- ----------------------------
INSERT INTO `mobi_points_log` VALUES (6, 3, 1, NULL, 2, 100.00, 0.00, -25.00, 0.00, 75.00, 0.00, 2, NULL, NULL, '2026-06-17 16:01:44', NULL);
INSERT INTO `mobi_points_log` VALUES (7, 3, 1, NULL, 2, 100.00, 0.00, -5.00, 0.00, 95.00, 0.00, 2, NULL, NULL, '2026-06-18 16:03:54', '2026-06-18 16:59:09');
INSERT INTO `mobi_points_log` VALUES (8, 3, 1, NULL, 2, 95.00, 0.00, -25.00, 0.00, 70.00, 0.00, 2, NULL, NULL, '2026-06-18 16:10:22', '2026-06-18 16:59:06');
INSERT INTO `mobi_points_log` VALUES (9, 3, 1, NULL, 2, 70.00, 0.00, -5.00, 0.00, 65.00, 0.00, 2, NULL, NULL, '2026-06-18 16:10:41', '2026-06-18 16:59:01');
INSERT INTO `mobi_points_log` VALUES (10, 3, 1, NULL, 2, 65.00, 0.00, -5.00, 0.00, 60.00, 0.00, 2, NULL, NULL, '2026-06-18 16:13:32', '2026-06-18 16:58:46');
INSERT INTO `mobi_points_log` VALUES (11, 3, 1, NULL, 2, 60.00, 0.00, -5.00, 0.00, 55.00, 0.00, 2, NULL, NULL, '2026-06-18 16:17:13', '2026-06-18 16:51:45');
INSERT INTO `mobi_points_log` VALUES (12, 3, 1, NULL, 2, 55.00, 0.00, -20.00, 0.00, 35.00, 0.00, 2, NULL, NULL, '2026-06-18 16:17:51', '2026-06-18 16:51:35');
INSERT INTO `mobi_points_log` VALUES (13, 3, 1, NULL, 2, 35.00, 0.00, -25.00, 0.00, 10.00, 0.00, 2, NULL, NULL, '2026-06-18 16:20:11', '2026-06-18 16:51:20');
INSERT INTO `mobi_points_log` VALUES (14, 3, 1, NULL, 2, 10.00, 0.00, -5.00, 0.00, 5.00, 0.00, 2, NULL, NULL, '2026-06-18 16:22:43', '2026-06-18 16:51:13');
INSERT INTO `mobi_points_log` VALUES (15, 3, 1, NULL, 2, 5.00, 0.00, -5.00, 0.00, 0.00, 0.00, 2, NULL, NULL, '2026-06-18 16:30:38', '2026-06-18 16:30:49');
INSERT INTO `mobi_points_log` VALUES (16, 3, 1, NULL, 2, 100.00, 0.00, -5.00, 0.00, 95.00, 0.00, 1, NULL, NULL, '2026-06-24 16:18:05', '2026-06-24 16:18:05');
INSERT INTO `mobi_points_log` VALUES (17, 3, 1, NULL, 1, 95.00, 0.00, 5.00, 0.00, 100.00, 0.00, 4, NULL, NULL, '2026-06-24 17:24:31', '2026-06-24 17:24:31');
INSERT INTO `mobi_points_log` VALUES (18, 3, 1, NULL, 1, 100.00, 0.00, 50.00, 0.00, 150.00, 0.00, 6, '测试', NULL, '2026-06-24 17:29:08', '2026-06-24 17:29:08');

-- ----------------------------
-- Table structure for mobi_points_account
-- ----------------------------
DROP TABLE IF EXISTS `mobi_points_account`;
CREATE TABLE `mobi_points_account`  (
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `base_points` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '当前基础积分',
  `bonus_points` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '当前赠送积分',
  `total_used_points` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '该店铺累计已使用积分',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `shop_id`) USING BTREE,
  INDEX `idx_shop_user`(`shop_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺会员积分余额表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_coupon_template
-- ----------------------------
DROP TABLE IF EXISTS `mobi_coupon_template`;
CREATE TABLE `mobi_coupon_template`  (
  `template_id` bigint NOT NULL AUTO_INCREMENT,
  `shop_id` bigint NOT NULL,
  `coupon_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `count` int NULL DEFAULT NULL COMMENT '折扣券发放数量（为空无限）',
  `type` tinyint(1) NOT NULL COMMENT '1-折扣(打折) 2-固定减免(满减)',
  `threshold_amount` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '满减阈值(满多少可用)',
  `discount_value` decimal(10, 2) NOT NULL COMMENT '打折力度(如85表示85折) 或 减免金额',
  `valid_days` int NULL DEFAULT NULL COMMENT '生效时间',
  `total_quantity` bigint NULL DEFAULT NULL COMMENT '发放数量',
  `issued_quantity` bigint NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `distribution_start_time` datetime NULL DEFAULT NULL COMMENT '开始发放时间',
  `distribution_end_time` datetime NULL DEFAULT NULL COMMENT '结束发放时间',
  `distribution_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '0-禁用 1-未开始 2-发放中 3-已结束 4-手动停止',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '为空表示永久有效',
  `statue` tinyint NULL DEFAULT 0 COMMENT '是否开启（0：开启，1；关闭）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`) USING BTREE,
  INDEX `idx_shop_id`(`shop_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '折扣券配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_activity_rule
-- ----------------------------
DROP TABLE IF EXISTS `mobi_activity_rule`;
CREATE TABLE `mobi_activity_rule`  (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则唯一流水ID(主键)',
  `activity_id` bigint NOT NULL COMMENT '关联的活动主表ID',
  `threshold_amount` int NOT NULL COMMENT '满足条件的门槛金额(单位:分。例如满100元，存10000)',
  `gift_points` int NOT NULL COMMENT '满足门槛后赠送的积分数量',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`rule_id`) USING BTREE,
  INDEX `idx_activity_id`(`activity_id` ASC) USING BTREE COMMENT '关联外键索引'
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动满赠规则明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mobi_activity
-- ----------------------------
DROP TABLE IF EXISTS `mobi_activity`;
CREATE TABLE `mobi_activity`  (
  `activity_id` bigint NOT NULL AUTO_INCREMENT COMMENT '活动唯一流水ID(主键)',
  `shop_id` bigint NOT NULL COMMENT '所属店铺ID',
  `activity_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动名称(如：五一充值狂欢)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '活动描述/公告正文',
  `kind` tinyint(1) NOT NULL DEFAULT 1 COMMENT '活动种类：1-满赠活动 2-公告',
  `activity_type` tinyint(1) NOT NULL COMMENT '活动类型：1-充值满赠，2-消费满赠',
  `start_time` datetime NOT NULL COMMENT '活动开始时间',
  `end_time` datetime NOT NULL COMMENT '活动结束时间',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '活动状态：0-未开始，1-进行中，2-已结束，3-手动停止',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`activity_id`) USING BTREE,
  INDEX `idx_shop_status_time`(`shop_id` ASC, `status` ASC, `start_time` ASC, `end_time` ASC) USING BTREE COMMENT '优化千万级下店铺查询进行中活动的性能'
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '店铺积分活动主表' ROW_FORMAT = DYNAMIC;


SET FOREIGN_KEY_CHECKS = 1;
