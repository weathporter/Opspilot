# 目标目录结构

```text
OpsPilot/
├── src/                         当前Spring Boot业务应用
├── deploy/
│   ├── linux/                   systemd、Nginx、环境变量和日志轮转
│   └── docker/                  Docker专用配置
├── observability/               指标、日志、告警与仪表盘
├── scripts/                     运维自动化脚本
├── docs/
│   ├── architecture/            架构与边界
│   ├── decisions/               架构决策记录
│   ├── learning-notes/          配套学习笔记
│   ├── runbooks/                标准操作手册
│   ├── troubleshooting/         排障手册
│   └── postmortems/             故障复盘
├── Dockerfile
├── compose.yml
├── compose.observability.yml
└── pom.xml
```

第一阶段不把现有Maven工程移动到`backend/`，因为当前只有一个后端应用，移动只会增加IDE、Docker构建和脚本路径变化。加入Vue前端时再执行一次可验证的目录迁移，并用新的ADR记录原因。
