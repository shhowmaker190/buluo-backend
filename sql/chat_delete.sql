-- 私聊消息「单方删除」支持
-- 删除是单向的：只对自己隐藏，对方的聊天记录不受影响。
-- from_deleted：发送方是否删除；to_deleted：接收方是否删除。
ALTER TABLE chat_message
    ADD COLUMN from_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '发送方已删除(仅自己不可见)' AFTER is_read,
    ADD COLUMN to_deleted   TINYINT(1) NOT NULL DEFAULT 0 COMMENT '接收方已删除(仅自己不可见)' AFTER from_deleted;
