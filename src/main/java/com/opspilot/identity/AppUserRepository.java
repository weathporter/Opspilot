package com.opspilot.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

/** 用户持久化端口；所有查询都使用已经标准化的小写 username。 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** @return 指定登录名对应的用户；不存在时为空。 */
    Optional<AppUser> findByUsername(String username);

    /** 引导管理员和用户管理接口用它执行唯一性预检查，数据库唯一键仍是最终防线。 */
    boolean existsByUsername(String username);

    /** 管理台按最近创建顺序展示用户。 */
    List<AppUser> findAllByOrderByCreatedAtDesc();
}
