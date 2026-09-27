-- 首次双副本启动时串行化管理员创建；既有 V1 不改写，Flyway 按版本追加此表。
-- 固定主键只需一行，事务内的 INSERT/UPDATE 行锁会持续到管理员及审计事件提交。
CREATE TABLE bootstrap_claim (
    id TINYINT PRIMARY KEY
);
