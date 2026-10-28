package com.vote.dto;
import lombok.Data;
@Data
public class CreatePostDTO {
    private String title;
    private String content;
    private Long tribeId;
}
