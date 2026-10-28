-- 部落闲聊：由投票系统迁移为论坛系统
SET FOREIGN_KEY_CHECKS = 0;

-- 板块表
CREATE TABLE IF NOT EXISTS board (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_board_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO board (name, description)
VALUES ('闲聊区', '随便聊聊，分享日常'),
       ('技术交流', '编程、数码与经验分享'),
       ('校园生活', '校园里的那些事'),
       ('游戏动漫', '游戏与二次元同好聚集地'),
       ('二手交易', '闲置物品交流')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 投票表 -> 帖子表
RENAME TABLE vote TO post;

ALTER TABLE post
    CHANGE COLUMN description content TEXT NULL,
    ADD COLUMN board_id BIGINT NULL AFTER user_id,
    DROP COLUMN end_time;

UPDATE post SET board_id = (SELECT id FROM board ORDER BY id LIMIT 1) WHERE board_id IS NULL;

ALTER TABLE post MODIFY COLUMN board_id BIGINT NOT NULL;
ALTER TABLE post ADD INDEX idx_post_board (board_id);
ALTER TABLE post ADD INDEX idx_post_status_created (status, created_at);

-- 评论：支持楼中楼
ALTER TABLE comment
    CHANGE COLUMN vote_id post_id BIGINT NOT NULL,
    ADD COLUMN parent_id BIGINT NULL AFTER post_id,
    ADD INDEX idx_comment_post (post_id),
    ADD INDEX idx_comment_parent (parent_id);

-- 通知与评论限制关联字段改名
ALTER TABLE notification CHANGE COLUMN vote_id post_id BIGINT NULL;
ALTER TABLE comment_restriction CHANGE COLUMN vote_id post_id BIGINT NOT NULL;

-- 投票相关表不再使用
DROP TABLE IF EXISTS vote_option;
DROP TABLE IF EXISTS vote_record;

SET FOREIGN_KEY_CHECKS = 1;
