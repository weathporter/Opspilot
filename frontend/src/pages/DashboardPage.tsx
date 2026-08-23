import { ArrowRight, Building2, CheckCircle2, ReceiptText, RefreshCw, SendHorizontal, Server } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, LoadingState } from '../components/Feedback';
import { TransferTable } from '../components/TransferTable';
import { accountApi, operationsApi } from '../lib/api';
import { formatCurrency, formatDateTime, formatPercent } from '../lib/format';
import type { Account, OperationsSummary } from '../types';

/**
 * 业务门户首屏：余额与真实业务数据优先，运行状态作为支撑信息而不是喧宾夺主。
 * 数据由账户与聚合 API 并行读取，不生成无法解释的演示指标。
 */
export function DashboardPage() {
  const { hasAnyRole, session } = useAuth();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<OperationsSummary | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [summaryResponse, accountResponse] = await Promise.all([
        operationsApi.summary(),
        accountApi.list('', 6),
      ]);
      setSummary(summaryResponse);
      setAccounts(accountResponse);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '总览数据读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  if (loading && !summary) return <LoadingState label="正在汇总账户、交易与运行状态…" />;
  if (error && !summary) return <ErrorState message={error} onRetry={() => void load()} />;
  if (!summary) return null;

  return (
    <div className="overview-page page-enter">
      <section className="balance-band">
        <div>
          <span>平台账户总余额</span>
          <strong>{formatCurrency(summary.totalBalance)}</strong>
          <small>截至 {formatDateTime(summary.generatedAt)} · 共 {summary.accountCount} 个账户</small>
        </div>
        <dl className="balance-band__metrics">
          <div><dt>累计交易</dt><dd>{summary.transferCount}</dd></div>
          <div><dt>已完成金额</dt><dd>{formatCurrency(summary.completedTransferVolume)}</dd></div>
          <div><dt>完成率</dt><dd>{formatPercent(summary.successRate)}</dd></div>
        </dl>
        <button className="icon-button" aria-label="刷新总览" type="button" onClick={() => void load()}><RefreshCw size={18} /></button>
      </section>

      <section className="operational-strip" aria-label="当前平台状态">
        <div><i className="signal signal--success" /><span>业务服务</span><strong>可访问</strong></div>
        <div><Server size={17} /><span>环境</span><strong>{summary.environment}</strong></div>
        <div><ReceiptText size={17} /><span>版本</span><strong>{summary.version}</strong></div>
        <div><CheckCircle2 size={17} /><span>已完成订单</span><strong>{summary.completedTransferCount}</strong></div>
      </section>

      <section className="overview-grid">
        <article className="panel account-overview">
          <div className="section-heading section-heading--inline">
            <div><h2>账户概览</h2><p>最近更新的业务账户</p></div>
            <button className="text-link" type="button" onClick={() => navigate('/accounts')}>查看全部<ArrowRight size={15} /></button>
          </div>
          {accounts.length === 0 ? (
            <div className="empty-state"><Building2 size={26} /><strong>尚无账户</strong><span>由操作员创建首个账户后，余额和交易链路会在此显示。</span></div>
          ) : (
            <div className="account-overview__rows">
              {accounts.map((account) => (
                <button key={account.accountNo} type="button" onClick={() => navigate('/accounts')}>
                  <span className="account-symbol"><Building2 size={17} /></span>
                  <span><strong>{account.holderName}</strong><small className="mono">{account.accountNo}</small></span>
                  <span><strong>{formatCurrency(account.balance)}</strong><small>{account.status}</small></span>
                </button>
              ))}
            </div>
          )}
        </article>

        <aside className="panel action-panel">
          <div className="section-heading"><h2>常用操作</h2><p>{session.displayName} 当前可执行的业务入口</p></div>
          {hasAnyRole('ADMIN', 'OPERATOR') && <button type="button" onClick={() => navigate('/transfers')}><span><SendHorizontal size={19} /></span><div><strong>发起转账</strong><small>使用幂等键保护重试</small></div><ArrowRight size={16} /></button>}
          {hasAnyRole('ADMIN', 'OPERATOR') && <button type="button" onClick={() => navigate('/accounts')}><span><Building2 size={19} /></span><div><strong>创建账户</strong><small>余额和状态写入 MySQL</small></div><ArrowRight size={16} /></button>}
          <button type="button" onClick={() => navigate('/ledger')}><span><ReceiptText size={19} /></span><div><strong>核验流水</strong><small>订单、借记与贷记证据</small></div><ArrowRight size={16} /></button>
        </aside>
      </section>

      <section className="panel recent-section">
        <div className="section-heading section-heading--inline"><div><h2>最近交易</h2><p>选择交易可进入完整流水核验</p></div><button className="text-link" type="button" onClick={() => navigate('/transfers')}>交易管理<ArrowRight size={15} /></button></div>
        <TransferTable transfers={summary.recentTransfers} onSelect={() => navigate('/transfers')} />
      </section>
    </div>
  );
}
