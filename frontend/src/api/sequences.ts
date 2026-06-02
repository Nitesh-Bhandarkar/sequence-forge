import { apiClient } from '../lib/api';
import type { GenerateSequenceResponse, CounterStatusResponse, ApiResponse } from '../types';
import axios from 'axios';

// Sequence generation uses X-API-Key, not Bearer JWT
export const generateSequence = async (
  apiKey: string,
  templateId: string,
  params: Record<string, string>,
): Promise<GenerateSequenceResponse> => {
  const res = await axios.post<ApiResponse<GenerateSequenceResponse>>(
    '/api/v1/sequences/generate',
    { templateId, params },
    { headers: { 'X-API-Key': apiKey } },
  );
  return res.data.data!;
};

export const peekCounter = async (
  apiKey: string,
  templateId: string,
  params: Record<string, string> = {},
): Promise<CounterStatusResponse> => {
  const res = await axios.post<ApiResponse<CounterStatusResponse>>(
    `/api/v1/sequences/counter?templateId=${templateId}`,
    params,
    { headers: { 'X-API-Key': apiKey } },
  );
  return res.data.data!;
};

export const fetchAuditLog = async (params?: {
  templateId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}) => {
  const res = await apiClient.get('/audit', { params });
  return res.data.data!;
};
