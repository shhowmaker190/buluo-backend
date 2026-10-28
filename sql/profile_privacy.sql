-- 用户主页隐私：是否在主页公开「我的帖子」列表
-- posts_public = 1 公开（默认），0 隐藏（他人访问时看不到帖子列表，本人进入自己的主页仍可见）
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报 duplicate column，属正常现象
ALTER TABLE user
    ADD COLUMN posts_public TINYINT NOT NULL DEFAULT 1 AFTER status;
