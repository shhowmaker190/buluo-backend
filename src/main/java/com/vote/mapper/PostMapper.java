package com.vote.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vote.dto.HotCommentDTO;
import com.vote.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    @Select("<script>" +
            "SELECT p.*, u.username AS username, u.avatar AS avatar, " +
            "t.name AS tribe_name, t.avatar AS tribe_avatar, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comment_count, " +
            "(SELECT COUNT(*) FROM post ap WHERE ap.user_id = p.user_id AND ap.status = 1) AS author_post_count, " +
            "(SELECT COUNT(*) FROM comment ac WHERE ac.user_id = p.user_id) AS author_comment_count, " +
            "u.exp AS author_exp, " +
            "(SELECT tm.role FROM tribe_member tm WHERE tm.tribe_id = p.tribe_id AND tm.user_id = p.user_id) AS author_tribe_role " +
            "FROM post p LEFT JOIN user u ON u.id = p.user_id LEFT JOIN tribe t ON t.id = p.tribe_id " +
            "WHERE p.status = 1 " +
            "<if test='tribeId != null'> AND p.tribe_id = #{tribeId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<choose>" +
            "<when test=\"sort == 'hot'\"> ORDER BY comment_count DESC, p.created_at DESC </when>" +
            "<otherwise> ORDER BY p.created_at DESC </otherwise>" +
            "</choose>" +
            "</script>")
    Page<Post> selectPostPage(Page<Post> page, @Param("tribeId") Long tribeId, @Param("sort") String sort, @Param("keyword") String keyword);

    @Select("SELECT p.*, t.name AS tribe_name, t.avatar AS tribe_avatar, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comment_count " +
            "FROM post p LEFT JOIN tribe t ON t.id = p.tribe_id " +
            "WHERE p.user_id = #{userId} " +
            "ORDER BY p.created_at DESC")
    List<Post> selectMyPosts(@Param("userId") Long userId);

    @Select("SELECT p.*, t.name AS tribe_name, t.avatar AS tribe_avatar, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comment_count, " +
            "(SELECT COUNT(*) FROM post ap WHERE ap.user_id = p.user_id AND ap.status = 1) AS author_post_count, " +
            "(SELECT COUNT(*) FROM comment ac WHERE ac.user_id = p.user_id) AS author_comment_count, " +
            "u.exp AS author_exp " +
            "FROM post p LEFT JOIN tribe t ON t.id = p.tribe_id LEFT JOIN user u ON u.id = p.user_id " +
            "WHERE p.user_id = #{userId} AND p.status = 1 " +
            "ORDER BY p.created_at DESC LIMIT #{limit}")
    List<Post> selectPublishedPosts(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 管理端：全部帖子（含待审/已拒绝），带作者与部落，方便按 ID 排查
     */
    @Select("SELECT p.*, u.username AS username, u.avatar AS avatar, " +
            "t.name AS tribe_name, t.avatar AS tribe_avatar, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comment_count " +
            "FROM post p LEFT JOIN user u ON u.id = p.user_id LEFT JOIN tribe t ON t.id = p.tribe_id " +
            "ORDER BY p.id DESC LIMIT #{limit}")
    List<Post> selectAllForAdmin(@Param("limit") int limit);

    /** 管理端：待审核帖子，带作者与部落（原先只查 post 表，列表里看不到作者/部落） */
    @Select("SELECT p.*, u.username AS username, u.avatar AS avatar, " +
            "t.name AS tribe_name, t.avatar AS tribe_avatar, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id) AS comment_count " +
            "FROM post p LEFT JOIN user u ON u.id = p.user_id LEFT JOIN tribe t ON t.id = p.tribe_id " +
            "WHERE p.status = 0 ORDER BY p.created_at ASC")
    List<Post> selectPendingForAdmin();

    /**
     * 批量取每个帖子最热门的 N 条回复（点赞数降序，同赞数按时间升序）。
     * 用窗口函数避免 N+1 查询：一次查询即可拿到本页全部帖子的热门回复。
     * 依赖 MySQL 8+，低版本需改成 Java 侧分组截取。
     */
    @Select("<script>" +
            "SELECT * FROM (" +
            "  SELECT c.id AS id, c.post_id AS post_id, c.user_id AS user_id, c.content AS content, " +
            "         c.like_count AS like_count, c.created_at AS created_at, " +
            "         u.username AS username, u.avatar AS avatar, " +
            "         (SELECT tm.role FROM tribe_member tm WHERE tm.tribe_id = p.tribe_id AND tm.user_id = c.user_id) AS role, " +
            "         (SELECT COUNT(*) FROM post ap WHERE ap.user_id = c.user_id AND ap.status = 1) AS author_post_count, " +
            "         (SELECT COUNT(*) FROM comment ac WHERE ac.user_id = c.user_id) AS author_comment_count, " +
            "         u.exp AS author_exp, " +
            "         ROW_NUMBER() OVER (PARTITION BY c.post_id ORDER BY c.like_count DESC, c.created_at ASC) AS rn " +
            "  FROM comment c " +
            "  JOIN post p ON p.id = c.post_id " +
            "  LEFT JOIN user u ON u.id = c.user_id " +
            "  WHERE c.post_id IN " +
            "  <foreach collection='postIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>" +
            ") t WHERE t.rn &lt;= #{limit} ORDER BY t.post_id, t.rn" +
            "</script>")
    List<HotCommentDTO> selectHotComments(@Param("postIds") List<Long> postIds, @Param("limit") int limit);
}
