SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS campuscircle
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campuscircle;

CREATE TABLE IF NOT EXISTS school (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学校ID',
    name VARCHAR(64) NOT NULL COMMENT '学校名称',
    province VARCHAR(32) NOT NULL COMMENT '省份',
    city VARCHAR(32) NOT NULL COMMENT '城市',
    latitude DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    longitude DECIMAL(10, 6) NOT NULL COMMENT '经度',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_city_status (city, status),
    KEY idx_location (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校表';

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '加密后的密码',
    nickname VARCHAR(32) NOT NULL COMMENT '昵称',
    school_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属学校ID',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    bio VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_school_id (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(32) NOT NULL COMMENT '分类名称',
    code VARCHAR(32) NOT NULL COMMENT '分类编码',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-启用，1-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子分类表';

CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '发帖用户ID',
    school_id BIGINT NOT NULL COMMENT '帖子所属学校ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '正文内容',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-删除，2-隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id),
    KEY idx_school_created (school_id, created_at),
    KEY idx_category_created (category_id, created_at),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

CREATE TABLE IF NOT EXISTS post_stat (
    post_id BIGINT PRIMARY KEY COMMENT '帖子ID',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
    hot_score DOUBLE NOT NULL DEFAULT 0 COMMENT '热度分',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_hot_score (hot_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子统计表';

CREATE TABLE IF NOT EXISTS `comment` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_post_created (post_id, created_at),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

CREATE TABLE IF NOT EXISTS post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-已点赞，1-已取消',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    receiver_id BIGINT NOT NULL COMMENT '接收通知用户ID',
    sender_id BIGINT NOT NULL COMMENT '触发通知用户ID',
    post_id BIGINT DEFAULT NULL COMMENT '关联帖子ID',
    comment_id BIGINT DEFAULT NULL COMMENT '关联评论ID',
    type TINYINT NOT NULL COMMENT '通知类型：1-评论，2-点赞',
    event_key VARCHAR(128) NOT NULL COMMENT '业务事件幂等键',
    content VARCHAR(255) NOT NULL COMMENT '通知内容',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0-未读，1-已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_event_key (event_key),
    KEY idx_receiver_read_created (receiver_id, read_status, created_at),
    KEY idx_receiver_created (receiver_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知表';
