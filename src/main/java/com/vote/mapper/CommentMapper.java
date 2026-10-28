package com.vote.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vote.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}