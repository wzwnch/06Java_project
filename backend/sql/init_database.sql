-- =====================================================
-- 短链接系统数据库初始化脚本
-- 数据库：link_db
-- 字符集：utf8mb4
-- 创建日期：2026-04-25
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `link_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `link_db`;

-- =====================================================
-- 1. 用户表 (link_user)
-- 分片策略：按 username 哈希分片
-- =====================================================
CREATE TABLE IF NOT EXISTS `link_user` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花算法',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(256) NOT NULL COMMENT '密码（加密）',
    `phone` VARCHAR(256) DEFAULT NULL COMMENT '手机号（加密）',
    `mail` VARCHAR(256) DEFAULT NULL COMMENT '邮箱（加密）',
    `real_phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号（脱敏展示用）',
    `real_mail` VARCHAR(64) DEFAULT NULL COMMENT '邮箱（脱敏展示用）',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. 短链接分组表 (link_group)
-- 索引：idx_username, idx_gid
-- =====================================================
CREATE TABLE IF NOT EXISTS `link_group` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花算法',
    `gid` VARCHAR(32) NOT NULL COMMENT '分组唯一标识',
    `name` VARCHAR(64) NOT NULL COMMENT '分组名称',
    `username` VARCHAR(64) NOT NULL COMMENT '所属用户',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，默认 0',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gid` (`gid`),
    KEY `idx_username` (`username`),
    KEY `idx_gid` (`gid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链接分组表';

-- =====================================================
-- 3. 短链接表 (link_link)
-- 分片策略：按 short_code 哈希分片
-- 索引：idx_gid, idx_status
-- =====================================================
CREATE TABLE IF NOT EXISTS `link_link` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花算法',
    `short_code` VARCHAR(16) NOT NULL COMMENT '短链接码',
    `gid` VARCHAR(32) NOT NULL COMMENT '分组标识',
    `origin_url` VARCHAR(2048) NOT NULL COMMENT '原始 URL',
    `favicon_url` VARCHAR(512) DEFAULT NULL COMMENT '网站图标 URL',
    `title` VARCHAR(256) DEFAULT NULL COMMENT '网站标题',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间，NULL 表示永不过期',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-回收站',
    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-正常，1-删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`),
    KEY `idx_gid` (`gid`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链接表';

-- =====================================================
-- 4. 短链接访问日志表 (link_link_stats)
-- 分片策略：按 create_time 按月分表
-- 索引：idx_short_code, idx_create_time
-- =====================================================
CREATE TABLE IF NOT EXISTS `link_link_stats` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花算法',
    `short_code` VARCHAR(16) NOT NULL COMMENT '短链接码',
    `gid` VARCHAR(32) NOT NULL COMMENT '分组标识',
    `pv` BIGINT NOT NULL DEFAULT 1 COMMENT 'PV 计数',
    `uv` VARCHAR(64) DEFAULT NULL COMMENT 'UV 标识（用户指纹）',
    `uip` VARCHAR(64) DEFAULT NULL COMMENT 'UIP 标识（IP）',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '访问 IP',
    `region` VARCHAR(64) DEFAULT NULL COMMENT '地区',
    `os` VARCHAR(32) DEFAULT NULL COMMENT '操作系统',
    `browser` VARCHAR(32) DEFAULT NULL COMMENT '浏览器',
    `device` VARCHAR(32) DEFAULT NULL COMMENT '设备类型',
    `network` VARCHAR(32) DEFAULT NULL COMMENT '网络类型',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_short_code` (`short_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链接访问日志表';

-- =====================================================
-- 5. 短链接统计汇总表 (link_link_stats_today)
-- 唯一索引：uk_short_code_date
-- =====================================================
CREATE TABLE IF NOT EXISTS `link_link_stats_today` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `short_code` VARCHAR(16) NOT NULL COMMENT '短链接码',
    `gid` VARCHAR(32) NOT NULL COMMENT '分组标识',
    `date` DATE NOT NULL COMMENT '统计日期',
    `pv` BIGINT NOT NULL DEFAULT 0 COMMENT '当日 PV',
    `uv` BIGINT NOT NULL DEFAULT 0 COMMENT '当日 UV',
    `uip` BIGINT NOT NULL DEFAULT 0 COMMENT '当日 UIP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code_date` (`short_code`, `date`),
    KEY `idx_gid` (`gid`),
    KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链接统计汇总表';

-- =====================================================
-- 初始化说明
-- =====================================================
-- 1. 布隆过滤器数据初始化：
--    - 用户名布隆过滤器：应用启动时从 link_user 表加载所有 username 到 Redis Bloom
--    - 短链接码布隆过滤器：应用启动时从 link_link 表加载所有 short_code 到 Redis Bloom
--    - 实现方式：在 Spring Boot 应用启动监听器中初始化
--
-- 2. Redis Key 设计：
--    - link:info:{shortCode} - 短链接信息缓存
--    - link:user:bloom - 用户名布隆过滤器
--    - link:short:bloom - 短链接码布隆过滤器
--
-- 3. 分库分表配置：
--    - link_user 表：按 username 哈希分片
--    - link_link 表：按 short_code 哈希分片
--    - link_link_stats 表：按 create_time 按月分表
--    - 使用 ShardingSphere-JDBC 实现分库分表
-- =====================================================
