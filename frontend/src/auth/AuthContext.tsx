import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { authApi } from '../lib/api';
import type { AuthSession, UserRole } from '../types';

type AuthContextValue = {
  session: AuthSession;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasAnyRole: (...roles: UserRole[]) => boolean;
};

const anonymousSession: AuthSession = { authenticated: false, roles: [] };
const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * 全局身份状态只保存后端返回的最小用户视图，不保存 SESSION ID 或密码。
 * 页面刷新时重新向服务端确认，服务端仍是身份和权限的唯一事实来源。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession>(anonymousSession);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      setSession(await authApi.session());
    } catch {
      // API 不可达时登录页仍应可渲染，并给用户一次手工重试机会。
      setSession(anonymousSession);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
    const expire = () => setSession(anonymousSession);
    window.addEventListener('northledger:session-expired', expire);
    return () => window.removeEventListener('northledger:session-expired', expire);
  }, [refresh]);

  const login = useCallback(async (username: string, password: string) => {
    setSession(await authApi.login(username, password));
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setSession(anonymousSession);
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    session,
    loading,
    login,
    logout,
    hasAnyRole: (...roles) => roles.some((role) => session.roles.includes(role)),
  }), [loading, login, logout, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** 所有消费身份的组件必须位于 AuthProvider 内，错误用法立即失败而不是静默匿名。 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
