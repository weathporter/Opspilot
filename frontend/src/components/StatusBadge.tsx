import { CheckCircle2, Clock3 } from 'lucide-react';
import type { TransferStatus } from '../types';

/** 状态不能只依赖颜色，图标和文字让色觉障碍用户也能辨别。 */
export function TransferStatusBadge({ status }: { status: TransferStatus }) {
  const completed = status === 'COMPLETED';
  const Icon = completed ? CheckCircle2 : Clock3;
  return (
    <span className={`status-badge ${completed ? 'status-badge--success' : 'status-badge--warning'}`}>
      <Icon size={14} aria-hidden="true" />
      {completed ? '已完成' : '处理中'}
    </span>
  );
}
