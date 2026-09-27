-- 只追加迁移，不改写已执行的 V1/V2/V3；旧 Helm 双副本首次启动也不能同时引导管理员。
CREATE TABLE bootstrap_claim (
    id TINYINT PRIMARY KEY
);
