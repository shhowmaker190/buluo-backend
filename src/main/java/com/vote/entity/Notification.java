package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;              // 通知接收者ID
    
    private Long postId;              // 关联的帖子ID
    
    private Long commentId;           // 关联的评论ID（可选）
    
    private String type;             // 通知类型：COMMENT（有人评论/回复）、LIKE（有人点赞）
    
    private String title;             // 通知标题
    
    private String content;           // 通知内容
    
    private String fromUsername;      // 触发通知的用户名
    
    private Boolean isRead;           // 是否已读
    
    private Boolean emailSent;        // 邮件是否已发送

    /** 跳转目标类型：POST（帖子）/ CHAT（私聊会话）/ FRIEND（好友申请） */
    private String bizType;

    /** 跳转目标 ID：帖子ID / 对方用户ID / 好友申请ID */
    private Long bizId;

    private LocalDateTime createdAt;  // 创建时间
}
