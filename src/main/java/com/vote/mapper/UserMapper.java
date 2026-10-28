package com.vote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vote.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 原子加经验：避免「先查后写」在并发签到时丢更新 */
    @Update("UPDATE user SET exp = exp + #{delta} WHERE id = #{userId}")
    int addExp(@Param("userId") Long userId, @Param("delta") int delta);
}
