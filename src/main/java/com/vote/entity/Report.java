package com.vote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 举报记录：用户举报帖子 / 部落，管理员审核后下架内容或驳回 */
@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    /** POST / TRIBE */
    private String targetType;
    private Long targetId;
    /** 举报原因类别 */
    private String reason;
    private String detail;
    /** 0=待处理 1=已下架内容 2=驳回 */
    private Integer status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    // 以下字段不进表，仅列表展示时填充
    @TableField(exist = false)
    private String reporterName;
    @TableField(exist = false)
    private String targetTitle;
    @TableField(exist = false)
    private String targetAuthor;
}
