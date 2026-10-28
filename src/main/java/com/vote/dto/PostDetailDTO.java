package com.vote.dto;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class PostDetailDTO {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String username;
    private String avatar;
    private Long tribeId;
    private String tribeName;
    private String tribeAvatar;
    /** 楼主在该部落中的职位 */
    private String authorTribeRole;
    private LocalDateTime createdAt;
    private Integer status;
    private String rejectReason;
    private Integer commentCount;
    private Integer authorPostCount;
    private Integer authorCommentCount;
    /** 签到等额外经验，算等级时叠加 */
    private Integer authorExp;
    private List<CommentDTO> comments;

    @Data
    public static class CommentDTO {
        private Long id;
        private String content;
        private Long userId;
        private String username;
        private String avatar;
        private LocalDateTime createdAt;
        private Integer likeCount;
        private Boolean liked;
        private Long parentId;
        private Integer floor;
        private Integer authorPostCount;
        private Integer authorCommentCount;
        /** 签到等额外经验，算等级时叠加 */
        private Integer authorExp;
        /** 该评论作者在本部落中的职位 */
        private String role;
        private List<CommentDTO> replies;
    }
}
