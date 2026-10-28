package com.vote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vote.entity.Tribe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TribeMapper extends BaseMapper<Tribe> {

    @Select("<script>" +
            "SELECT t.*, u.username AS creator_name, " +
            "(SELECT COUNT(*) FROM tribe_member m WHERE m.tribe_id = t.id) AS member_count, " +
            "(SELECT COUNT(*) FROM post p WHERE p.tribe_id = t.id AND p.status = 1) AS post_count " +
            "<if test='userId != null'>, (SELECT m.role FROM tribe_member m WHERE m.tribe_id = t.id AND m.user_id = #{userId}) AS my_role </if>" +
            "FROM tribe t LEFT JOIN user u ON u.id = t.creator_id " +
            "WHERE t.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'> AND t.name LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY member_count DESC, t.created_at DESC" +
            "</script>")
    List<Tribe> selectApproved(@Param("keyword") String keyword, @Param("userId") Long userId);

    @Select("SELECT t.*, " +
            "(SELECT COUNT(*) FROM tribe_member m WHERE m.tribe_id = t.id) AS member_count, " +
            "(SELECT COUNT(*) FROM post p WHERE p.tribe_id = t.id AND p.status = 1) AS post_count " +
            "FROM tribe t WHERE t.status = 1")
    List<Tribe> selectWithStats();

    @Select("SELECT t.*, u.username AS creator_name, " +
            "(SELECT COUNT(*) FROM tribe_member m WHERE m.tribe_id = t.id) AS member_count, " +
            "(SELECT COUNT(*) FROM post p WHERE p.tribe_id = t.id AND p.status = 1) AS post_count, " +
            "(SELECT m.role FROM tribe_member m WHERE m.tribe_id = t.id AND m.user_id = #{userId}) AS my_role " +
            "FROM tribe t LEFT JOIN user u ON u.id = t.creator_id " +
            "WHERE t.id = #{tribeId}")
    Tribe selectDetail(@Param("tribeId") Long tribeId, @Param("userId") Long userId);

    @Select("SELECT t.*, u.username AS creator_name, " +
            "(SELECT COUNT(*) FROM tribe_member m WHERE m.tribe_id = t.id) AS member_count, " +
            "(SELECT COUNT(*) FROM post p WHERE p.tribe_id = t.id AND p.status = 1) AS post_count, " +
            "m.role AS my_role, m.muted AS my_muted, m.joined_at AS my_joined_at " +
            "FROM tribe t LEFT JOIN user u ON u.id = t.creator_id " +
            "INNER JOIN tribe_member m ON m.tribe_id = t.id AND m.user_id = #{userId} " +
            "WHERE t.status = 1 " +
            "ORDER BY FIELD(m.role, 'LEADER', 'DEPUTY', 'MEMBER'), t.created_at DESC")
    List<Tribe> selectMyTribes(@Param("userId") Long userId);

    @Select("SELECT t.*, u.username AS creator_name, " +
            "(SELECT COUNT(*) FROM tribe_member m WHERE m.tribe_id = t.id) AS member_count, " +
            "(SELECT COUNT(*) FROM post p WHERE p.tribe_id = t.id AND p.status = 1) AS post_count " +
            "FROM tribe t LEFT JOIN user u ON u.id = t.creator_id " +
            "WHERE t.creator_id = #{userId} " +
            "ORDER BY t.created_at DESC")
    List<Tribe> selectCreatedTribes(@Param("userId") Long userId);
}
