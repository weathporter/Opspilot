# MySQL备份恢复Runbook

## 备份

```bash
sudo bash scripts/backup-mysql.sh
```

脚本使用`--single-transaction`减少InnoDB在线备份对业务的影响，生成gzip文件和SHA-256校验文件，并按保留天数清理同一受控目录下的旧备份。

## 校验

```bash
gzip -t /var/backups/opspilot/mysql/opspilot_YYYYMMDD_HHMMSS.sql.gz
sha256sum -c /var/backups/opspilot/mysql/opspilot_YYYYMMDD_HHMMSS.sql.gz.sha256
```

备份成功不等于可恢复，必须定期在隔离测试库进行恢复演练。

## 恢复前检查

```bash
sudo bash scripts/restore-mysql.sh \
  --file /path/to/backup.sql.gz \
  --confirm opspilot \
  --dry-run
```

## 正式恢复

```bash
sudo bash scripts/restore-mysql.sh \
  --file /path/to/backup.sql.gz \
  --confirm opspilot
```

脚本会先再做一次安全备份，然后才写入数据库。正式环境还需要维护窗口、审批、业务停止策略和恢复后的数据校验。
