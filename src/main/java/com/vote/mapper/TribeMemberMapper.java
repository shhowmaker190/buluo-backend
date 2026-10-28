package com.vote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vote.entity.TribeMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TribeMemberMapper extends BaseMapper<TribeMember> {

    @Select("SELECT m.*, u.username AS username, u.avatar AS avatar, " +
            "(SELECT COUNT(*) FROM post p WHERE p.user_id = m.user_id AND p.status = 1) AS author_post_count, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.user_id = m.user_id) AS author_comment_count, " +
            "u.exp AS author_exp " +
            "FROM tribe_member m LEFT JOIN user u ON u.id = m.user_id " +
            "WHERE m.tribe_id = #{tribeId} " +
            "ORDER BY FIELD(m.role, 'LEADER', 'DEPUTY', 'MEMBER'), m.joined_at ASC")
    List<TribeMember> selectMembers(@Param("tribeId") Long tribeId);
}
