import { AlertTriangle, LoaderCircle, RefreshCw } from 'lucide-react';

/** 页面级加载状态保留稳定高度，避免内容到达时产生明显布局跳动。 */
export function LoadingState({ label = '正在读取真实运行数据…' }: { label?: string }) {
  return (
    <div className="feedback-state" role="status" aria-live="polite">
      <LoaderCircle className="spin" size={24} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

/** 错误状态提供明确原因和原地重试，不要求用户刷新整个浏览器。 */
export function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="feedback-state feedback-state--error" role="alert">
      <AlertTriangle size={24} aria-hidden="true" />
      <div>
        <strong>数据读取失败</strong>
        <span>{message}</span>
      </div>
      <button className="button button--secondary" type="button" onClick={onRetry}>
        <RefreshCw size={16} aria-hidden="true" />
        重新读取
      </button>
    </div>
  );
}
