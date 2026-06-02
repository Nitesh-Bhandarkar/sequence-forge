import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { createTemplate } from '../api/templates';
import { classifyPlaceholder } from '../api/ai';
import { extractError } from '../lib/api';
import type { PlaceholderType, PlaceholderConfigRequest } from '../types';
import AIChatPanel from '../components/AIChatPanel';

const DATE_FORMATS = [
  'FINANCIAL_YEAR', 'FINANCIAL_YEAR_FULL', 'FINANCIAL_QUARTER',
  'YEAR_4', 'YEAR_2', 'MONTH_2', 'DAY_2',
  'QUARTER', 'HALF_YEAR', 'WEEK_OF_YEAR',
  'YYYYMM', 'YYYYMMDD',
];

const DATE_FORMAT_LABELS: Record<string, string> = {
  FINANCIAL_YEAR: 'Financial Year (2627)',
  FINANCIAL_YEAR_FULL: 'Financial Year Full (2026-27)',
  FINANCIAL_QUARTER: 'Financial Quarter (FQ1–FQ4)',
  YEAR_4: 'Year 4-digit (2026)',
  YEAR_2: 'Year 2-digit (26)',
  MONTH_2: 'Month (06)',
  DAY_2: 'Day (01)',
  QUARTER: 'Calendar Quarter (Q1–Q4)',
  HALF_YEAR: 'Half Year (H1/H2)',
  WEEK_OF_YEAR: 'ISO Week (01–53)',
  YYYYMM: 'YYYYMM (202606)',
  YYYYMMDD: 'YYYYMMDD (20260601)',
};

function extractNames(fmt: string): string[] {
  return [...fmt.matchAll(/\{([^}]+)}/g)].map((m) => m[1]);
}

interface PlaceholderRow extends PlaceholderConfigRequest {
  placeholderName: string;
}

export default function TemplateBuilderPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [formatString, setFormatString] = useState('');
  const [maxCounterValue, setMaxCounterValue] = useState(9999);
  const [placeholders, setPlaceholders] = useState<PlaceholderRow[]>([]);

  // Auto-detect placeholders from format string
  useEffect(() => {
    const names = extractNames(formatString);
    setPlaceholders((prev) =>
      names.map((n) => prev.find((p) => p.placeholderName === n) ?? {
        placeholderName: n,
        placeholderType: 'STATIC' as PlaceholderType,
        isRequired: true,
      }),
    );
  }, [formatString]);

  const updatePlaceholder = (index: number, patch: Partial<PlaceholderRow>) => {
    setPlaceholders((prev) => prev.map((p, i) => (i === index ? { ...p, ...patch } : p)));
  };

  const counterCount = placeholders.filter((p) => p.placeholderType === 'COUNTER').length;
  const preview = buildPreview(formatString, placeholders, maxCounterValue);

  const [classifying, setClassifying] = useState<string | null>(null);

  const handleAiClassify = async (index: number) => {
    const ph = placeholders[index];
    setClassifying(ph.placeholderName);
    try {
      const result = await classifyPlaceholder(ph.placeholderName, name || formatString);
      updatePlaceholder(index, {
        placeholderType: result.placeholderType as PlaceholderType,
        dateFormat: result.dateFormat ?? undefined,
        description: result.description,
      });
      toast.success(`Classified {${ph.placeholderName}} as ${result.placeholderType}`);
    } catch {
      toast.error('AI classification failed');
    } finally {
      setClassifying(null);
    }
  };

  const mutation = useMutation({
    mutationFn: () =>
      createTemplate({ name, description, formatString, maxCounterValue, placeholders }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['templates'] });
      toast.success('Template created');
      navigate('/templates');
    },
    onError: (e) => toast.error(extractError(e)),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (counterCount !== 1) {
      toast.error('Template must have exactly one COUNTER placeholder');
      return;
    }
    mutation.mutate();
  };

  return (
    <div className="p-8 flex gap-6 h-full min-h-0">
    <div className="flex-1 max-w-2xl">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900">New Template</h2>
        <p className="text-sm text-gray-500 mt-1">
          Define a format string using <code className="bg-gray-100 px-1 rounded">{'{PLACEHOLDER}'}</code> syntax
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Basic info */}
        <div className="card p-6 space-y-4">
          <h3 className="text-sm font-semibold text-gray-700">Template details</h3>
          <div>
            <label className="label">Name *</label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} required placeholder="Invoice Number" />
          </div>
          <div>
            <label className="label">Description</label>
            <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional description" />
          </div>
          <div>
            <label className="label">Format string *</label>
            <input
              className="input font-mono"
              value={formatString}
              onChange={(e) => setFormatString(e.target.value)}
              required
              placeholder="{SS}/{CC}/{FY}/{SEQ}"
            />
            <p className="text-xs text-gray-400 mt-1">
              Use <code>{'{NAME}'}</code> for placeholders. Exactly one must be type COUNTER.
            </p>
          </div>
          <div className="w-40">
            <label className="label">Max counter value *</label>
            <input
              className="input"
              type="number"
              min={1}
              value={maxCounterValue}
              onChange={(e) => setMaxCounterValue(Number(e.target.value))}
              required
            />
            <p className="text-xs text-gray-400 mt-1">
              Padding: {String(maxCounterValue).length} digits
            </p>
          </div>
        </div>

        {/* Placeholder configuration */}
        {placeholders.length > 0 && (
          <div className="card p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-gray-700">Placeholder configuration</h3>
              {counterCount !== 1 && (
                <span className="text-xs text-red-500">
                  {counterCount === 0 ? 'No COUNTER placeholder set' : 'Too many COUNTER placeholders'}
                </span>
              )}
            </div>
            {placeholders.map((p, i) => (
              <PlaceholderEditor
                key={p.placeholderName}
                row={p}
                onChange={(patch) => updatePlaceholder(i, patch)}
                onAiClassify={() => handleAiClassify(i)}
                classifying={classifying === p.placeholderName}
              />
            ))}
          </div>
        )}

        {/* Preview */}
        {preview && (
          <div className="card p-4 bg-brand-50 border-brand-100">
            <p className="text-xs font-medium text-brand-700 mb-1">Preview</p>
            <code className="text-sm text-brand-800 font-mono">{preview}</code>
          </div>
        )}

        <div className="flex gap-3">
          <button type="submit" className="btn-primary" disabled={mutation.isPending}>
            {mutation.isPending ? 'Creating…' : 'Create Template'}
          </button>
          <button type="button" className="btn-secondary" onClick={() => navigate('/templates')}>
            Cancel
          </button>
        </div>
      </form>
    </div>

    {/* AI Chat Panel */}
    <div className="w-80 shrink-0" style={{ height: 'calc(100vh - 4rem)' }}>
      <AIChatPanel templateContext={formatString ? `${name} — ${formatString}` : undefined} />
    </div>
    </div>
  );
}

