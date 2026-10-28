-- 创建通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '通知接收者ID',
    `vote_id` BIGINT COMMENT '关联的投票ID',
    `comment_id` BIGINT COMMENT '关联的评论ID',
    `type` VARCHAR(50) DEFAULT NULL COMMENT '通知类型：VOTE、LIKE',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `from_username` VARCHAR(100) DEFAULT NULL COMMENT '触发通知的用户名',
    `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    `email_sent` TINYINT(1) DEFAULT 0 COMMENT '邮件是否已发送',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_vote_id` (`vote_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
