-- ======================================================
-- 在线校园问答互助系统 - 数据库建表脚本
-- 数据库：MySQL 8.0
-- 字符集：utf8mb4
-- ======================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `campus_qa` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `campus_qa`;

-- ------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号（登录账号）',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname`    VARCHAR(50)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `bio`         VARCHAR(200) DEFAULT NULL COMMENT '个人简介',
  `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0普通用户 1管理员',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '账号状态：0正常 1禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------
-- 2. 问题分类表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序，越大越靠前',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题分类表';

-- ------------------------------------------------------
-- 3. 标签表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(30)  NOT NULL COMMENT '标签名',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ------------------------------------------------------
-- 4. 问题表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL COMMENT '提问者ID',
  `title`        VARCHAR(150) NOT NULL COMMENT '问题标题（5-150字符）',
  `content`      TEXT         NOT NULL COMMENT '问题内容（Markdown，最大10000字符）',
  `category_id`  BIGINT       NOT NULL COMMENT '分类ID',
  `view_count`   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count`   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `answer_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '回答数',
  `is_top`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：0普通 1置顶',
  `is_adopted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已采纳回答：0未采纳 1已采纳（可选标记，便于列表展示"已解决"）',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1已关闭 2已删除',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status_top` (`status`, `is_top`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题表';

-- ------------------------------------------------------
-- 5. 问题-标签关联表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `question_tag`;
CREATE TABLE `question_tag` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT NOT NULL COMMENT '问题ID',
  `tag_id`      BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_tag` (`question_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题标签关联表';

-- ------------------------------------------------------
-- 6. 回答表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT       NOT NULL COMMENT '所属问题ID',
  `user_id`     BIGINT       NOT NULL COMMENT '回答者ID',
  `content`     TEXT         NOT NULL COMMENT '回答内容（Markdown）',
  `like_count`  INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_accepted` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已采纳：0未采纳 1已采纳',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1已删除',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回答表';

-- ------------------------------------------------------
-- 7. 点赞/踩记录表
--    target_type：目标类型 1问题 2回答（与接口参数 targetType 一致）
--    action_type：行为类型 1点赞 2踩
--    唯一键 (user_id, target_id, target_type)：
--      保证同一用户对同一目标只有一条记录 → 点赞/踩天然互斥，
--      同时允许"对问题1点赞"和"对回答1点赞"共存。
-- ------------------------------------------------------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `target_id`   BIGINT   NOT NULL COMMENT '目标ID（问题ID或回答ID）',
  `target_type`  TINYINT  NOT NULL COMMENT '目标类型：1问题 2回答',
  `action_type`  TINYINT  NOT NULL COMMENT '行为类型：1点赞 2踩',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
  KEY `idx_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞踩记录表';

-- ------------------------------------------------------
-- 8. 收藏表
-- ------------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `question_id` BIGINT   NOT NULL COMMENT '问题ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ======================================================
-- 初始化数据
-- ======================================================

-- 默认分类
INSERT INTO `category` (`name`, `description`, `sort_order`) VALUES
('技术交流', '编程技术讨论', 50),
('学习资料', '学习资源分享', 40),
('校园生活', '校园日常交流', 30),
('求职招聘', '实习就业信息', 20),
('二手交易', '闲置物品交易', 10);

-- 默认管理员账号（可选）
-- 说明：password 必须是 BCrypt 加密后的密文，请使用项目里的 BCrypt 工具类生成后替换下面的占位值。
-- 例：明文 123456 的 BCrypt 值形如 $2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-- INSERT INTO `user` (`phone`, `password`, `nickname`, `role`) VALUES
-- ('13800000000', '$2a$10$请替换为真实BCrypt密文', '系统管理员', 1);
