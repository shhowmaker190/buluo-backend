package com.vote.dto;
import lombok.Data;
@Data
public class CommentDTO {
    private Long postId;
    private String content;
    private Long parentId;
}
