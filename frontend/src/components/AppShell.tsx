import {
  Activity,
  BookOpenCheck,
  Building2,
  ChevronRight,
  CircleUserRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Network,
  ReceiptText,
  Scale,
  SendHorizontal,
  ShieldCheck,
  UsersRound,
  X,
  type LucideIcon,
} from 'lucide-react';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { UserRole } from '../types';

type NavigationItem = {
  to: string;
  label: string;
  shortLabel: string;
  icon: LucideIcon;
  roles?: UserRole[];
};

const navigation: NavigationItem[] = [
  { to: '/', label: '业务总览', shortLabel: '总览', icon: LayoutDashboard },
  { to: '/accounts', label: '账户管理', shortLabel: '账户', icon: Building2, roles: ['ADMIN', 'OPERATOR', 'AUDITOR'] },
  { to: '/transfers', label: '交易管理', shortLabel: '交易', icon: SendHorizontal, roles: ['ADMIN', 'OPERATOR', 'AUDITOR'] },
  { to: '/ledger', label: '流水核验', shortLabel: '流水', icon: ReceiptText, roles: ['ADMIN', 'OPERATOR', 'AUDITOR'] },
  { to: '/reconciliation', label: '批次对账', shortLabel: '对账', icon: Scale, roles: ['ADMIN', 'OPERATOR', 'AUDITOR'] },
  { to: '/audit', label: '安全审计', shortLabel: '审计', icon: BookOpenCheck, roles: ['ADMIN', 'AUDITOR'] },
  { to: '/users', label: '用户与权限', shortLabel: '用户', icon: UsersRound, roles: ['ADMIN'] },
  { to: '/system', label: '系统运行', shortLabel: '运行', icon: Activity, roles: ['ADMIN', 'OPERATOR', 'AUDITOR'] },
];

const pageTitles: Record<string, { title: string; description: string }> = {
  '/': { title: '业务总览', description: '账户资金、交易结果与运行状态' },
  '/accounts': { title: '账户管理', description: '开户、余额与账户状态' },
  '/transfers': { title: '交易管理', description: '转账处理、幂等重试与订单查询' },
  '/ledger': { title: '流水核验', description: '订单与双边账务流水证据' },
  '/reconciliation': { title: '批次对账', description: '最近交易抽样核验与差异追踪' },
  '/audit': { title: '安全审计', description: '登录、越权和管理操作追溯' },
  '/users': { title: '用户与权限', description: '平台用户和职责角色治理' },
  '/system': { title: '系统运行', description: '探针、监控入口与部署状态' },
};

const roleLabels: Record<UserRole, string> = {
  ADMIN: '系统管理员',
  OPERATOR: '业务操作员',
  AUDITOR: '审计员',
  CUSTOMER: '客户',
};

/** 统一的桌面侧栏、顶栏和移动底部导航。 */
export function AppShell({ children }: { children: ReactNode }) {
  const { hasAnyRole, logout, session } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const mainContentRef = useRef<HTMLElement>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const page = pageTitles[location.pathname] ?? pageTitles['/'];
  const visibleNavigation = navigation.filter((item) => !item.roles || hasAnyRole(...item.roles));
  const primaryRole = session.roles[0] ?? 'CUSTOMER';

  useEffect(() => {
    // 切换页面后关闭移动侧栏，并把键盘/读屏焦点移到新页面主体，避免焦点留在旧导航项。
    setMenuOpen(false);
    mainContentRef.current?.focus();
  }, [location.pathname]);

  async function signOut() {
    await logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-frame">
      <a className="skip-link" href="#main-content">跳到主要内容</a>
      {menuOpen && (
        <button className="nav-backdrop" aria-label="关闭导航" type="button" onClick={() => setMenuOpen(false)} />
      )}
      <aside className={`side-navigation ${menuOpen ? 'side-navigation--open' : ''}`}>
        <div className="side-navigation__brand">
          <div className="brand-lockup">
            <span className="brand-mark" aria-hidden="true">N</span>
            <strong>NorthLedger</strong>
          </div>
          <button className="icon-button side-navigation__close" aria-label="关闭导航" type="button" onClick={() => setMenuOpen(false)}>
            <X size={19} />
          </button>
        </div>

        <nav className="side-navigation__links" aria-label="主要导航">
          <span className="navigation-caption">业务</span>
          {visibleNavigation.map((item, index) => {
            const Icon = item.icon;
            const showDivider = index > 0 && item.to === '/audit';
            return (
              <div key={item.to} className={showDivider ? 'navigation-divider' : undefined}>
                {showDivider && <span className="navigation-caption">治理</span>}
                <NavLink to={item.to} end={item.to === '/'}>
                  <Icon size={18} aria-hidden="true" />
                  <span>{item.label}</span>
                  <ChevronRight className="navigation-arrow" size={15} aria-hidden="true" />
                </NavLink>
              </div>
            );
          })}
        </nav>

        <div className="side-navigation__context">
          <div><Network size={16} /><span>部署环境</span></div>
          <strong>NorthLedger</strong>
          <small>实际环境以“系统运行”页探针为准</small>
        </div>
      </aside>

      <section className="main-workspace">
        <header className="top-navigation">
          <button className="icon-button top-navigation__menu" aria-label="打开导航" type="button" onClick={() => setMenuOpen(true)}>
            <Menu size={21} />
          </button>
          <div className="top-navigation__title">
            <h1>{page.title}</h1>
            <p>{page.description}</p>
          </div>
          <div className="top-navigation__status">
            <span className="live-indicator"><i />服务会话</span>
          </div>
          <div className="user-menu">
            <CircleUserRound size={21} aria-hidden="true" />
            <div>
              <strong>{session.displayName ?? session.username}</strong>
              <span>{roleLabels[primaryRole]}</span>
            </div>
            <button className="icon-button" aria-label="退出登录" title="退出登录" type="button" onClick={() => void signOut()}>
              <LogOut size={17} />
            </button>
          </div>
        </header>

        <main ref={mainContentRef} id="main-content" className="workspace-content" tabIndex={-1}>{children}</main>
      </section>

      <nav className="mobile-navigation" aria-label="移动端导航">
        {visibleNavigation.slice(0, 5).map((item) => {
          const Icon = item.icon;
          return (
            <NavLink key={item.to} to={item.to} end={item.to === '/'}>
              <Icon size={20} aria-hidden="true" />
              <span>{item.shortLabel}</span>
            </NavLink>
          );
        })}
      </nav>
      <span className="security-signature" aria-hidden="true"><ShieldCheck size={14} />SESSION</span>
    </div>
  );
}
