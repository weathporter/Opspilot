import { ArrowRight, CheckCircle2, Scale } from 'lucide-react';
import type { TransferAudit } from '../types';
import { formatCurrency, formatDateTime } from '../lib/format';

/** 单侧流水卡片突出方向、余额前后和发生额，便于现场讲解双录记账。 */
function EntryCard({ entry }: { entry: TransferAudit['entries'][number] }) {
  const debit = entry.entryType === 'DEBIT';
  return (
    <article className={`ledger-entry ${debit ? 'ledger-entry--debit' : 'ledger-entry--credit'}`}>
      <header>
        <span>{debit ? '扣款流水' : '入账流水'}</span>
        <strong>{debit ? 'DEBIT' : 'CREDIT'}</strong>
      </header>
      <dl className="detail-list">
        <div><dt>流水号</dt><dd className="mono">#{entry.entryId}</dd></div>
        <div><dt>业务账户</dt><dd className="mono">{entry.accountNo}</dd></div>
        <div><dt>发生金额</dt><dd>{formatCurrency(entry.amount)}</dd></div>
        <div><dt>变动前余额</dt><dd>{formatCurrency(entry.balanceBefore)}</dd></div>
        <div><dt>变动后余额</dt><dd>{formatCurrency(entry.balanceAfter)}</dd></div>
        <div><dt>发生时间</dt><dd>{formatDateTime(entry.createdAt)}</dd></div>
      </dl>
    </article>
  );
}

/** 订单审计面板把两条流水放在同一视线内，并显示服务端平衡校验结果。 */
export function AuditPanel({ audit }: { audit: TransferAudit }) {
  return (
    <section className="audit-panel" aria-labelledby="audit-title">
      <div className="section-heading section-heading--inline">
        <div>
          <h2 id="audit-title">双录流水明细</h2>
          <p className="mono">订单：{audit.transfer.requestId}</p>
        </div>
        <span className={`balance-check ${audit.balanced ? 'balance-check--ok' : 'balance-check--error'}`}>
          {audit.balanced ? <CheckCircle2 size={16} /> : <Scale size={16} />}
          {audit.balanced ? '借贷平衡' : '需要复核'}
        </span>
      </div>
      <div className="ledger-pair">
        {audit.entries.map((entry, index) => (
          <div className="ledger-pair__item" key={entry.entryId}>
            <EntryCard entry={entry} />
            {index === 0 && <ArrowRight className="ledger-pair__arrow" size={20} aria-hidden="true" />}
          </div>
        ))}
      </div>
    </section>
  );
}
