import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { fetchApiKeys, createApiKey, revokeApiKey } from '../api/apikeys';
import { extractError } from '../lib/api';
import type { ApiKeyCreatedResponse } from '../types';

export default function ApiKeysPage() {
  const qc = useQueryClient();
  const [newKeyName, setNewKeyName] = useState('');
  const [createdKey, setCreatedKey] = useState<ApiKeyCreatedResponse | null>(null);

  const { data: keys = [], isLoading } = useQuery({ queryKey: ['apikeys'], queryFn: fetchApiKeys });

  const createMut = useMutation({
    mutationFn: () => createApiKey(newKeyName),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['apikeys'] });
      setCreatedKey(data);
      setNewKeyName('');
    },
    onError: (e) => toast.error(extractError(e)),
  });

  const revokeMut = useMutation({
    mutationFn: revokeApiKey,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['apikeys'] });
      toast.success('API key revoked');
    },
    onError: (e) => toast.error(extractError(e)),
  });

  return (
    <div className="p-8 space-y-6 max-w-3xl">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">API Keys</h2>
        <p className="text-sm text-gray-500 mt-1">Used for sequence generation requests (X-API-Key header)</p>
      </div>

      {/* Create */}
      <div className="card p-5">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Create new key</h3>
        <form
          onSubmit={(e) => { e.preventDefault(); createMut.mutate(); }}
          className="flex gap-3"
        >
          <input
            className="input flex-1"
            placeholder="Key name (e.g. Production)"
            value={newKeyName}
            onChange={(e) => setNewKeyName(e.target.value)}
            required
          />
          <button type="submit" className="btn-primary" disabled={createMut.isPending}>
            {createMut.isPending ? 'Creating…' : 'Create'}
          </button>
        </form>
      </div>

      {/* New key reveal modal */}
      {createdKey && (
        <div className="card p-5 border-green-200 bg-green-50">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-semibold text-green-800">Key created — copy it now</p>
              <p className="text-xs text-green-600 mt-0.5">This is shown only once and cannot be retrieved again.</p>
            </div>
            <button onClick={() => setCreatedKey(null)} className="text-green-600 hover:text-green-800 text-lg leading-none">×</button>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <code className="flex-1 text-xs bg-white border border-green-200 rounded px-3 py-2 font-mono break-all">
              {createdKey.plainKey}
            </code>
            <button
              onClick={() => { navigator.clipboard.writeText(createdKey.plainKey); toast.success('Copied!'); }}
              className="btn-secondary text-xs px-3 py-2 shrink-0"
            >
              Copy
            </button>
          </div>
        </div>
      )}

      {/* List */}
      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-sm text-gray-400">Loading…</div>
        ) : keys.length === 0 ? (
          <div className="p-8 text-center text-sm text-gray-400">No API keys yet.</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-xs text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-5 py-3 text-left">Name</th>
                <th className="px-5 py-3 text-left">Prefix</th>
                <th className="px-5 py-3 text-left">Last used</th>
                <th className="px-5 py-3 text-left">Created</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {keys.map((key) => (
                <tr key={key.id} className="hover:bg-gray-50">
                  <td className="px-5 py-3 font-medium">{key.name}</td>
                  <td className="px-5 py-3 font-mono text-gray-500">{key.keyPrefix}…</td>
                  <td className="px-5 py-3 text-gray-500">
                    {key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-5 py-3 text-gray-500">{new Date(key.createdAt).toLocaleDateString()}</td>
                  <td className="px-5 py-3 text-right">
                    <button
                      onClick={() => { if (confirm('Revoke this key?')) revokeMut.mutate(key.id); }}
                      className="text-xs text-red-500 hover:text-red-700 font-medium"
                    >
                      Revoke
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
