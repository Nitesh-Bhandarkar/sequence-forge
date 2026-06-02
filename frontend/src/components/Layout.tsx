import { NavLink, useNavigate } from 'react-router-dom';
import { clearAuth, getTenantId } from '../lib/auth';

const NAV_ITEMS = [
  { to: '/dashboard', label: '⊞  Dashboard' },
  { to: '/templates', label: '⊟  Templates' },
  { to: '/apikeys', label: '⚷  API Keys' },
  { to: '/audit', label: '≡  Audit Log' },
];

export default function Layout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const tenantId = getTenantId();

  const handleLogout = () => {
    clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className="w-60 bg-white border-r border-gray-200 flex flex-col">

        {/* Logo */}
        <div className="px-6 py-5 border-b border-gray-200">
          <span className="text-lg font-bold text-brand-600">Sequence Forge</span>
        </div>

        {/* Nav links */}
        <nav className="flex-1 px-3 py-4 space-y-0.5">
          {NAV_ITEMS.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Tenant + logout */}
        <div className="px-4 py-4 border-t border-gray-200 space-y-3">
          {tenantId && (
            <p className="text-xs text-gray-400 truncate" title={tenantId}>
              Tenant: {tenantId.slice(0, 8)}…
            </p>
          )}
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 transition-colors"
          >
            <span>↩</span> Sign out
          </button>
        </div>

      </aside>
      <main className="flex-1 overflow-auto">{children}</main>
    </div>
  );
}
