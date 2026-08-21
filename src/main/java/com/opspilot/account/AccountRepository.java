package com.opspilot.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 账户仓储：同时支撑业务写入、控制台读模型和转账悲观锁。 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /** 按外部业务账号查询，Optional 明确表达“可能不存在”。 */
    Optional<Account> findByAccountNo(String accountNo);

    /**
     * 同时匹配账号与户名的管理端搜索。
     * Pageable 把 limit 和排序下推到 MySQL，而不是把全表读进 JVM 后再截断。
     */
    List<Account> findByAccountNoContainingOrHolderNameContainingIgnoreCase(
            String accountNo,
            String holderName,
            Pageable pageable
    );

    /** 汇总所有账户余额，为运行总览提供真实数据库口径。 */
    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal sumBalance();

    /**
     * 在当前事务中执行 SELECT ... FOR UPDATE。
     * 锁直到事务提交或回滚才释放，用于阻止并发转账基于同一个旧余额重复扣款。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNo = :accountNo")
    Optional<Account> findByAccountNoForUpdate(@Param("accountNo") String accountNo);
}
