package com.vote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 改名申请：提交后由管理员审核，通过才真正修改 user.username */
@Data
@TableName("name_request")
public class NameRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String oldName;
    private String newName;
    /** 0=待审核 1=通过 2=拒绝 */
    private Integer status;
    private String rejectReason;
    private Long handlerId;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String userNo;
}
