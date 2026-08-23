import { ChevronRight, Inbox } from 'lucide-react';
import type { Transfer } from '../types';
import { formatCurrency, formatDateTime, maskAccount } from '../lib/format';
import { TransferStatusBadge } from './StatusBadge';

interface TransferTableProps {
  transfers: Transfer[];
  selectedId?: string;
  onSelect?: (transfer: Transfer) => void;
  compact?: boolean;
}

/**
 * 统一的转账表格同时服务运行总览、转账中心和流水审计。
 * 行使用 button-like 键盘交互，点击后可继续读取双录流水，而不是停在静态列表。
 */
export function TransferTable({ transfers, selectedId, onSelect, compact = false }: TransferTableProps) {
  if (transfers.length === 0) {
    return (
      <div className="empty-state">
        <Inbox size={24} aria-hidden="true" />
        <strong>还没有转账记录</strong>
        <span>先创建两个账户，再发起一笔转账即可形成订单与双录流水。</span>
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table className={`data-table ${compact ? 'data-table--compact' : ''}`}>
        <thead>
          <tr>
            <th scope="col">请求编号</th>
            <th scope="col">付款账户</th>
            <th scope="col">收款账户</th>
            <th scope="col">金额</th>
            <th scope="col">状态</th>
            <th scope="col">时间</th>
            {onSelect && <th scope="col"><span className="sr-only">查看详情</span></th>}
          </tr>
        </thead>
        <tbody>
          {transfers.map((transfer) => (
            <tr
              key={transfer.requestId}
              className={`${selectedId === transfer.requestId ? 'data-table__row--selected' : ''} ${onSelect ? 'data-table__row--interactive' : ''}`}
              tabIndex={onSelect ? 0 : undefined}
              onClick={() => onSelect?.(transfer)}
              onKeyDown={(event) => {
                if (onSelect && (event.key === 'Enter' || event.key === ' ')) {
                  event.preventDefault();
                  onSelect(transfer);
                }
              }}
            >
              <td className="mono" title={transfer.requestId}>{transfer.requestId}</td>
              <td title={transfer.sourceAccountNo}>{maskAccount(transfer.sourceAccountNo)}</td>
              <td title={transfer.targetAccountNo}>{maskAccount(transfer.targetAccountNo)}</td>
              <td className="amount-cell">{formatCurrency(transfer.amount)}</td>
              <td><TransferStatusBadge status={transfer.status} /></td>
              <td>{formatDateTime(transfer.completedAt ?? transfer.createdAt)}</td>
              {onSelect && <td><ChevronRight size={16} aria-hidden="true" /></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
