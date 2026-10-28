package com.vote.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vote.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
