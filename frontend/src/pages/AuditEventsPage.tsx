import { BookOpenCheck, Filter, RefreshCw, Search, ShieldAlert } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ErrorState, LoadingState } from '../components/Feedback';
import { auditApi } from '../lib/api';
import { formatDateTime } from '../lib/format';
import type { AuditEvent } from '../types';

const outcomeLabels = { SUCCESS: '成功', FAILURE: '失败', DENIED: '已拒绝' } as const;

/** 安全审计页把数据库事件与 traceId 组织为可检索的时间线。 */
export function AuditEventsPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [query, setQuery] = useState('');
  const [outcome, setOutcome] = useState<AuditEvent['outcome'] | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setEvents(await auditApi.list(200));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '审计事件读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const visible = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return events.filter((event) => {
      const matchesOutcome = !outcome || event.outcome === outcome;
      const matchesText = !keyword || [event.eventType, event.actor, event.targetId, event.traceId, event.sourceIp]
        .some((value) => value?.toLowerCase().includes(keyword));
      return matchesOutcome && matchesText;
    });
  }, [events, outcome, query]);

  if (loading && events.length === 0) return <LoadingState label="正在读取审计证据…" />;
  if (error && events.length === 0) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <section className="panel table-panel page-enter">
      <div className="section-heading section-heading--inline">
        <div>
          <h2><BookOpenCheck size={19} />最近审计事件</h2>
          <p>只追加记录；普通业务角色无法读取，平台不提供删除接口。</p>
        </div>
        <button className="button button--secondary" type="button" onClick={() => void load()}>
          <RefreshCw size={16} />刷新
        </button>
      </div>
      <div className="toolbar">
        <div className="search-field search-field--wide">
          <Search size={16} />
          <input aria-label="搜索审计事件" placeholder="事件、操作者、对象或 traceId" value={query} onChange={(event) => setQuery(event.target.value)} />
        </div>
        <label className="select-field">
          <Filter size={16} />
          <span className="sr-only">结果筛选</span>
          <select value={outcome} onChange={(event) => setOutcome(event.target.value as AuditEvent['outcome'] | '')}>
            <option value="">全部结果</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILURE">失败</option>
            <option value="DENIED">已拒绝</option>
          </select>
        </label>
      </div>
      {error && <div className="inline-message inline-message--error" role="alert">{error}</div>}
      {visible.length === 0 ? (
        <div className="empty-state"><ShieldAlert size={27} /><strong>没有匹配事件</strong><span>调整搜索或结果筛选条件。</span></div>
      ) : (
        <div className="table-scroll">
          <table className="data-table audit-table">
            <thead><tr><th>时间</th><th>事件</th><th>结果</th><th>操作者</th><th>对象</th><th>来源 IP</th><th>traceId</th><th>说明</th></tr></thead>
            <tbody>
              {visible.map((event) => (
                <tr key={event.id}>
                  <td>{formatDateTime(event.occurredAt)}</td>
                  <td><strong>{event.eventType}</strong></td>
                  <td><span className={`status-badge status-badge--${event.outcome.toLowerCase()}`}>{outcomeLabels[event.outcome]}</span></td>
                  <td>{event.actor ?? '匿名'}</td>
                  <td className="mono">{event.targetId ?? '—'}</td>
                  <td className="mono">{event.sourceIp ?? '—'}</td>
                  <td className="mono trace-cell" title={event.traceId}>{event.traceId ?? '—'}</td>
                  <td>{event.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
