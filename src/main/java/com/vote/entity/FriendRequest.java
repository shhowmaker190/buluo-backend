package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 好友申请：status 0待处理 / 1已同意 / 2已拒绝 */
@Data
@TableName("friend_request")
public class FriendRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String message;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
