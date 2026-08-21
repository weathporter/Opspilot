import { Search, ShieldCheck } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AuditPanel } from '../components/AuditPanel';
import { ErrorState, LoadingState } from '../components/Feedback';
import { TransferTable } from '../components/TransferTable';
import { transferApi } from '../lib/api';
import type { Transfer, TransferAudit } from '../types';

/** 流水审计页从订单进入双录证据，适合面试现场解释对账与可追溯性。 */
export function LedgerPage() {
  const [transfers, setTransfers] = useState<Transfer[]>([]);
  const [audit, setAudit] = useState<TransferAudit | null>(null);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const rows = await transferApi.list(undefined, 100);
      setTransfers(rows);
      if (rows[0]) setAudit(await transferApi.audit(rows[0].requestId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '流水审计数据读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = useMemo(() => {
    const keyword = query.trim();
    if (!keyword) return transfers;
    return transfers.filter((transfer) => [transfer.requestId, transfer.sourceAccountNo, transfer.targetAccountNo].some((value) => value.includes(keyword)));
  }, [query, transfers]);

  async function selectTransfer(transfer: Transfer) {
    setError('');
    try {
      setAudit(await transferApi.audit(transfer.requestId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '流水读取失败');
    }
  }

  if (loading) return <LoadingState label="正在核对订单与双录流水…" />;
  if (error && transfers.length === 0) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <div className="page-stack page-enter">
      <div className="audit-intro">
        <ShieldCheck size={22} />
        <div><strong>审计口径</strong><span>一笔完成订单必须对应一条 DEBIT 和一条 CREDIT，且发生额相等。</span></div>
      </div>
      <section className="panel table-panel">
        <div className="section-heading section-heading--inline">
          <div><h2>订单索引</h2><p>从业务订单向下追踪资金流水</p></div>
          <div className="search-field"><Search size={16} /><input value={query} aria-label="搜索订单" placeholder="请求编号或账号" onChange={(event) => setQuery(event.target.value)} /></div>
        </div>
        {error && <div className="inline-message inline-message--error">{error}</div>}
        <TransferTable transfers={visible} selectedId={audit?.transfer.requestId} onSelect={(transfer) => void selectTransfer(transfer)} compact />
      </section>
      {audit && <AuditPanel audit={audit} />}
    </div>
  );
}
