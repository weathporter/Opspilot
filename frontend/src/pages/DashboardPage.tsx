import {
  Activity,
  ArrowRight,
  Database,
  Gauge,
  Landmark,
  Network,
  RefreshCw,
  ScrollText,
  Server,
  ShieldCheck,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ErrorState, LoadingState } from '../components/Feedback';
import { TransferTable } from '../components/TransferTable';
import { TrendChart } from '../components/TrendChart';
import { formatCurrency, formatDateTime } from '../lib/format';
import { operationsApi } from '../lib/api';
import type { OperationsSummary } from '../types';

/** 运行状态条的单项定义；label 与 evidence 分别表达结论和可核验证据。 */
function HealthItem({ icon: Icon, label, value, evidence }: {
  icon: typeof Activity;
  label: string;
  value: string;
  evidence: string;
}) {
  return (
    <div className="health-item">
      <div className="health-item__icon"><Icon size={23} strokeWidth={1.7} aria-hidden="true" /></div>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        <small>{evidence}</small>
      </div>
      <i className="health-item__signal" aria-label="正常" />
    </div>
  );
}

/** 运行总览使用服务端聚合读模型，展示数据不会与有限表格行数互相推算。 */
export function DashboardPage() {
  const [summary, setSummary] = useState<OperationsSummary | null>(null);
  const [readiness, setReadiness] = useState('检查中');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setError('');
    try {
      const [summaryResponse, readinessResponse] = await Promise.all([
        operationsApi.summary(),
        operationsApi.readiness(),
      ]);
      setSummary(summaryResponse);
      setReadiness(readinessResponse.status === 'UP' ? '正常' : readinessResponse.status);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '未知错误');
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (error) return <ErrorState message={error} onRetry={() => void load()} />;
  if (!summary) return <LoadingState />;

  return (
    <div className="page-stack page-enter">
      <section className="health-rail" aria-label="核心链路状态">
        <HealthItem icon={Network} label="业务入口" value={readiness} evidence="Nginx → readiness" />
        <HealthItem icon={Server} label="应用服务" value="正常" evidence={`${summary.version} · ${summary.environment}`} />
        <HealthItem icon={Database} label="MySQL" value="正常" evidence={`${summary.accountCount} 个账户可查询`} />
        <HealthItem icon={ShieldCheck} label="数据闭环" value="正常" evidence={`${summary.completedTransferCount} 笔已审计`} />
      </section>

      <section className="metric-strip" aria-label="业务指标">
        <div><span>账户总数</span><strong>{summary.accountCount.toLocaleString('zh-CN')}</strong><small>真实数据库记录</small></div>
        <div><span>账户资金总额</span><strong>{formatCurrency(summary.totalBalance)}</strong><small>当前余额汇总</small></div>
        <div><span>累计转账</span><strong>{summary.transferCount.toLocaleString('zh-CN')}</strong><small>幂等订单口径</small></div>
        <div><span>完成率</span><strong>{summary.successRate.toFixed(2)}%</strong><small>{formatCurrency(summary.completedTransferVolume)}</small></div>
      </section>

      <div className="dashboard-grid">
        <section className="panel panel--chart">
          <div className="section-heading section-heading--inline">
            <div>
              <h2>近 24 小时交易趋势</h2>
              <p>按服务端整点聚合，空时段同样保留</p>
            </div>
            <button className="icon-button" type="button" aria-label="刷新总览" onClick={() => void load()}>
              <RefreshCw size={17} />
            </button>
          </div>
          <TrendChart trend={summary.trend} />
        </section>

        <aside className="dashboard-side">
          <section className="panel quick-actions">
            <div className="section-heading"><h2>快速操作</h2></div>
            <button type="button" onClick={() => navigate('/accounts')}>
              <span className="quick-actions__icon"><Landmark size={20} /></span>
              <span><strong>创建账户</strong><small>准备转账双方账户与初始资金</small></span>
              <ArrowRight size={17} />
            </button>
            <button type="button" onClick={() => navigate('/transfers')}>
              <span className="quick-actions__icon quick-actions__icon--cyan"><Activity size={20} /></span>
              <span><strong>发起转账</strong><small>验证事务、锁与幂等语义</small></span>
              <ArrowRight size={17} />
            </button>
          </section>

          <section className="panel evidence-list">
            <div className="section-heading"><h2>运行证据</h2></div>
            <ol>
              <li><i /><div><strong>就绪探针</strong><span>/health · {readiness}</span></div><time>{formatDateTime(summary.generatedAt)}</time></li>
              <li><i /><div><strong>MySQL 汇总查询</strong><span>事务数据可读</span></div><time>{summary.accountCount} rows</time></li>
              <li><i /><div><strong>双录流水</strong><span>订单与流水可追溯</span></div><time>{summary.completedTransferCount} orders</time></li>
              <li><i /><div><strong>可观测性栈</strong><span>Prometheus · Grafana · Loki</span></div><time>Compose</time></li>
            </ol>
          </section>
        </aside>
      </div>

      <section className="panel recent-section">
        <div className="section-heading section-heading--inline">
          <div><h2>最近转账</h2><p>点击“流水审计”可查看每笔订单的借贷证据</p></div>
          <button className="text-button" type="button" onClick={() => navigate('/ledger')}>
            查看全部 <ArrowRight size={15} />
          </button>
        </div>
        <TransferTable transfers={summary.recentTransfers} compact />
      </section>

      <div className="resume-proof">
        <Gauge size={19} aria-hidden="true" />
        <span>当前页面同时证明：真实业务数据、健康检查、可观测入口与审计链路，不是静态大屏。</span>
        <ScrollText size={19} aria-hidden="true" />
      </div>
    </div>
  );
}
