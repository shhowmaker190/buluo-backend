package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Long userId;
    private Long postId;
    // 楼中楼：父评论ID，为空表示主楼评论
    private Long parentId;
    private LocalDateTime createdAt;
    private Integer likeCount;
}