-- 私聊消息支持「分享帖子 / 部落」卡片
-- msg_type: text=普通文本, post=分享帖子, tribe=分享部落
-- ref_*: 分享卡片的目标信息（冗余存一份，避免历史消息渲染时再回查）
ALTER TABLE chat_message
    ADD COLUMN msg_type   VARCHAR(16)  NOT NULL DEFAULT 'text' COMMENT '消息类型: text/post/tribe' AFTER content,
    ADD COLUMN ref_id     BIGINT       NULL COMMENT '分享对象ID(帖子/部落)' AFTER msg_type,
    ADD COLUMN ref_title  VARCHAR(200) NULL COMMENT '分享卡片标题' AFTER ref_id,
    ADD COLUMN ref_sub    VARCHAR(200) NULL COMMENT '分享卡片副标题' AFTER ref_title,
    ADD COLUMN ref_avatar VARCHAR(255) NULL COMMENT '分享卡片头像' AFTER ref_sub;
