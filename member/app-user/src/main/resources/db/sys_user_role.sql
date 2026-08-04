/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : min_admin

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 14/07/2026 17:54:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `union_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户唯一id',
  `open_id` varbinary(255) NULL DEFAULT NULL COMMENT '用户微信唯一id',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_user_name`(`user_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 111 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (1, 2);
INSERT INTO `sys_role_menu` VALUES (1, 3);
INSERT INTO `sys_role_menu` VALUES (1, 4);
INSERT INTO `sys_role_menu` VALUES (1, 6);
INSERT INTO `sys_role_menu` VALUES (1, 7);
INSERT INTO `sys_role_menu` VALUES (1, 100);
INSERT INTO `sys_role_menu` VALUES (1, 101);
INSERT INTO `sys_role_menu` VALUES (1, 102);
INSERT INTO `sys_role_menu` VALUES (1, 103);
INSERT INTO `sys_role_menu` VALUES (1, 104);
INSERT INTO `sys_role_menu` VALUES (1, 105);
INSERT INTO `sys_role_menu` VALUES (1, 106);
INSERT INTO `sys_role_menu` VALUES (1, 107);
INSERT INTO `sys_role_menu` VALUES (1, 200);
INSERT INTO `sys_role_menu` VALUES (1, 201);
INSERT INTO `sys_role_menu` VALUES (1, 202);
INSERT INTO `sys_role_menu` VALUES (1, 203);
INSERT INTO `sys_role_menu` VALUES (1, 204);
INSERT INTO `sys_role_menu` VALUES (1, 300);
INSERT INTO `sys_role_menu` VALUES (1, 301);
INSERT INTO `sys_role_menu` VALUES (1, 302);
INSERT INTO `sys_role_menu` VALUES (1, 303);
INSERT INTO `sys_role_menu` VALUES (1, 500);
INSERT INTO `sys_role_menu` VALUES (1, 501);
INSERT INTO `sys_role_menu` VALUES (1, 2100);
INSERT INTO `sys_role_menu` VALUES (1, 2101);
INSERT INTO `sys_role_menu` VALUES (1, 2102);
INSERT INTO `sys_role_menu` VALUES (1, 2103);
INSERT INTO `sys_role_menu` VALUES (1, 2104);
INSERT INTO `sys_role_menu` VALUES (1, 2110);
INSERT INTO `sys_role_menu` VALUES (1, 2120);
INSERT INTO `sys_role_menu` VALUES (1, 2121);
INSERT INTO `sys_role_menu` VALUES (1, 2122);
INSERT INTO `sys_role_menu` VALUES (1, 2123);
INSERT INTO `sys_role_menu` VALUES (1, 2124);
INSERT INTO `sys_role_menu` VALUES (1, 2130);
INSERT INTO `sys_role_menu` VALUES (1, 2131);
INSERT INTO `sys_role_menu` VALUES (1, 2132);
INSERT INTO `sys_role_menu` VALUES (1, 2133);
INSERT INTO `sys_role_menu` VALUES (1, 2134);
INSERT INTO `sys_role_menu` VALUES (1, 2150);
INSERT INTO `sys_role_menu` VALUES (1, 2151);
INSERT INTO `sys_role_menu` VALUES (1, 2152);
INSERT INTO `sys_role_menu` VALUES (1, 2160);
INSERT INTO `sys_role_menu` VALUES (1, 2161);
INSERT INTO `sys_role_menu` VALUES (1, 2162);
INSERT INTO `sys_role_menu` VALUES (1, 2200);
INSERT INTO `sys_role_menu` VALUES (1, 2201);
INSERT INTO `sys_role_menu` VALUES (1, 2202);
INSERT INTO `sys_role_menu` VALUES (1, 2203);
INSERT INTO `sys_role_menu` VALUES (1, 2204);
INSERT INTO `sys_role_menu` VALUES (1, 2205);
INSERT INTO `sys_role_menu` VALUES (1, 2210);
INSERT INTO `sys_role_menu` VALUES (1, 5000);
INSERT INTO `sys_role_menu` VALUES (1, 5001);
INSERT INTO `sys_role_menu` VALUES (1, 5002);
INSERT INTO `sys_role_menu` VALUES (1, 5003);
INSERT INTO `sys_role_menu` VALUES (1, 5004);
INSERT INTO `sys_role_menu` VALUES (1, 5006);
INSERT INTO `sys_role_menu` VALUES (1, 5007);
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (2, 6);
INSERT INTO `sys_role_menu` VALUES (2, 100);
INSERT INTO `sys_role_menu` VALUES (2, 101);
INSERT INTO `sys_role_menu` VALUES (2, 102);
INSERT INTO `sys_role_menu` VALUES (2, 200);
INSERT INTO `sys_role_menu` VALUES (2, 201);
INSERT INTO `sys_role_menu` VALUES (2, 300);
INSERT INTO `sys_role_menu` VALUES (2, 301);
INSERT INTO `sys_role_menu` VALUES (2, 500);
INSERT INTO `sys_role_menu` VALUES (101, 5008);
INSERT INTO `sys_role_menu` VALUES (101, 5009);
INSERT INTO `sys_role_menu` VALUES (101, 5011);
INSERT INTO `sys_role_menu` VALUES (101, 5012);
INSERT INTO `sys_role_menu` VALUES (101, 5013);
INSERT INTO `sys_role_menu` VALUES (101, 5014);
INSERT INTO `sys_role_menu` VALUES (101, 5015);
INSERT INTO `sys_role_menu` VALUES (101, 5016);
INSERT INTO `sys_role_menu` VALUES (101, 5017);
INSERT INTO `sys_role_menu` VALUES (101, 5018);
INSERT INTO `sys_role_menu` VALUES (101, 5019);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号，每次更新递增',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE INDEX `uk_role_key`(`role_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '0', 8, 0, 'admin', '2026-05-08 11:24:41', NULL, '2026-07-06 15:12:51', '超级管理员嘻嘻');
INSERT INTO `sys_role` VALUES (2, '普通角色', 'common', 2, '0', 3, 0, 'admin', '2026-05-08 11:24:41', NULL, '2026-07-06 15:12:48', '普通角色');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由参数',
  `is_frame` int NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5020 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 1, '/system', NULL, NULL, 1, 0, 'M', '0', '0', '', 'Setting', 'admin', '2026-05-08 11:24:41', '1', '2026-05-14 16:17:59', '系统管理目录', 0);
