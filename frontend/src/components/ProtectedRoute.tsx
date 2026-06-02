import { Navigate, Outlet } from 'react-router-dom';
import { isAuthenticated } from '../lib/auth';
import Layout from './Layout';

export default function ProtectedRoute() {
  if (!isAuthenticated()) return <Navigate to="/login" replace />;
  return (
    <Layout>
      <Outlet />
    </Layout>
  );
}
