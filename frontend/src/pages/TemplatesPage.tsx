import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { fetchTemplates, deleteTemplate } from '../api/templates';
import { extractError } from '../lib/api';

export default function TemplatesPage() {
  const qc = useQueryClient();
  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['templates'],
    queryFn: fetchTemplates,
  });

  const deleteMut = useMutation({
    mutationFn: deleteTemplate,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['templates'] });
      toast.success('Template deleted');
    },
    onError: (e) => toast.error(extractError(e)),
  });

  if (isLoading) return <LoadingState />;

  return (
    <div className="p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Templates</h2>
          <p className="text-sm text-gray-500 mt-1">{templates.length} template{templates.length !== 1 ? 's' : ''}</p>
        </div>
        <Link to="/templates/new" className="btn-primary">+ New Template</Link>
      </div>

      {templates.length === 0 ? (
        <div className="card p-12 text-center">
          <p className="text-gray-400 text-sm">No templates yet.</p>
          <Link to="/templates/new" className="btn-primary mt-4 inline-flex">Create your first template</Link>
        </div>
      ) : (
        <div className="space-y-3">
          {templates.map((t) => (
            <div key={t.id} className="card px-6 py-5 flex items-start justify-between gap-4">
              <div className="space-y-2 min-w-0">
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold text-gray-900">{t.name}</h3>
                  <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                    {t.placeholders.length} placeholders
                  </span>
                </div>
                <code className="text-sm text-brand-600 bg-brand-50 px-2 py-1 rounded">
                  {t.formatString}
                </code>
                {t.description && <p className="text-xs text-gray-500">{t.description}</p>}
                <p className="text-xs text-gray-400">
                  Max counter: {t.maxCounterValue.toLocaleString()} · Padding: {t.counterPadding} digits
                </p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => {
                    if (confirm(`Delete template "${t.name}"?`)) deleteMut.mutate(t.id);
                  }}
                  className="btn-danger text-xs px-3 py-1.5"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function LoadingState() {
  return (
    <div className="p-8">
      <div className="animate-pulse space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="card h-24 bg-gray-100" />
        ))}
      </div>
    </div>
  );
}
