import { ShieldX } from 'lucide-react';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { RequireAuthentication } from './auth/RequireAuthentication';
import { AppShell } from './components/AppShell';
import { AccountsPage } from './pages/AccountsPage';
import { AuditEventsPage } from './pages/AuditEventsPage';
import { DashboardPage } from './pages/DashboardPage';
import { LedgerPage } from './pages/LedgerPage';
import { LoginPage } from './pages/LoginPage';
import { ReconciliationPage } from './pages/ReconciliationPage';
import { SystemPage } from './pages/SystemPage';
import { TransfersPage } from './pages/TransfersPage';
import { UsersPage } from './pages/UsersPage';
import type { UserRole } from './types';

/** 认证后的共同页面壳；Outlet 由当前子路由替换。 */
function ProtectedLayout() {
  return <AppShell><Outlet /></AppShell>;
}

/**
 * 前端角色门禁用于给用户清晰反馈，后端 Spring Security 仍会独立执行同一权限矩阵。
 * 这避免把“看不到菜单”误当作真正的访问控制。
 */
function RoleGate({ roles }: { roles: UserRole[] }) {
  const { hasAnyRole } = useAuth();
  if (hasAnyRole(...roles)) return <Outlet />;
  return (
    <section className="feedback-state feedback-state--forbidden">
      <ShieldX size={34} />
      <h2>没有访问权限</h2>
      <p>当前职责不能查看该页面；服务端也会拒绝直接构造的 API 请求。</p>
    </section>
  );
}

/** 路由定义保持“公开登录页 → 认证门禁 → 页面壳 → 角色页面”的清晰层级。 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuthentication />}>
        <Route element={<ProtectedLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="accounts" element={<AccountsPage />} />
          <Route path="transfers" element={<TransfersPage />} />
          <Route path="ledger" element={<LedgerPage />} />
          <Route path="reconciliation" element={<ReconciliationPage />} />
          <Route element={<RoleGate roles={['ADMIN', 'AUDITOR']} />}>
            <Route path="audit" element={<AuditEventsPage />} />
          </Route>
          <Route element={<RoleGate roles={['ADMIN']} />}>
            <Route path="users" element={<UsersPage />} />
          </Route>
          <Route path="system" element={<SystemPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Route>
    </Routes>
  );
}