INSERT INTO `sys_menu` VALUES (2, '用户管理', 1, 1, '/system/user', 'system/user/index', NULL, 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2026-05-08 11:24:41', '', NULL, '用户管理菜单', 0);
INSERT INTO `sys_menu` VALUES (3, '角色管理', 1, 2, '/system/role', 'system/role/index', NULL, 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2026-05-08 11:24:41', '', NULL, '角色管理菜单', 0);
INSERT INTO `sys_menu` VALUES (4, '菜单管理', 1, 3, '/system/menu', 'system/menu/index', NULL, 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2026-05-08 11:24:41', '', NULL, '菜单管理菜单', 0);
INSERT INTO `sys_menu` VALUES (6, '日志管理', 1, 5, '/system/log', 'system/log/index', NULL, 1, 0, 'C', '0', '0', 'system:log:list', 'log', 'admin', '2026-05-08 11:24:41', 'admin', '2026-07-01 20:48:46', '日志管理菜单', 0);
INSERT INTO `sys_menu` VALUES (7, '小程序权限项', 1, 6, '/system/mobi-role', 'system/mobiRole/index', NULL, 1, 0, 'C', '0', '0', 'system:mobi:role:list', 'key', 'admin', '2026-06-03 16:00:00', 'admin', '2026-07-01 20:50:19', '小程序接口权限配置（mobi_role）', 0);
INSERT INTO `sys_menu` VALUES (100, '用户查询', 2, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:query', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (101, '用户新增', 2, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:add', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (102, '用户修改', 2, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:edit', '', 'admin', '2026-05-08 11:24:41', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (103, '用户删除', 2, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:remove', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (104, '权限项查询', 7, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:role:query', '#', 'admin', '2026-06-03 16:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (105, '权限项新增', 7, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:role:add', '#', 'admin', '2026-06-03 16:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (106, '权限项修改', 7, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:role:edit', '#', 'admin', '2026-06-03 16:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (107, '权限项删除', 7, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:role:remove', '#', 'admin', '2026-06-03 16:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (200, '角色查询', 3, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:query', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (201, '角色新增', 3, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:add', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (202, '角色修改', 3, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:edit', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (203, '角色删除', 3, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:remove', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (204, '分配权限', 3, 5, '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:assign', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (300, '菜单查询', 4, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:query', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (301, '菜单新增', 4, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:add', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (302, '菜单修改', 4, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:edit', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (303, '菜单删除', 4, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:remove', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (500, '日志查询', 6, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:log:query', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (501, '日志删除', 6, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:log:remove', '', 'admin', '2026-05-08 11:24:41', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2100, '用户管理', 0, 4, '/user', NULL, NULL, 1, 0, 'M', '0', '0', '', 'user', 'admin', '2026-05-26 17:57:58', '', NULL, '店铺会员数据', 0);
INSERT INTO `sys_menu` VALUES (2101, '用户积分', 2100, 1, '/user/points', 'user/points/index', NULL, 1, 0, 'C', '0', '0', 'system:user:points:list', 'coin', 'admin', '2026-05-26 17:57:58', 'admin', '2026-06-03 16:00:00', '店铺用户积分', 0);
INSERT INTO `sys_menu` VALUES (2102, '用户消费记录', 2100, 2, '/user/consume', 'user/consume/index', NULL, 1, 0, 'C', '0', '0', 'system:user:consume:list', 'documentation', 'admin', '2026-05-26 17:57:58', 'admin', '2026-06-03 16:00:00', '店铺消费记录', 0);
INSERT INTO `sys_menu` VALUES (2103, '消费详情', 2102, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:consume:query', '#', 'admin', '2026-05-26 17:57:58', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (2104, '调整积分', 2101, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:points:adjust', '#', 'admin', '2026-06-24 16:00:00', '', NULL, '手动增减基础/赠送积分', 0);
INSERT INTO `sys_menu` VALUES (2110, '商品管理', 5000, 5, '/product/manage', 'product/management/index', NULL, 1, 0, 'C', '0', '0', 'system:product:list', 'goods', 'admin', '2026-05-26 17:57:58', 'admin', '2026-06-03 16:00:00', '店铺商品', 0);
INSERT INTO `sys_menu` VALUES (2120, '店铺活动', 5000, 6, '/shop/activity', 'shop/activity/index', NULL, 1, 0, 'C', '0', '0', 'system:shop:activity:list', 'star', 'admin', '2026-06-03 17:00:00', '', NULL, '店铺活动管理', 0);
INSERT INTO `sys_menu` VALUES (2121, '活动查询', 2120, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:activity:query', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2122, '活动新增', 2120, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:activity:add', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2123, '活动修改', 2120, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:activity:edit', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2124, '活动删除', 2120, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:activity:remove', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2130, '店铺折扣券', 5000, 7, '/shop/coupon', 'shop/coupon/index', NULL, 1, 0, 'C', '0', '0', 'system:shop:coupon:list', 'ticket', 'admin', '2026-06-03 17:00:00', '', NULL, '店铺折扣券模板', 0);
INSERT INTO `sys_menu` VALUES (2131, '折扣券查询', 2130, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:coupon:query', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2132, '折扣券新增', 2130, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:coupon:add', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2133, '折扣券修改', 2130, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:coupon:edit', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2134, '折扣券删除', 2130, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:coupon:remove', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2150, '用户折扣券', 2100, 4, '/user/coupon', 'user/coupon/index', NULL, 1, 0, 'C', '0', '0', 'system:user:coupon:list', 'ticket', 'admin', '2026-06-03 17:00:00', '', NULL, '用户持券管理', 0);
INSERT INTO `sys_menu` VALUES (2151, '发放折扣券', 2150, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:coupon:grant', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2152, '删除用户券', 2150, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:coupon:remove', '#', 'admin', '2026-06-03 17:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2160, '小程序用户管理', 2100, 5, '/user/mobi-user', 'user/mobiUser/index', NULL, 1, 0, 'C', '0', '0', 'system:mobi:user:list', 'peoples', 'admin', '2026-06-03 17:55:00', '', NULL, 'mobi_user 小程序用户', 0);
INSERT INTO `sys_menu` VALUES (2161, '用户查询', 2160, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:user:query', '#', 'admin', '2026-06-03 17:55:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2162, '用户修改', 2160, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:user:edit', '#', 'admin', '2026-06-03 17:55:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2200, '工单管理', 0, 6, '/ticket', NULL, NULL, 1, 0, 'M', '0', '0', '', 'service', 'admin', '2026-07-01 10:00:00', '', NULL, '客服工单目录', 0);
INSERT INTO `sys_menu` VALUES (2201, '工单列表', 2200, 1, '/ticket/list', 'ticket/list/index', NULL, 1, 0, 'C', '0', '0', 'system:ticket:list', 'list', 'admin', '2026-07-01 10:00:00', '', NULL, '全部工单', 0);
INSERT INTO `sys_menu` VALUES (2202, '认领工单', 2201, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:ticket:claim', '#', 'admin', '2026-07-01 10:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2203, '工单详情', 2201, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:ticket:query', '#', 'admin', '2026-07-01 10:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2204, '工单回复', 2201, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:ticket:reply', '#', 'admin', '2026-07-01 10:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2205, '完成工单', 2201, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:ticket:complete', '#', 'admin', '2026-07-01 10:00:00', '', NULL, '', 0);
INSERT INTO `sys_menu` VALUES (2210, '我的工单', 2200, 2, '/ticket/mine', 'ticket/mine/index', NULL, 1, 0, 'C', '0', '0', 'system:ticket:mine', 'user', 'admin', '2026-07-01 10:00:00', '', NULL, '当前客服处理中的工单', 0);
INSERT INTO `sys_menu` VALUES (5000, '店铺管理', 0, 3, '/shop', NULL, NULL, 1, 0, 'M', '0', '0', '', 'shop', 'admin', '2026-05-14 15:11:58', '', NULL, '店铺管理', 0);
INSERT INTO `sys_menu` VALUES (5001, '店铺审核', 5000, 1, '/shop/audit', 'shop/audit/index', NULL, 1, 0, 'C', '0', '0', 'system:shop:audit:list', 'eye-open', 'admin', '2026-05-14 15:11:58', 'admin', '2026-06-03 16:00:00', '待审与驳回列表', 0);
INSERT INTO `sys_menu` VALUES (5002, '店铺列表', 5000, 2, '/shop/manage', 'shop/management/index', NULL, 1, 0, 'C', '0', '0', 'system:shop:list', 'list', 'admin', '2026-05-14 15:11:58', 'admin', '2026-06-03 16:00:00', '全部店铺', 0);
INSERT INTO `sys_menu` VALUES (5003, '提交审核', 5001, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:audit', '#', 'admin', '2026-05-14 15:11:58', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (5004, '店铺详情(审)', 5001, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:query', '#', 'admin', '2026-05-14 15:11:58', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (5006, '店铺详情', 5002, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:query', '#', 'admin', '2026-05-14 15:11:58', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (5007, '新增店铺', 5002, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'system:shop:add', '#', 'admin', '2026-05-14 15:11:58', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (5008, '小程序权限', 0, 5, '/app', '', NULL, 1, 0, 'M', '0', '0', '', 'Avatar', 'admin', '2026-05-17 16:57:51', 'admin', '2026-06-03 16:00:00', '后台角色分配用（与 min_app.mobi_role.wx:* 对应）', 0);
INSERT INTO `sys_menu` VALUES (5009, '新增店铺', 5008, 1, '/app/shop/add', '', NULL, 1, 0, 'C', '0', '0', 'system:mobi:shop:add', '', 'admin', '2026-05-17 16:58:57', 'admin', '2026-06-03 16:00:00', '', 0);
INSERT INTO `sys_menu` VALUES (5011, '店铺Tab', 5008, 0, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:shop:list', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '底部店铺Tab、店主店铺列表', 0);
INSERT INTO `sys_menu` VALUES (5012, '会员访问', 5008, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:user', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '已加入店铺列表与店内详情', 0);
INSERT INTO `sys_menu` VALUES (5013, '店铺预览', 5008, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:user:query', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '扫码/输入店铺代码预览', 0);
INSERT INTO `sys_menu` VALUES (5014, '加入店铺', 5008, 5, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:user:enter', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '顾客加入店铺', 0);
INSERT INTO `sys_menu` VALUES (5015, '活动查询', 5008, 6, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:activity:query', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '活动详情与进行中活动', 0);
INSERT INTO `sys_menu` VALUES (5016, '活动编辑', 5008, 7, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:activity:edit', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '保存或结束活动', 0);
INSERT INTO `sys_menu` VALUES (5017, '活动删除', 5008, 8, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:activity:remove', '#', 'admin', '2026-05-27 17:11:38', 'admin', '2026-06-03 16:00:00', '删除活动及规则', 0);
INSERT INTO `sys_menu` VALUES (5018, '扫码收银', 5008, 9, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:shop:consume', '#', 'admin', '2026-05-30 11:16:32', 'admin', '2026-06-03 16:00:00', '店员扫码扣积分、校验付款码', 0);
INSERT INTO `sys_menu` VALUES (5019, '付款码', 5008, 10, '', '', NULL, 1, 0, 'F', '0', '0', 'system:mobi:pay:qrcode', '#', 'admin', '2026-05-30 11:16:32', 'admin', '2026-06-03 16:00:00', '会员生成付款二维码', 0);

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `log_type` int NOT NULL COMMENT '日志类型（1登录日志 2操作日志）',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态（0正常 1异常）',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户账号',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作模块',
  `operation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作类型',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求方法',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求方式（GET POST PUT DELETE）',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求URL',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '返回结果',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `execute_time` bigint NULL DEFAULT 0 COMMENT '执行时长（毫秒）',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作IP地址',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作地点',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_log_type`(`log_type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统日志表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
