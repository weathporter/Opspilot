# OpsPilot 看板 JSON 阅读指南

Grafana dashboard 文件必须是严格 JSON，JSON 规范不允许 `#`、`//` 或 `/* ... */` 注释。为了既保持 Grafana 可加载，又让你能读懂配置，解释采用两种合法方式：每个面板使用 `description` 字段，通用字段在本文说明。

## 顶层字段

- `uid` 是跨环境稳定的看板标识，URL 和引用不会因数据库自增 ID 改变；`id: null` 让 Grafana 在本实例自行分配内部 ID。
- `editable: false` 与 provisioning 的 `allowUiUpdates: false` 配合，确保 Git 中 JSON 是唯一事实来源。
- `refresh: 10s` 表示浏览器每 10 秒重新查询；`time` 默认查看最近 30 分钟；`timezone: browser` 按使用者浏览器时区展示。
- `graphTooltip: 1` 让多个时序面板共享十字光标，便于把错误率、延迟和内存异常按同一时间点关联。
- `schemaVersion` 是 Grafana dashboard JSON 模型版本；升级 Grafana 自动迁移后，应检查生成的 diff。
- `tags` 用于搜索/分类；`templating.list` 为空表示第一阶段尚未加入实例、环境等下拉变量。

## 每个面板的共同结构

- `datasource.type/uid` 明确选择 Prometheus 或 Loki；UID 必须与 provisioning 文件一致。
- `gridPos` 用 24 列网格描述高度 `h`、宽度 `w`、横坐标 `x`、纵坐标 `y`。
- `id` 是看板内唯一面板编号；`type` 决定 stat、timeseries 或 logs 渲染方式。
- `targets` 是数据查询。Prometheus 使用 PromQL `expr`，Loki 使用 LogQL `expr`；`refId` 用 A/B/C 区分同一面板的多条查询。
- `fieldConfig.defaults.unit` 只控制显示单位，不会改变查询数值；`thresholds` 和 `mappings` 控制状态颜色/文字。
- `reduceOptions.calcs: lastNotNull` 让 stat 面板显示时间范围内最后一个非空样本，而不是平均值。
- `options.legend`、`tooltip`、`colorMode` 等只影响呈现，不改变采集或告警计算。

面板里的 PromQL/LogQL 机制请直接在 Grafana 将鼠标悬停到标题上查看 `description`。告警阈值仍以 Prometheus rules 为准，看板颜色不能替代告警规则。
