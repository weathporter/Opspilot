import { Activity, BellRing, Boxes, CheckCircle2, Container, ExternalLink, FileClock, Gauge, GitBranch, ServerCog } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { operationsApi } from '../lib/api';

/** 根据当前浏览器主机拼出本地监控入口，避免把 localhost 写死到远程演示。 */
function localToolUrl(port: number): string {
  return `${window.location.protocol}//${window.location.hostname}:${port}`;
}

/** 系统状态页区分“实时探针结果”和“项目已实现能力”，避免把静态文案冒充监控数据。 */
export function SystemPage() {
  const [readiness, setReadiness] = useState('检查中');
  const [environment, setEnvironment] = useState('检查中');
  const [checkedAt, setCheckedAt] = useState('—');

  const check = useCallback(async () => {
    // 两个请求独立结算：聚合接口暂时失败时仍保留 readiness 结论，反之亦然。
    const [readinessResult, summaryResult] = await Promise.allSettled([
      operationsApi.readiness(),
      operationsApi.summary(),
    ]);
    setReadiness(
      readinessResult.status === 'fulfilled'
        ? (readinessResult.value.status === 'UP' ? '正常' : readinessResult.value.status)
        : '不可用',
    );
    setEnvironment(
      summaryResult.status === 'fulfilled' ? summaryResult.value.environment : '未知',
    );
    setCheckedAt(new Date().toLocaleTimeString('zh-CN', { hour12: false }));
  }, []);

  useEffect(() => {
    void check();
  }, [check]);

  // 13000/19090/19093 是宿主机 Compose 映射端口，不应在 Kubernetes 页面冒充集群监控入口。
  const isComposeEnvironment = environment === 'compose';

  const capabilities = [
    { icon: Container, title: '容器化基线', text: '非 root、只读文件系统、健康检查、优雅停机' },
    { icon: Boxes, title: 'Kubernetes / Helm', text: '双副本、Ingress、探针、HPA、PDB、NetworkPolicy' },
    { icon: GitBranch, title: '发布与回滚', text: '版本化镜像、滚动升级、失败阻断、一键回滚' },
    { icon: BellRing, title: '监控与告警', text: 'Prometheus、Grafana、Alertmanager、Loki、Alloy' },
    { icon: FileClock, title: '备份与排障', text: 'MySQL 备份恢复、诊断收集、故障演练、Runbook' },
    { icon: ServerCog, title: 'Linux 原生部署', text: 'systemd、Nginx、logrotate、Shell 自动化脚本' },
  ];

  return (
    <div className="page-stack page-enter">
      <section className="system-hero">
        <div className="system-hero__signal"><Activity size={28} /></div>
        <div><span>实时 readiness</span><strong>{readiness}</strong><small>最后检查：{checkedAt} · 运行模式：{environment}</small></div>
        <button className="button button--secondary" type="button" onClick={() => void check()}><Gauge size={16} />重新检查</button>
      </section>

      <section className="panel">
        <div className="section-heading"><h2>监控入口</h2><p>{isComposeEnvironment ? '当前 Compose 观测栈已随应用启动。' : '当前不是 Compose 模式，不展示宿主机观测端口。'}</p></div>
        {isComposeEnvironment ? (
          <div className="tool-links">
            <a href={localToolUrl(13000)} target="_blank" rel="noreferrer"><span><strong>Grafana</strong><small>业务与主机仪表盘</small></span><ExternalLink size={17} /></a>
            <a href={localToolUrl(19090)} target="_blank" rel="noreferrer"><span><strong>Prometheus</strong><small>指标查询与 Targets</small></span><ExternalLink size={17} /></a>
            <a href={localToolUrl(19093)} target="_blank" rel="noreferrer"><span><strong>Alertmanager</strong><small>告警分组与恢复状态</small></span><ExternalLink size={17} /></a>
          </div>
        ) : (
          <div className="inline-message inline-message--warning" role="status">
            Kubernetes 模式应连接集群内 Prometheus/Grafana 或企业监控平台；本地 Compose 端口不会自动存在。
          </div>
        )}
      </section>

      <section className="panel">
        <div className="section-heading"><h2>企业级交付能力</h2><p>每一项都有项目文件、脚本或运行配置作为证据。</p></div>
        <div className="capability-list">
          {capabilities.map((capability) => {
            const Icon = capability.icon;
            return <article key={capability.title}><Icon size={20} /><div><strong>{capability.title}</strong><span>{capability.text}</span></div><CheckCircle2 size={17} className="success-text" /></article>;
          })}
        </div>
      </section>

      <section className="architecture-flow" aria-label="请求链路">
        {['浏览器', 'Nginx / Ingress', 'Spring Boot', 'MySQL', 'Prometheus / Loki'].map((node, index) => (
          <div key={node}><span>{index + 1}</span><strong>{node}</strong>{index < 4 && <i aria-hidden="true" />}</div>
        ))}
      </section>
    </div>
  );
}