function PlaceholderEditor({ row, onChange, onAiClassify, classifying }: {
  row: PlaceholderRow;
  onChange: (patch: Partial<PlaceholderRow>) => void;
  onAiClassify: () => void;
  classifying: boolean;
}) {
  return (
    <div className="border border-gray-200 rounded-lg p-4 space-y-3">
      <div className="flex items-center gap-2">
        <code className="text-sm font-semibold text-brand-600 bg-brand-50 px-2 py-0.5 rounded">
          {'{' + row.placeholderName + '}'}
        </code>
        <button
          type="button"
          onClick={onAiClassify}
          disabled={classifying}
          className="text-xs bg-violet-50 text-violet-600 hover:bg-violet-100 px-2 py-0.5 rounded transition-colors disabled:opacity-50"
        >
          {classifying ? '⏳ Classifying…' : '✨ AI classify'}
        </button>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Type</label>
          <select
            className="input"
            value={row.placeholderType}
            onChange={(e) => onChange({ placeholderType: e.target.value as PlaceholderType, dateFormat: undefined })}
          >
            <option value="STATIC">STATIC — caller provides value</option>
            <option value="DATE">DATE — date-based</option>
            <option value="COUNTER">COUNTER — auto-increment</option>
          </select>
        </div>
        {row.placeholderType === 'DATE' && (
          <div>
            <label className="label">Date format *</label>
            <select
              className="input"
              value={row.dateFormat ?? ''}
              onChange={(e) => onChange({ dateFormat: e.target.value })}
              required
            >
              <option value="">Select format…</option>
              {DATE_FORMATS.map((f) => (
                <option key={f} value={f}>{DATE_FORMAT_LABELS[f]}</option>
              ))}
            </select>
          </div>
        )}
      </div>
      <div>
        <label className="label">Description</label>
        <input
          className="input text-sm"
          value={row.description ?? ''}
          onChange={(e) => onChange({ description: e.target.value })}
          placeholder="e.g. State code (MH, KA…)"
        />
      </div>
      <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
        <input
          type="checkbox"
          checked={row.isRequired}
          onChange={(e) => onChange({ isRequired: e.target.checked })}
          className="rounded border-gray-300 text-brand-600"
        />
        Required
      </label>
    </div>
  );
}

function buildPreview(format: string, placeholders: PlaceholderRow[], maxCounter: number): string {
  if (!format) return '';
  const padding = String(maxCounter).length;
  let result = format;
  for (const p of placeholders) {
    const sample = p.placeholderType === 'COUNTER'
      ? '1'.padStart(padding, '0')
      : p.placeholderType === 'DATE'
        ? p.dateFormat?.split('_')[0] ?? 'DATE'
        : p.placeholderName.toLowerCase();
    result = result.replace(new RegExp(`\\{${p.placeholderName}\\}`, 'g'), sample);
  }
  return result;
}
