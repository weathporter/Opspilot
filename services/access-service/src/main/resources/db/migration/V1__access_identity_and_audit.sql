-- 新建的 Access 独立库从 V1 开始；不能把原单体 V2 的 Flyway 历史直接复制到本库。
-- 现有部署的数据搬迁需要另行备份、核对与切换，不能依靠 CREATE TABLE 自动完成。
CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);

CREATE TABLE app_user_role (
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT pk_app_user_role PRIMARY KEY (user_id, role),
    CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

CREATE TABLE audit_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    actor VARCHAR(64),
    target_type VARCHAR(64),
    target_id VARCHAR(128),
    trace_id VARCHAR(64),
    source_ip VARCHAR(45),
    description VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at);
CREATE INDEX idx_audit_event_actor_occurred_at ON audit_event (actor, occurred_at);
