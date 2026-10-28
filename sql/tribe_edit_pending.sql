-- 首领修改部落信息需管理员审核：新增待审字段
-- pending_status: 0=无待审修改, 1=有修改待审核
ALTER TABLE tribe
    ADD COLUMN pending_name VARCHAR(50) NULL AFTER reject_reason,
    ADD COLUMN pending_description VARCHAR(200) NULL AFTER pending_name,
    ADD COLUMN pending_avatar VARCHAR(255) NULL AFTER pending_description,
    ADD COLUMN pending_status TINYINT NOT NULL DEFAULT 0 AFTER pending_avatar;
