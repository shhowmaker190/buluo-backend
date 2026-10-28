package com.vote.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 帖子列表里展示的热门回复（按点赞数取前 N 条） */
@Data
public class HotCommentDTO {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String avatar;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
    /** 该回复作者在帖子所属部落中的职位 */
    private String role;
    /** 作者活跃数据，用于前端算等级 */
    private Integer authorPostCount;
    private Integer authorCommentCount;
    /** 作者签到等额外经验，算等级时叠加 */
    private Integer authorExp;
}
