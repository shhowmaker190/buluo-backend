package com.vote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 好友关系：互为好友时插入两条（A→B、B→A），查询时只需按 user_id 过滤 */
@Data
@TableName("friend")
public class Friend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long friendId;
    private LocalDateTime createdAt;
}
