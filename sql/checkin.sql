-- 每日签到打卡 + 等级经验
-- 等级经验 = 已发布帖子数 × 3 + 评论数 × 1 + user.exp（签到等额外经验）

ALTER TABLE user
  ADD COLUMN exp INT NOT NULL DEFAULT 0 COMMENT '签到获得的额外经验，直接叠加到等级活跃值' AFTER posts_public;

CREATE TABLE IF NOT EXISTS checkin_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  checkin_date DATE NOT NULL,
  exp INT NOT NULL DEFAULT 5 COMMENT '本次签到获得的经验（含连续奖励）',
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  -- 每人每天只能有一条，靠唯一索引兜住并发重复打卡
  UNIQUE KEY uk_user_date (user_id, checkin_date),
  KEY idx_user_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到记录';

-- 管理员等级直接拉满（满级门槛 300，给 500 保证始终是元老 Lv6）
UPDATE user SET exp = 500 WHERE role = 'ADMIN';
