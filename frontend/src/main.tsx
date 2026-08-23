import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './auth/AuthContext';
import './styles.css';

/**
 * BrowserRouter 让导航状态体现在 URL 中，刷新或分享 /transfers 等地址仍能回到同一页面。
 * Nginx 的 try_files 回退会把未知前端路由交给 index.html。
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
