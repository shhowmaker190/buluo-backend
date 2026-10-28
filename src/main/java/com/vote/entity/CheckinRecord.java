package com.vote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 每日签到记录：一个用户一天最多一条（uk_user_date 唯一索引兜底） */
@Data
@TableName("checkin_record")
public class CheckinRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate checkinDate;
    /** 本次签到获得的经验（含连续签到奖励） */
    private Integer exp;
    private LocalDateTime createdAt;
}
