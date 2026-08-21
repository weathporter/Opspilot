import { CheckCircle2, Landmark, Plus, Search, WalletCards } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { ErrorState, LoadingState } from '../components/Feedback';
import { accountApi } from '../lib/api';
import { formatCurrency, formatDateTime } from '../lib/format';
import type { Account } from '../types';

/** 生成 16 位本地演示账号；后端唯一约束仍是并发冲突的最终防线。 */
function createDemoAccountNo(): string {
  const suffix = `${Date.now()}${Math.floor(Math.random() * 1000)}`.slice(-14);
  return `62${suffix}`;
}

/** 账户管理页完成“开户 → 列表回读 → 可用于转账”的第一段业务闭环。 */
export function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState<Account | null>(null);
  const [form, setForm] = useState({
    accountNo: createDemoAccountNo(),
    holderName: '',
    openingBalance: '10000.00',
  });

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setAccounts(await accountApi.list('', 100));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '账户读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAccounts();
  }, [loadAccounts]);

  const filteredAccounts = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return accounts;
    return accounts.filter((account) =>
      account.accountNo.includes(keyword) || account.holderName.toLowerCase().includes(keyword),
    );
  }, [accounts, query]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    setSuccess(null);
    try {
      const created = await accountApi.create({
        accountNo: form.accountNo.trim(),
        holderName: form.holderName.trim(),
        openingBalance: Number(form.openingBalance),
      });
      setSuccess(created);
      setForm({ accountNo: createDemoAccountNo(), holderName: '', openingBalance: '10000.00' });
      await loadAccounts();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '开户失败');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading && accounts.length === 0) return <LoadingState label="正在从 MySQL 读取账户…" />;
  if (error && accounts.length === 0) return <ErrorState message={error} onRetry={() => void loadAccounts()} />;

  return (
    <div className="page-grid page-grid--form page-enter">
      <section className="panel form-panel">
        <div className="section-heading">
          <h2>创建演示账户</h2>
          <p>开户成功后立即写入 MySQL，并可在转账中心选择。</p>
        </div>
        <form onSubmit={handleSubmit} className="stacked-form">
          <label>
            <span>业务账号</span>
            <input
              value={form.accountNo}
              inputMode="numeric"
              pattern="[0-9]{8,32}"
              required
              onChange={(event) => setForm({ ...form, accountNo: event.target.value })}
            />
            <small>8～32 位数字；数据库唯一约束防止重复开户。</small>
          </label>
          <label>
            <span>账户名称</span>
            <input
              value={form.holderName}
              maxLength={64}
              required
              placeholder="例如：秋招演示主账户"
              onChange={(event) => setForm({ ...form, holderName: event.target.value })}
            />
          </label>
          <label>
            <span>开户余额</span>
            <div className="input-with-suffix">
              <input
                value={form.openingBalance}
                type="number"
                min="0"
                step="0.01"
                required
                onChange={(event) => setForm({ ...form, openingBalance: event.target.value })}
              />
              <span>CNY</span>
            </div>
          </label>
          {error && <div className="inline-message inline-message--error" role="alert">{error}</div>}
          <button className="button button--primary button--wide" disabled={submitting} type="submit">
            <Plus size={17} />
            {submitting ? '正在写入…' : '确认开户'}
          </button>
        </form>

        {success && (
          <div className="success-card" role="status">
            <CheckCircle2 size={25} />
            <div><strong>账户创建成功</strong><span className="mono">{success.accountNo}</span></div>
            <b>{formatCurrency(success.balance)}</b>
          </div>
        )}
      </section>

      <section className="panel table-panel">
        <div className="section-heading section-heading--inline">
          <div><h2>账户清单</h2><p>共 {accounts.length} 个账户，数据来自后端列表 API</p></div>
          <div className="search-field">
            <Search size={16} aria-hidden="true" />
            <input value={query} placeholder="搜索账号或名称" aria-label="搜索账户" onChange={(event) => setQuery(event.target.value)} />
          </div>
        </div>
        {filteredAccounts.length === 0 ? (
          <div className="empty-state"><WalletCards size={26} /><strong>没有匹配账户</strong><span>修改搜索条件或创建第一个演示账户。</span></div>
        ) : (
          <div className="account-list">
            {filteredAccounts.map((account) => (
              <article key={account.accountNo}>
                <div className="account-list__icon"><Landmark size={19} /></div>
                <div className="account-list__main">
                  <strong>{account.holderName}</strong>
                  <span className="mono">{account.accountNo}</span>
                </div>
                <div className="account-list__balance"><strong>{formatCurrency(account.balance)}</strong><span>{formatDateTime(account.updatedAt)} 更新</span></div>
                <span className="status-badge status-badge--success"><CheckCircle2 size={13} />正常</span>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
