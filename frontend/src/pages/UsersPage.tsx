import { CheckCircle2, KeyRound, Plus, RefreshCw, ShieldCheck, UserRound } from 'lucide-react';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { ErrorState, LoadingState } from '../components/Feedback';
import { userApi } from '../lib/api';
import { formatDateTime } from '../lib/format';
import type { PlatformUser, UserRole } from '../types';

const availableRoles: Array<{ value: UserRole; label: string; detail: string }> = [
  { value: 'ADMIN', label: '管理员', detail: '用户治理与全部审计' },
  { value: 'OPERATOR', label: '操作员', detail: '账户和交易写操作' },
  { value: 'AUDITOR', label: '审计员', detail: '业务与安全证据只读' },
];

/** 管理员用户治理页，完成创建、角色配置、列表回读和无凭据泄露的闭环。 */
export function UsersPage() {
  const [users, setUsers] = useState<PlatformUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({ username: '', password: '', displayName: '', roles: ['OPERATOR'] as UserRole[] });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setUsers(await userApi.list());
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '用户读取失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  function toggleRole(role: UserRole) {
    setForm((current) => ({
      ...current,
      roles: current.roles.includes(role)
        ? current.roles.filter((item) => item !== role)
        : [...current.roles, role],
    }));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    setSuccess('');
    try {
      const created = await userApi.create({
        username: form.username.trim(),
        password: form.password,
        displayName: form.displayName.trim(),
        roles: form.roles,
      });
      setSuccess(`用户 ${created.username} 已创建，密码未出现在响应或日志中。`);
      setForm({ username: '', password: '', displayName: '', roles: ['OPERATOR'] });
      await load();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '用户创建失败');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading && users.length === 0) return <LoadingState label="正在读取平台用户…" />;
  if (error && users.length === 0) return <ErrorState message={error} onRetry={() => void load()} />;

  return (
    <div className="page-grid page-grid--form page-enter">
      <section className="panel form-panel">
        <div className="section-heading"><h2><UserRound size={19} />创建平台用户</h2><p>职责在后端再次校验，前端隐藏菜单不是安全边界。</p></div>
        <form className="stacked-form" onSubmit={submit}>
          <label><span>用户名</span><input required minLength={3} maxLength={64} pattern="[A-Za-z0-9._-]+" autoComplete="off" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></label>
          <label><span>显示名称</span><input required maxLength={64} value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} /></label>
          <label><span>初始密码</span><input required minLength={12} maxLength={128} type="password" autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /><small><KeyRound size={13} />至少 12 个字符，只在提交时发送。</small></label>
          <fieldset className="role-picker"><legend>职责角色</legend>{availableRoles.map((role) => <label key={role.value}><input type="checkbox" checked={form.roles.includes(role.value)} onChange={() => toggleRole(role.value)} /><span><strong>{role.label}</strong><small>{role.detail}</small></span></label>)}</fieldset>
          {error && <div className="inline-message inline-message--error" role="alert">{error}</div>}
          {success && <div className="inline-message inline-message--success" role="status"><CheckCircle2 size={15} />{success}</div>}
          <button className="button button--primary button--wide" disabled={submitting || form.roles.length === 0} type="submit"><Plus size={16} />{submitting ? '正在创建…' : '创建用户'}</button>
        </form>
      </section>

      <section className="panel table-panel">
        <div className="section-heading section-heading--inline"><div><h2><ShieldCheck size={19} />用户与职责</h2><p>共 {users.length} 个用户，密码字段不属于此接口契约。</p></div><button className="icon-button" aria-label="刷新用户" type="button" onClick={() => void load()}><RefreshCw size={17} /></button></div>
        <div className="table-scroll"><table className="data-table user-table"><thead><tr><th>用户</th><th>登录名</th><th>角色</th><th>状态</th><th>创建时间</th></tr></thead><tbody>{users.map((user) => <tr key={user.id}><td><strong>{user.displayName}</strong></td><td className="mono">{user.username}</td><td><div className="role-list">{user.roles.map((role) => <span key={role}>{role}</span>)}</div></td><td><span className={`status-badge status-badge--${user.status === 'ACTIVE' ? 'success' : 'failure'}`}>{user.status}</span></td><td>{formatDateTime(user.createdAt)}</td></tr>)}</tbody></table></div>
      </section>
    </div>
  );
}
