package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tribe")
public class Tribe {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String avatar;
    private Long creatorId;
    /** 0=待审核, 1=已通过, 2=已拒绝 */
    private Integer status;
    private String rejectReason;
    /** 首领提交的待审修改内容；pendingStatus = 1 表示有待审核的修改 */
    private String pendingName;
    private String pendingDescription;
    private String pendingAvatar;
    private Integer pendingStatus;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private Integer memberCount;
    /** 已发布帖子数（仅热门榜查询填充） */
    @TableField(exist = false)
    private Integer postCount;
    /** 热度 = 成员数 × 3 + 已发布帖子数 × 2（仅热门榜查询填充） */
    @TableField(exist = false)
    private Integer heat;
    @TableField(exist = false)
    private String myRole;
}
