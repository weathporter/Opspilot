/** 后端 AccountResponse 的前端类型镜像，字段名与 JSON 契约保持一致。 */
export interface Account {
  accountNo: string;
  holderName: string;
  balance: number;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

/** 当前项目的订单只有处理中和已完成两种持久状态；失败事务不会留下半成品订单。 */
export type TransferStatus = 'PROCESSING' | 'COMPLETED';

/** 单笔转账只读快照。 */
export interface Transfer {
  requestId: string;
  sourceAccountNo: string;
  targetAccountNo: string;
  amount: number;
  status: TransferStatus;
  createdAt: string;
  completedAt?: string;
}

/** 双录流水中的借记或贷记证据。 */
export interface LedgerEntry {
  entryId: number;
  accountNo: string;
  entryType: 'DEBIT' | 'CREDIT';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  createdAt: string;
}

/** 订单、双边流水和平衡校验组成完整审计响应。 */
export interface TransferAudit {
  transfer: Transfer;
  entries: LedgerEntry[];
  balanced: boolean;
}

/** 近 24 小时图表的单个整点数据。 */
export interface TransferTrendPoint {
  hour: string;
  completedCount: number;
  processingCount: number;
  completedAmount: number;
}

/** 运行总览一次请求即可获得的聚合读模型。 */
export interface OperationsSummary {
  generatedAt: string;
  environment: string;
  version: string;
  accountCount: number;
  totalBalance: number;
  transferCount: number;
  completedTransferCount: number;
  completedTransferVolume: number;
  successRate: number;
  trend: TransferTrendPoint[];
  recentTransfers: Transfer[];
}

/** Operations 独立保存的最近交易抽样对账批次，不代表全库历史已经核验。 */
export interface ReconciliationRun {
  id: number;
  completedAt: string;
  checkedCount: number;
  discrepancyCount: number;
  status: 'BALANCED' | 'DISCREPANCY';
}

/** 单批次详情仅保存不平衡交易的 requestId，用于回到双边流水页进一步取证。 */
export interface ReconciliationDetail extends ReconciliationRun {
  discrepancyRequestIds: string[];
}

/** Spring ProblemDetail 扩展响应，用于把后端业务错误转换为可读表单提示。 */
export interface ApiProblem {
  title?: string;
  detail?: string;
  code?: string;
  status?: number;
  traceId?: string;
  errors?: Record<string, string>;
}

/** 服务端 SESSION 的最小前端视图；真正的 SESSION ID 只存在 HttpOnly Cookie。 */
export interface AuthSession {
  authenticated: boolean;
  username?: string;
  displayName?: string;
  roles: UserRole[];
}

/** 与后端 UserRole 一致的固定职责集合。 */
export type UserRole = 'ADMIN' | 'OPERATOR' | 'AUDITOR' | 'CUSTOMER';

/** 管理台用户响应刻意不包含密码或 passwordHash。 */
export interface PlatformUser {
  id: number;
  username: string;
  displayName: string;
  status: 'ACTIVE' | 'LOCKED' | 'DISABLED';
  roles: UserRole[];
  createdAt: string;
  updatedAt: string;
}

/** 安全与业务审计事件。 */
export interface AuditEvent {
  id: number;
  eventType: string;
  outcome: 'SUCCESS' | 'FAILURE' | 'DENIED';
  actor?: string;
  targetType?: string;
  targetId?: string;
  traceId?: string;
  sourceIp?: string;
  description: string;
  occurredAt: string;
}
