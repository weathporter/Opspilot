import { AlertTriangle, CheckCircle2, RefreshCw, Scale } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { ErrorState, LoadingState } from '../components/Feedback';
import { operationsApi } from '../lib/api';
import { formatDateTime } from '../lib/format';
import type { ReconciliationDetail, ReconciliationRun } from '../types';

/** 对账工作台沿用平台的白色表格与深蓝导航，不把抽样结果包装成全库完整审计。 */
export function ReconciliationPage() {
  const { hasAnyRole } = useAuth();
  const canRun = hasAnyRole('ADMIN', 'OPERATOR');
  const [runs, setRuns] = useState<ReconciliationRun[]>([]);
  const [selected, setSelected] = useState<ReconciliationDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const recent = await operationsApi.reconciliationRuns();
      setRuns(recent);
      if (recent[0]) setSelected(await operationsApi.reconciliationDetail(recent[0].id));
      else setSelected(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '对账历史读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function run() {
    setRunning(true);
    setError('');
    setNotice('');
    try {
      const created = await operationsApi.runReconciliation(100);
      const [recent, detail] = await Promise.all([
        operationsApi.reconciliationRuns(),
        operationsApi.reconciliationDetail(created.id),
      ]);
      setRuns(recent);
      setSelected(detail);
      setNotice(created.checkedCount === 0
        ? '本批次没有可核验交易；不能据此判断历史账务均正常。'
        : `本批次核验 ${created.checkedCount} 笔，发现 ${created.discrepancyCount} 笔差异。`);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '本次对账未完成');
    } finally {
      setRunning(false);
    }
  }

  async function select(id: number) {
    setError('');
    try {
      setSelected(await operationsApi.reconciliationDetail(id));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '批次详情读取失败');
    }
  }

  if (loading && runs.length === 0) return <LoadingState label="正在读取对账历史…" />;
  if (error && runs.length === 0) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <div className="page-stack page-enter">
      <section className="audit-intro reconciliation-intro">
        <Scale size={22} aria-hidden="true" />
        <div><strong>核验范围</strong><span>每次只核验最近 100 笔订单与双边流水；结果保存在独立的运维历史库中。</span></div>
      </section>
      <section className="panel table-panel">
        <div className="section-heading section-heading--inline">
          <div><h2>对账批次</h2><p>保留检查笔数、差异数与完成时间，点击批次查看差异订单。</p></div>
          <div className="reconciliation-actions">
            <button className="button button--secondary" type="button" disabled={loading || running} onClick={() => void load()}>
              <RefreshCw size={16} aria-hidden="true" />刷新
            </button>
            {canRun && <button className="button button--primary" type="button" disabled={running} onClick={() => void run()}>
              <Scale size={16} aria-hidden="true" />{running ? '正在核验…' : '运行一次对账'}
            </button>}
          </div>
        </div>
        {error && <div className="inline-message inline-message--error" role="alert">{error}</div>}
        {notice && <div className="inline-message" role="status">{notice}</div>}
        {runs.length === 0 ? (
          <div className="empty-state"><Scale size={27} /><strong>还没有对账批次</strong>
            <span>{canRun ? '运行一次最近交易抽样核验，结果会保存到此处。' : '等待管理员或操作员执行对账。'}</span></div>
        ) : (
          <div className="table-scroll"><table className="data-table reconciliation-table">
            <thead><tr><th>批次</th><th>完成时间</th><th>核验笔数</th><th>差异笔数</th><th>结论</th><th>详情</th></tr></thead>
            <tbody>{runs.map((item) => <tr key={item.id} className={selected?.id === item.id ? 'data-table__row--selected' : undefined}>
              <td className="mono">#{item.id}</td><td>{formatDateTime(item.completedAt)}</td>
              <td>{item.checkedCount}</td><td>{item.discrepancyCount}</td>
              <td><span className={`status-badge status-badge--${item.status === 'BALANCED' ? 'success' : 'failure'}`}>
                {item.checkedCount === 0 ? '无可核验交易' : item.status === 'BALANCED' ? '本批无差异' : '发现差异'}
              </span></td>
              <td><button className="reconciliation-detail-button" type="button" onClick={() => void select(item.id)}
                aria-label={`查看对账批次 ${item.id} 的详情`}>查看</button></td>
            </tr>)}</tbody>
          </table></div>
        )}
      </section>
      {selected && <section className="panel reconciliation-detail" aria-live="polite">
        <div className="section-heading"><h2>{selected.discrepancyCount ? <AlertTriangle size={19} /> : <CheckCircle2 size={19} />}
          批次 #{selected.id} · {selected.discrepancyCount ? '待排查差异' : '本批无差异'}</h2>
          <p>核验 {selected.checkedCount} 笔，完成于 {formatDateTime(selected.completedAt)}。仅代表本次抽样结果。</p></div>
        {selected.discrepancyRequestIds.length > 0 ? (
          <ul className="reconciliation-discrepancies">{selected.discrepancyRequestIds.map((id) =>
            <li key={id}><span className="mono">{id}</span><span>请到“流水核验”页按请求编号查看订单与双边流水。</span></li>)}</ul>
        ) : <p className="reconciliation-empty">本批次未发现不平衡流水。</p>}
      </section>}
    </div>
  );
}
