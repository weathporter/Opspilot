# Flyway 迁移阅读说明

## 为什么 V1 里面没有教学注释

Flyway 会把已经执行的迁移版本和文件校验和写入 `flyway_schema_history`。即使只给 SQL 增加注释，文件校验和也会改变，已有环境启动时就会报 `Migration checksum mismatch`。

因此必须遵守两条规则：

1. 已在任何共享或持久化环境执行过的 `V1`、`V2` 等迁移永远不再编辑。
2. 新的表结构变化创建下一个版本文件，例如 `V2__add_transfer_failure_reason.sql`，不能回头修改 V1。

下面是 V1 的逐段教学说明。它与真正执行的 SQL 分开保存，因此既不改变 Flyway 校验和，也能帮助你理解数据库设计。

## account：账户当前状态

```sql
CREATE TABLE account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no VARCHAR(32) NOT NULL,
    holder_name VARCHAR(64) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_account_no UNIQUE (account_no),
    CONSTRAINT ck_account_balance_non_negative CHECK (balance >= 0)
);
```

- `id` 是内部自增代理主键；对外业务账号由 `account_no` 表达并设置唯一约束。
- `DECIMAL(19, 2)` 精确保存十进制金额，避免 `FLOAT/DOUBLE` 的二进制舍入误差。
- `status` 对应 Java 枚举，但以字符串保存，SQL 排障时可以直接看懂，也不会因枚举调整顺序而误读旧数据。
- `version` 对应 JPA `@Version`，更新时比较并递增，用于发现并发覆盖。
- `TIMESTAMP(6)` 保留微秒，与 Java `truncatedTo(MICROS)` 对齐。
- `uk_account_no` 是并发创建相同账号时的最终防线；应用层“先查再插”无法消除竞态窗口。
- `ck_account_balance_non_negative` 让数据库拒绝负余额，形成应用规则之外的纵深防御。

## transfer_order：转账订单与幂等记录

```sql
CREATE TABLE transfer_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    source_account_no VARCHAR(32) NOT NULL,
    target_account_no VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6),
    CONSTRAINT uk_transfer_request_id UNIQUE (request_id),
    CONSTRAINT ck_transfer_amount_positive CHECK (amount > 0)
);
```

- `request_id` 来自 `Idempotency-Key`。数据库唯一约束保证同一把幂等键最多落一条订单。
- 订单保存付款账号、收款账号和金额，使服务收到重试时可以比较载荷是否与原请求一致。
- 新订单从 `PROCESSING` 开始，成功后写 `COMPLETED` 和 `completed_at`。
- `ck_transfer_amount_positive` 防止零金额或负金额进入订单表。

## ledger_entry：双边资金流水

```sql
CREATE TABLE ledger_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_order_id BIGINT NOT NULL,
    account_no VARCHAR(32) NOT NULL,
    entry_type VARCHAR(8) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_ledger_transfer_order
        FOREIGN KEY (transfer_order_id) REFERENCES transfer_order (id),
    CONSTRAINT ck_ledger_amount_positive CHECK (amount > 0)
);
```

- 一笔成功转账产生付款方 `DEBIT` 和收款方 `CREDIT` 两条流水。
- 账户表保存当前余额，流水表保存余额为什么发生变化，是审计和对账的证据。
- `account_no` 冗余保存在流水中，按账号查流水时不必每次联结订单表。
- `balance_after` 保存交易后余额快照，可以帮助重建余额变化链并发现账实不符。
- 外键禁止产生不属于任何转账订单的孤立流水。

## 索引

```sql
CREATE INDEX idx_ledger_account_created_at
    ON ledger_entry (account_no, created_at);

CREATE INDEX idx_transfer_source_created_at
    ON transfer_order (source_account_no, created_at);
```

复合索引遵循最左前缀原则，分别支持“某账号在某段时间的流水”和“某付款账号在某段时间的转出订单”。索引不是越多越好：它会增加写入成本和磁盘占用，应围绕实际查询模式设计。
