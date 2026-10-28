package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.vote.dto.HotCommentDTO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private Long tribeId;
    private LocalDateTime createdAt;
    // 审核状态: 0=待审核, 1=审核通过(已发布), 2=审核拒绝
    private Integer status;
    // 拒绝原因
    private String rejectReason;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
    @TableField(exist = false)
    private String tribeName;
    @TableField(exist = false)
    private String tribeAvatar;
    @TableField(exist = false)
    private Integer commentCount;
    /** 作者在该帖子所属部落中的职位 */
    @TableField(exist = false)
    private String authorTribeRole;
    // 作者活跃数据（仅列表查询填充，用于计算等级）
    @TableField(exist = false)
    private Integer authorPostCount;
    @TableField(exist = false)
    private Integer authorCommentCount;
    /** 作者签到等额外经验，算等级时叠加 */
    @TableField(exist = false)
    private Integer authorExp;
    /** 列表页展示的最热门回复（仅列表查询填充） */
    @TableField(exist = false)
    private List<HotCommentDTO> hotComments;
}
