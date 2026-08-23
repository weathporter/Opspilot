import { CheckCircle2, CopyCheck, Filter, RefreshCw, RotateCcw, Search, Send, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { AuditPanel } from '../components/AuditPanel';
import { ErrorState, LoadingState } from '../components/Feedback';
import { TransferTable } from '../components/TransferTable';
import { accountApi, transferApi } from '../lib/api';
import { createIdempotencyKey, formatCurrency, formatDateTime } from '../lib/format';
import type { Account, Transfer, TransferAudit, TransferStatus } from '../types';

type TransferForm = {
  sourceAccountNo: string;
  targetAccountNo: string;
  amount: string;
  requestId: string;
};

/** 转账中心实现真实提交、原键重放、历史筛选和双录流水读取。 */
export function TransfersPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transfers, setTransfers] = useState<Transfer[]>([]);
  const [audit, setAudit] = useState<TransferAudit | null>(null);
  const [result, setResult] = useState<Transfer | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<TransferStatus | ''>('');
  const [form, setForm] = useState<TransferForm>({
    sourceAccountNo: '',
    targetAccountNo: '',
    amount: '100.00',
    requestId: createIdempotencyKey(),
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [accountRows, transferRows] = await Promise.all([
        accountApi.list('', 100),
        transferApi.list(undefined, 100),
      ]);
      setAccounts(accountRows);
      setTransfers(transferRows);
      setForm((current) => ({
        ...current,
        sourceAccountNo: current.sourceAccountNo || accountRows[0]?.accountNo || '',
        targetAccountNo: current.targetAccountNo || accountRows[1]?.accountNo || '',
      }));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '转账数据读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const sourceAccount = accounts.find((account) => account.accountNo === form.sourceAccountNo);
  const targetAccount = accounts.find((account) => account.accountNo === form.targetAccountNo);

  const visibleTransfers = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return transfers.filter((transfer) => {
      const statusMatches = !status || transfer.status === status;
      const keywordMatches = !keyword || [transfer.requestId, transfer.sourceAccountNo, transfer.targetAccountNo]
        .some((value) => value.toLowerCase().includes(keyword));
      return statusMatches && keywordMatches;
    });
  }, [search, status, transfers]);

  async function submitTransfer(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const created = await transferApi.create(form.requestId.trim(), {
        sourceAccountNo: form.sourceAccountNo,
        targetAccountNo: form.targetAccountNo,
        amount: Number(form.amount),
      });
      setResult(created);
      setAudit(await transferApi.audit(created.requestId));
      setTransfers(await transferApi.list(undefined, 100));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '转账失败');
    } finally {
      setSubmitting(false);
    }
  }

  async function selectTransfer(transfer: Transfer) {
    setError('');
    try {
      setAudit(await transferApi.audit(transfer.requestId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '流水读取失败');
    }
  }

  function resetForm() {
    setResult(null);
    setAudit(null);
    setError('');
    setForm((current) => ({ ...current, amount: '100.00', requestId: createIdempotencyKey() }));
  }

  if (loading && accounts.length === 0) return <LoadingState label="正在读取账户与转账记录…" />;
  if (error && accounts.length === 0) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <div className="transfer-layout page-enter">
      <aside className="transfer-sidebar">
        <section className="panel form-panel">
          <div className="section-heading"><h2>发起一笔转账</h2><p>余额、订单和两条流水在同一事务提交。</p></div>
          {accounts.length < 2 ? (
            <div className="inline-message inline-message--warning">至少需要两个账户，请先在“账户管理”创建业务账户。</div>
          ) : (
            <form className="stacked-form" onSubmit={submitTransfer}>
              <label>
                <span>付款账户</span>
                <select value={form.sourceAccountNo} required onChange={(event) => setForm({ ...form, sourceAccountNo: event.target.value })}>
                  {accounts.map((account) => <option key={account.accountNo} value={account.accountNo}>{account.accountNo} · {account.holderName}</option>)}
                </select>
                <small>可用余额：{formatCurrency(sourceAccount?.balance)}</small>
              </label>
              <label>
                <span>收款账户</span>
                <select value={form.targetAccountNo} required onChange={(event) => setForm({ ...form, targetAccountNo: event.target.value })}>
                  {accounts.map((account) => <option key={account.accountNo} value={account.accountNo}>{account.accountNo} · {account.holderName}</option>)}
                </select>
                <small>当前余额：{formatCurrency(targetAccount?.balance)}</small>
              </label>
              <label>
                <span>转账金额</span>
                <div className="input-with-suffix">
                  <input value={form.amount} type="number" min="0.01" step="0.01" required onChange={(event) => setForm({ ...form, amount: event.target.value })} />
                  <span>CNY</span>
                </div>
              </label>
              <label>
                <span>请求编号（幂等键）</span>
                <input className="mono" value={form.requestId} maxLength={64} required onChange={(event) => setForm({ ...form, requestId: event.target.value })} />
                <small><CopyCheck size={13} />重复提交同一请求编号不会重复扣款。</small>
              </label>
              {form.sourceAccountNo === form.targetAccountNo && (
                <div className="inline-message inline-message--error">付款账户和收款账户不能相同。</div>
              )}
              {error && <div className="inline-message inline-message--error" role="alert">{error}</div>}
              <div className="form-actions">
                <button className="button button--secondary" type="button" onClick={resetForm}><Trash2 size={16} />清空</button>
                <button className="button button--primary" disabled={submitting || form.sourceAccountNo === form.targetAccountNo} type="submit"><Send size={16} />{submitting ? '处理中…' : '确认转账'}</button>
              </div>
            </form>
          )}
        </section>

        {result && (
          <section className="success-card success-card--transfer" role="status">
            <div className="success-card__title"><CheckCircle2 size={26} /><strong>转账成功</strong></div>
            <dl className="detail-list">
              <div><dt>订单编号</dt><dd className="mono">{result.requestId}</dd></div>
              <div><dt>转账金额</dt><dd>{formatCurrency(result.amount)}</dd></div>
              <div><dt>完成时间</dt><dd>{formatDateTime(result.completedAt)}</dd></div>
            </dl>
            <button className="button button--secondary button--wide" type="button" disabled={submitting} onClick={() => void submitTransfer()}>
              <RotateCcw size={16} />用同一幂等键重放
            </button>
          </section>
        )}
      </aside>

      <section className="transfer-workspace">
        <div className="panel table-panel">
          <div className="section-heading section-heading--inline">
            <div><h2>最近转账记录</h2><p>选择一行查看与之关联的双录流水</p></div>
            <button className="icon-button" type="button" aria-label="刷新转账记录" onClick={() => void load()}><RefreshCw size={17} /></button>
          </div>
          <div className="toolbar">
            <div className="search-field search-field--wide"><Search size={16} /><input value={search} placeholder="搜索请求编号或账号" aria-label="搜索转账" onChange={(event) => setSearch(event.target.value)} /></div>
            <label className="select-field"><Filter size={16} /><span className="sr-only">状态筛选</span><select value={status} onChange={(event) => setStatus(event.target.value as TransferStatus | '')}><option value="">全部状态</option><option value="COMPLETED">已完成</option><option value="PROCESSING">处理中</option></select></label>
          </div>
          <TransferTable transfers={visibleTransfers} selectedId={audit?.transfer.requestId} onSelect={(transfer) => void selectTransfer(transfer)} />
        </div>
        {audit && <AuditPanel audit={audit} />}
      </section>
    </div>
  );
}
