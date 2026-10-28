-- 用户唯一 6 位 ID：admin 固定 000001，其余按注册顺序递增分配
-- 用 VARCHAR(6) 而不是数字，才能保留前导零（000001）
-- 重复执行会报 duplicate column / duplicate key，属正常

ALTER TABLE user ADD COLUMN user_no VARCHAR(6) DEFAULT NULL COMMENT '用户唯一6位ID';

-- admin 固定为 000001
UPDATE user SET user_no = '000001' WHERE username = 'admin';

-- 其余存量用户按 id 升序从 000002 开始分配（MySQL 8 窗口函数）
UPDATE user u
  JOIN (
    SELECT id, LPAD(ROW_NUMBER() OVER (ORDER BY id) + 1, 6, '0') AS no
    FROM user
    WHERE username <> 'admin'
  ) t ON u.id = t.id
SET u.user_no = t.no;

-- 兜底：若上面没覆盖到（例如出现 NULL），随机补一个未占用的号
UPDATE user SET user_no = LPAD(FLOOR(100000 + RAND() * 899999), 6, '0') WHERE user_no IS NULL;

ALTER TABLE user MODIFY COLUMN user_no VARCHAR(6) NOT NULL COMMENT '用户唯一6位ID';
ALTER TABLE user ADD UNIQUE KEY uk_user_no (user_no);
