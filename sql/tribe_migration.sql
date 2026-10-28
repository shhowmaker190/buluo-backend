-- 板块(board) -> 部落(tribe) 迁移
-- 执行前已备份：sql/backup_before_tribe_20261022.sql
-- 按用户确认：删除 board 表，并删除所有已有归属板块的帖子（连同评论/点赞/通知/禁言记录）

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清理待删除帖子及其关联数据
DELETE FROM comment_like
WHERE comment_id IN (SELECT id FROM comment
                     WHERE post_id IN (SELECT id FROM post WHERE board_id IS NOT NULL));
DELETE FROM comment
WHERE post_id IN (SELECT id FROM post WHERE board_id IS NOT NULL);
DELETE FROM comment_restriction
WHERE post_id IN (SELECT id FROM post WHERE board_id IS NOT NULL);
DELETE FROM notification
WHERE post_id IN (SELECT id FROM post WHERE board_id IS NOT NULL);
DELETE FROM post WHERE board_id IS NOT NULL;

-- 2. 删除板块表
DROP TABLE IF EXISTS board;

-- 3. post 表改用 tribe_id
ALTER TABLE post DROP COLUMN board_id;
ALTER TABLE post ADD COLUMN tribe_id BIGINT NULL AFTER user_id;
ALTER TABLE post ADD INDEX idx_post_tribe (tribe_id);

-- 4. 部落表
CREATE TABLE IF NOT EXISTS tribe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    avatar VARCHAR(255),
    creator_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核,1=已通过,2=已拒绝',
    reject_reason VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tribe_name (name),
    INDEX idx_tribe_status (status),
    INDEX idx_tribe_creator (creator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. 部落成员表（role: LEADER 首领 / DEPUTY 副首领 / MEMBER 成员）
CREATE TABLE IF NOT EXISTS tribe_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tribe_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    muted TINYINT NOT NULL DEFAULT 0 COMMENT '1=在该部落内被禁言',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tribe_user (tribe_id, user_id),
    INDEX idx_member_user (user_id),
    INDEX idx_member_tribe_role (tribe_id, role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
