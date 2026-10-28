package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tribe_member")
public class TribeMember {
    public static final String ROLE_LEADER = "LEADER";
    public static final String ROLE_DEPUTY = "DEPUTY";
    public static final String ROLE_MEMBER = "MEMBER";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tribeId;
    private Long userId;
    /** LEADER=首领, DEPUTY=副首领, MEMBER=成员 */
    private String role;
    /** 1=在该部落内被禁言 */
    private Integer muted;
    private LocalDateTime joinedAt;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
    @TableField(exist = false)
    private Integer authorPostCount;
    @TableField(exist = false)
    private Integer authorCommentCount;
    /** 签到等额外经验，算等级时叠加 */
    @TableField(exist = false)
    private Integer authorExp;
}
