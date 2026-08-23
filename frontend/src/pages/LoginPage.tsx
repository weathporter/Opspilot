import { ArrowRight, Check, Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/** NorthLedger 的双栏登录页，直接连接 Spring Security 用户名密码认证。 */
export function LoginPage() {
  const { loading, login, session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (!loading && session.authenticated) return <Navigate to="/" replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await login(username.trim(), password);
      const requestedDestination = (location.state as { from?: string } | null)?.from;
      // 只接受应用内部绝对路径，拒绝 //host 与反斜杠变体，避免把登录跳转变成开放重定向。
      const destination = requestedDestination?.startsWith('/')
        && !requestedDestination.startsWith('//')
        && !requestedDestination.includes('\\')
        ? requestedDestination
        : '/';
      navigate(destination, { replace: true });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '登录失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-story" aria-label="NorthLedger 平台简介">
        <div className="brand-lockup brand-lockup--login">
          <span className="brand-mark" aria-hidden="true">N</span>
          <strong>NorthLedger</strong>
        </div>
        <div className="login-story__content">
          <h1>让每一笔资金变动都有清晰依据。</h1>
          <p>账户、转账、双录流水与安全审计位于同一条可追溯链路，面向真实部署和日常运行。</p>
          <ul>
            <li><Check size={17} />事务内完成余额、订单和双边流水</li>
            <li><Check size={17} />服务端会话、角色权限与 CSRF 防护</li>
            <li><Check size={17} />指标、日志、告警与故障处置证据</li>
          </ul>
        </div>
        <div className="login-story__footer">
          <ShieldCheck size={18} />
          <span>登录行为会记录结果、来源与请求追踪号。</span>
        </div>
      </section>

      <section className="login-form-region">
        <form className="login-form" onSubmit={submit}>
          <div className="login-form__heading">
            <LockKeyhole size={24} />
            <h2>登录业务平台</h2>
            <p>请输入管理员、操作员或审计员账号。</p>
          </div>
          <label>
            <span>用户名</span>
            <input
              autoComplete="username"
              autoFocus
              maxLength={64}
              required
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
          </label>
          <label>
            <span>密码</span>
            <span className="password-field">
              <input
                autoComplete="current-password"
                maxLength={128}
                required
                type={passwordVisible ? 'text' : 'password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <button
                aria-label={passwordVisible ? '隐藏密码' : '显示密码'}
                aria-pressed={passwordVisible}
                type="button"
                onClick={() => setPasswordVisible((visible) => !visible)}
              >
                {passwordVisible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
              </button>
            </span>
          </label>
          {error && <div className="form-error" role="alert">{error}</div>}
          <button className="button button--primary login-submit" disabled={submitting} type="submit">
            <span>{submitting ? '正在验证…' : '安全登录'}</span>
            <ArrowRight size={18} />
          </button>
          <p className="login-form__note">账号由平台管理员创建；生产环境不会使用仓库内置密码。</p>
        </form>
      </section>
    </main>
  );
}
