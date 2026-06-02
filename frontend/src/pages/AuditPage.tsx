import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchAuditLog } from '../api/sequences';
import { fetchTemplates } from '../api/templates';
import type { AuditResponse, Page } from '../types';

export default function AuditPage() {
  const [templateId, setTemplateId] = useState('');
  const [page, setPage] = useState(0);

  const { data: templates = [] } = useQuery({ queryKey: ['templates'], queryFn: fetchTemplates });
  const { data, isLoading } = useQuery<Page<AuditResponse>>({
    queryKey: ['audit', templateId, page],
    queryFn: () => fetchAuditLog({ templateId: templateId || undefined, page, size: 20 }),
  });

  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="p-8 space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Audit Log</h2>
        <p className="text-sm text-gray-500 mt-1">Immutable record of every generated sequence</p>
      </div>

      {/* Filters */}
      <div className="flex gap-3 items-center">
        <select
          className="input w-60"
          value={templateId}
          onChange={(e) => { setTemplateId(e.target.value); setPage(0); }}
        >
          <option value="">All templates</option>
          {templates.map((t) => (
            <option key={t.id} value={t.id}>{t.name}</option>
          ))}
        </select>
        {templateId && (
          <button className="text-sm text-gray-400 hover:text-gray-600" onClick={() => setTemplateId('')}>
            Clear filter
          </button>
        )}
      </div>

      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-sm text-gray-400">Loading…</div>
        ) : rows.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-400">No audit records found.</div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-xs text-gray-500 uppercase tracking-wider">
                <tr>
                  <th className="px-5 py-3 text-left">Sequence</th>
                  <th className="px-5 py-3 text-left">Counter</th>
                  <th className="px-5 py-3 text-left">Redis key</th>
                  <th className="px-5 py-3 text-left">Generated at</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-gray-50">
                    <td className="px-5 py-3 font-mono font-semibold text-brand-600">{row.fullSequence}</td>
                    <td className="px-5 py-3 text-gray-500">{row.counterValue}</td>
                    <td className="px-5 py-3 text-gray-400 text-xs truncate max-w-xs" title={row.resolvedKey}>
                      {row.resolvedKey}
                    </td>
                    <td className="px-5 py-3 text-gray-500 whitespace-nowrap">
                      {new Date(row.generatedAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {totalPages > 1 && (
              <div className="px-5 py-3 border-t border-gray-100 flex items-center gap-3 text-sm">
                <button
                  className="btn-secondary px-3 py-1.5 text-xs"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  ← Prev
                </button>
                <span className="text-gray-500">Page {page + 1} of {totalPages}</span>
                <button
                  className="btn-secondary px-3 py-1.5 text-xs"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
