-- 举报与改名申请
-- 举报：用户对帖子 / 部落发起举报，管理员审核后下架内容或驳回
CREATE TABLE IF NOT EXISTS report (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id  BIGINT       NOT NULL COMMENT '举报人',
    target_type  VARCHAR(16)  NOT NULL COMMENT 'POST / TRIBE',
    target_id    BIGINT       NOT NULL COMMENT '被举报对象ID',
    reason       VARCHAR(32)  NOT NULL COMMENT '举报原因类别',
    detail       VARCHAR(500) NULL COMMENT '补充说明',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待处理 1=已下架内容 2=驳回',
    handler_id   BIGINT       NULL COMMENT '处理人',
    handle_note  VARCHAR(200) NULL COMMENT '处理说明',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    handled_at   DATETIME     NULL,
    KEY idx_report_status (status, created_at),
    KEY idx_report_target (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='举报记录';

-- 改名申请：用户提交，管理员审核通过后才真正改 username
CREATE TABLE IF NOT EXISTS name_request (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    old_name      VARCHAR(50)  NOT NULL,
    new_name      VARCHAR(50)  NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待审核 1=通过 2=拒绝',
    reject_reason VARCHAR(200) NULL,
    handler_id    BIGINT       NULL,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    handled_at    DATETIME     NULL,
    KEY idx_name_req_status (status, created_at),
    KEY idx_name_req_user (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='改名申请';
