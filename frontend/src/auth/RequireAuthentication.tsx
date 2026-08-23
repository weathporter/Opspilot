import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { LoadingState } from '../components/Feedback';
import { useAuth } from './AuthContext';

/**
 * 路由体验层的登录门禁。它避免匿名用户看到页面闪烁，但不能替代后端 RBAC；
 * 手工请求 API 仍会由 Spring Security 再次校验。
 */
export function RequireAuthentication() {
  const { loading, session } = useAuth();
  const location = useLocation();

  if (loading) return <LoadingState label="正在确认安全会话…" />;
  if (!session.authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}
