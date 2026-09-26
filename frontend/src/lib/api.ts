import type {
  Account,
  ApiProblem,
  AuditEvent,
  AuthSession,
  OperationsSummary,
  PlatformUser,
  ReconciliationDetail,
  ReconciliationRun,
  Transfer,
  TransferAudit,
  TransferStatus,
  UserRole,
} from '../types';

/** 带 HTTP 状态、业务码和 traceId 的前端异常，便于页面区分登录失效、越权和业务冲突。 */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
    readonly traceId?: string,
    readonly fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/** 从浏览器 Cookie 中读取原始 CSRF Token；SESSION 因 HttpOnly 无法也不应被读取。 */
function readCookie(name: string): string | undefined {
  const prefix = `${encodeURIComponent(name)}=`;
  return document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))
    ?.slice(prefix.length);
}

/** GET/HEAD/OPTIONS 不修改状态；其余方法必须发送双提交 CSRF 请求头。 */
function isUnsafeMethod(method = 'GET'): boolean {
  return !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method.toUpperCase());
}

/**
 * 全站唯一 fetch 边界：统一携带同源 Cookie、CSRF、ProblemDetail 解析和登录失效通知。
 * 组件只接收类型化数据，不复制安全头或错误处理代码。
 */
export async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const method = init.method ?? 'GET';
  if (init.body instanceof URLSearchParams) {
    headers.set('Content-Type', 'application/x-www-form-urlencoded;charset=UTF-8');
  } else if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (isUnsafeMethod(method)) {
    const csrfToken = readCookie('XSRF-TOKEN');
    if (csrfToken) headers.set('X-XSRF-TOKEN', decodeURIComponent(csrfToken));
  }

  const response = await fetch(url, {
    ...init,
    method,
    credentials: 'same-origin',
    headers,
  });

  if (!response.ok) {
    let problem: ApiProblem = {};
    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      // Nginx 的 502/504 可能不是 JSON；下面仍用 HTTP 状态生成可排障提示。
    }
    if (response.status === 401 && url !== '/api/v1/auth/session') {
      window.dispatchEvent(new Event('northledger:session-expired'));
    }
    throw new ApiError(
      problem.detail ?? problem.title ?? `请求失败（HTTP ${response.status}）`,
      response.status,
      problem.code,
      problem.traceId,
      problem.errors,
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/** 登录、会话刷新和注销。登录后再次 GET 是为了取得认证成功后轮换的新 CSRF Token。 */
export const authApi = {
  session: () => request<AuthSession>('/api/v1/auth/session'),
  async login(username: string, password: string) {
    await request<AuthSession>('/api/v1/auth/session', {
      method: 'POST',
      body: new URLSearchParams({ username, password }),
    });
    return request<AuthSession>('/api/v1/auth/session');
  },
  logout: () => request<AuthSession>('/api/v1/auth/session', { method: 'DELETE' }),
};

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

/** 转账和双录流水 API。 */
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
  reconciliationRuns: () => request<ReconciliationRun[]>('/api/v1/operations/reconciliations?limit=20'),
  reconciliationDetail: (id: number) => request<ReconciliationDetail>(`/api/v1/operations/reconciliations/${id}`),
  runReconciliation: (limit = 100) => request<ReconciliationRun>(
    `/api/v1/operations/reconciliations?limit=${limit}`, { method: 'POST' },
  ),
};

/** 审计员/管理员只读事件查询。 */
export const auditApi = {
  list: (limit = 100) => request<AuditEvent[]>(`/api/v1/audit/events?limit=${limit}`),
};

/** 管理员用户治理 API；创建请求中的密码不会出现在响应。 */
export const userApi = {
  list: () => request<PlatformUser[]>('/api/v1/admin/users'),
  create: (payload: {
    username: string;
    password: string;
    displayName: string;
    roles: UserRole[];
  }) => request<PlatformUser>('/api/v1/admin/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
};
