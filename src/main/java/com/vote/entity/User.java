package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String email;
    private String avatar;
    private String role;
    private Integer status;
    // 是否在主页公开「我的帖子」：1=公开（默认），0=仅自己可见
    private Integer postsPublic;
    /** 用户唯一 6 位 ID（varchar 保留前导零，admin 固定 000001），用于按 ID 搜索、添加好友 */
    private String userNo;
    /** 签到等获得的额外经验，直接叠加到等级活跃值上（满级门槛 300） */
    private Integer exp;
    private LocalDateTime createdAt;
}