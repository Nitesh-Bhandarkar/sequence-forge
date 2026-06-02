import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchTemplates } from '../api/templates';
import { fetchAuditLog } from '../api/sequences';
import type { AuditResponse } from '../types';

export default function DashboardPage() {
  const { data: templates = [] } = useQuery({ queryKey: ['templates'], queryFn: fetchTemplates });
  const { data: audit } = useQuery({
    queryKey: ['audit', 'recent'],
    queryFn: () => fetchAuditLog({ size: 5 }),
  });

  const recentAudit: AuditResponse[] = audit?.content ?? [];

  return (
    <div className="p-8 space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Dashboard</h2>
        <p className="text-sm text-gray-500 mt-1">Overview of your sequence templates and recent activity</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard label="Templates" value={templates.length} />
        <StatCard label="Sequences today" value={recentAudit.length} />
        <StatCard label="Active templates" value={templates.filter(t => t.placeholders.length > 0).length} />
      </div>

      {/* Quick actions */}
      <div className="flex gap-3">
        <Link to="/templates/new" className="btn-primary">+ New Template</Link>
        <Link to="/audit" className="btn-secondary">View Audit Log</Link>
      </div>

      {/* Recent sequences */}
      <div className="card overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100">
          <h3 className="text-sm font-semibold text-gray-700">Recent sequences</h3>
        </div>
        {recentAudit.length === 0 ? (
          <p className="px-6 py-8 text-sm text-gray-400 text-center">No sequences generated yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-xs text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3 text-left">Sequence</th>
                <th className="px-6 py-3 text-left">Counter</th>
                <th className="px-6 py-3 text-left">Generated at</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {recentAudit.map((row) => (
                <tr key={row.id} className="hover:bg-gray-50">
                  <td className="px-6 py-3 font-mono font-medium text-brand-600">{row.fullSequence}</td>
                  <td className="px-6 py-3 text-gray-500">{row.counterValue}</td>
                  <td className="px-6 py-3 text-gray-500">{new Date(row.generatedAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="card px-6 py-5">
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">{label}</p>
      <p className="mt-1 text-3xl font-bold text-gray-900">{value}</p>
    </div>
  );
}
