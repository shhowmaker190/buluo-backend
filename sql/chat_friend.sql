-- 私聊与好友模块：新增 3 张表，notification 增加 2 个跳转字段
-- 说明：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 duplicate column，属正常。

CREATE TABLE IF NOT EXISTS friend (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL COMMENT '本人ID',
  friend_id  BIGINT NOT NULL COMMENT '好友ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_friend (user_id, friend_id),
  KEY idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系，互为好友时双向各存一条';

CREATE TABLE IF NOT EXISTS friend_request (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_user_id BIGINT NOT NULL COMMENT '申请人',
  to_user_id   BIGINT NOT NULL COMMENT '被申请人',
  message      VARCHAR(200) DEFAULT NULL COMMENT '申请留言',
  status       TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已同意 2已拒绝',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_to_status (to_user_id, status),
  KEY idx_from_user (from_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请';

CREATE TABLE IF NOT EXISTS chat_message (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_user_id BIGINT NOT NULL,
  to_user_id   BIGINT NOT NULL,
  content      TEXT NOT NULL,
  is_read      TINYINT(1) NOT NULL DEFAULT 0,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_conversation (from_user_id, to_user_id, created_at),
  KEY idx_to_read (to_user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息';

-- 通知要能跳转到帖子之外的目标（聊天会话、好友请求）
ALTER TABLE notification ADD COLUMN biz_type VARCHAR(32) DEFAULT NULL COMMENT 'POST/CHAT/FRIEND';
ALTER TABLE notification ADD COLUMN biz_id   BIGINT     DEFAULT NULL COMMENT '跳转目标ID';
