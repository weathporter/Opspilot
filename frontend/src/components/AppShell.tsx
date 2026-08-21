import {
  Activity,
  BookOpenCheck,
  Landmark,
  Menu,
  MonitorCog,
  Network,
  Send,
  UsersRound,
  X,
} from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

/** 页面路径、中文名称和图标集中维护，避免侧栏与移动导航出现不一致。 */
const navigation = [
  { path: '/', label: '运行总览', icon: Activity },
  { path: '/accounts', label: '账户管理', icon: UsersRound },
  { path: '/transfers', label: '转账中心', icon: Send },
  { path: '/ledger', label: '流水审计', icon: BookOpenCheck },
  { path: '/system', label: '系统状态', icon: MonitorCog },
];

const pageTitles: Record<string, string> = {
  '/': '运行总览',
  '/accounts': '账户管理',
  '/transfers': '转账中心',
  '/ledger': '流水审计',
  '/system': '系统状态',
};

/** 用 CSS + SVG 构造清晰品牌标记，不引入额外位图资产或网络依赖。 */
function BrandMark() {
  return (
    <div className="brand-mark" aria-hidden="true">
      <Network size={22} strokeWidth={2.2} />
    </div>
  );
}

/**
 * 全局应用壳负责一致导航、环境标识、移动端抽屉和主内容区域。
 * 各业务页面只渲染自己的内容，避免重复布局和响应式逻辑。
 */
export function AppShell({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const title = pageTitles[location.pathname] ?? 'OpsPilot';

  const closeMobileNavigation = () => setMobileOpen(false);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">跳到主要内容</a>
      <aside className={`sidebar ${mobileOpen ? 'sidebar--open' : ''}`} aria-label="主导航">
        <div className="brand">
          <BrandMark />
          <div>
            <strong>OpsPilot</strong>
            <span>金融业务运维驾驶舱</span>
          </div>
          <button
            className="icon-button sidebar__close"
            type="button"
            aria-label="关闭导航"
            onClick={closeMobileNavigation}
          >
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar__nav">
          {navigation.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`}
                onClick={closeMobileNavigation}
              >
                <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="operator-card">
          <div className="operator-card__avatar">OP</div>
          <div>
            <strong>ops_admin</strong>
            <span>运维管理员</span>
          </div>
        </div>
      </aside>

      {mobileOpen && (
        <button
          type="button"
          className="mobile-backdrop"
          aria-label="关闭导航遮罩"
          onClick={closeMobileNavigation}
        />
      )}

      <div className="workspace">
        <header className="topbar">
          <div className="topbar__title">
            <button
              className="icon-button topbar__menu"
              type="button"
              aria-label="打开导航"
              onClick={() => setMobileOpen(true)}
            >
              <Menu size={21} />
            </button>
            <h1>{title}</h1>
          </div>
          <div className="topbar__actions">
            <div className="environment-switch" title="当前由本地 Docker Compose 或 Minikube 提供服务">
              <Landmark size={16} aria-hidden="true" />
              <span>本地演示环境</span>
            </div>
            <button className="button button--primary topbar__cta" onClick={() => navigate('/transfers')}>
              <Send size={17} aria-hidden="true" />
              发起转账
            </button>
          </div>
        </header>
        <main className="content" id="main-content" tabIndex={-1}>{children}</main>
      </div>
    </div>
  );
}
