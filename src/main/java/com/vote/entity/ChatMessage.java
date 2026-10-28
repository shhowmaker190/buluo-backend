package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 私聊消息 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    /** 消息类型：text=普通文本，post=分享帖子，tribe=分享部落 */
    private String msgType;
    /** 分享对象 ID（帖子 / 部落） */
    private Long refId;
    /** 分享卡片标题（冗余存储，历史消息无需回查） */
    private String refTitle;
    /** 分享卡片副标题 */
    private String refSub;
    /** 分享卡片头像 */
    private String refAvatar;
    private Boolean isRead;
    /** 发送方是否已删除（单向删除，仅自己不可见） */
    private Boolean fromDeleted;
    /** 接收方是否已删除（单向删除，仅自己不可见） */
    private Boolean toDeleted;
    private LocalDateTime createdAt;
}
