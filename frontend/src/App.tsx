import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { AccountsPage } from './pages/AccountsPage';
import { DashboardPage } from './pages/DashboardPage';
import { LedgerPage } from './pages/LedgerPage';
import { SystemPage } from './pages/SystemPage';
import { TransfersPage } from './pages/TransfersPage';

/**
 * App 只负责组合应用壳与路由，业务数据和交互分别留在页面组件中。
 * 通配路由回到总览，避免用户输入错误地址后看到空白页面。
 */
export default function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/accounts" element={<AccountsPage />} />
        <Route path="/transfers" element={<TransfersPage />} />
        <Route path="/ledger" element={<LedgerPage />} />
        <Route path="/system" element={<SystemPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}
