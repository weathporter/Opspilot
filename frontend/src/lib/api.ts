import type {
  Account,
  ApiProblem,
  OperationsSummary,
  Transfer,
  TransferAudit,
  TransferStatus,
} from '../types';

/**
 * 统一封装 fetch 的成功判断、JSON 解析和业务错误转换。
 * 页面组件只关心“得到数据或捕获 Error”，避免每个页面复制响应处理代码。
 */
async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let problem: ApiProblem = {};
    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      // 代理层 502/504 可能返回 HTML；解析失败时仍保留 HTTP 状态作为排障线索。
    }
    throw new Error(problem.detail ?? problem.title ?? `请求失败（HTTP ${response.status}）`);
  }

  return (await response.json()) as T;
}

/** 账户模块 API。 */
export const accountApi = {
  list: (query = '', limit = 100) =>
    request<Account[]>(`/api/v1/accounts?query=${encodeURIComponent(query)}&limit=${limit}`),
  create: (payload: { accountNo: string; holderName: string; openingBalance: number }) =>
    request<Account>('/api/v1/accounts', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
};

/** 转账和流水审计 API。 */
export const transferApi = {
  list: (status?: TransferStatus, limit = 100) => {
    const statusQuery = status ? `&status=${status}` : '';
    return request<Transfer[]>(`/api/v1/transfers?limit=${limit}${statusQuery}`);
  },
  create: (
    idempotencyKey: string,
    payload: { sourceAccountNo: string; targetAccountNo: string; amount: number },
  ) =>
    request<Transfer>('/api/v1/transfers', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(payload),
    }),
  audit: (requestId: string) =>
    request<TransferAudit>(`/api/v1/transfers/${encodeURIComponent(requestId)}/ledger`),
};

/** 运行总览和探针 API。 */
export const operationsApi = {
  summary: () => request<OperationsSummary>('/api/v1/operations/summary'),
  readiness: () => request<{ status: string }>('/health'),
};
