/** 统一使用人民币格式，避免每个组件各自拼接货币符号和千分位。 */
const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 2,
});

/** 数字转换失败时回退到 0，保证监控页面不会因为单个空值整体崩溃。 */
export function formatCurrency(value: number | string | undefined): string {
  const numeric = Number(value ?? 0);
  return currencyFormatter.format(Number.isFinite(numeric) ? numeric : 0);
}

/** 使用本地时区展示秒级时间；后端数据库统一保存微秒精度。 */
export function formatDateTime(value?: string): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ');
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date);
}

/** 表格中只露出账号首尾，完整账号仍保留在 title 和审计详情中。 */
export function maskAccount(accountNo: string): string {
  if (accountNo.length <= 8) return accountNo;
  return `${accountNo.slice(0, 4)} ···· ${accountNo.slice(-4)}`;
}

/** 生成适合人工识别的本地演示幂等键；提交重试时输入框会保留同一个值。 */
export function createIdempotencyKey(): string {
  const timestamp = new Date().toISOString().replace(/\D/g, '').slice(0, 14);
  const random = Math.random().toString(36).slice(2, 8);
  return `web-${timestamp}-${random}`;
}
