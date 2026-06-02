import { apiClient } from '../lib/api';
import type { ApiKeyResponse, ApiKeyCreatedResponse, ApiResponse } from '../types';

export const fetchApiKeys = async (): Promise<ApiKeyResponse[]> => {
  const res = await apiClient.get<ApiResponse<ApiKeyResponse[]>>('/apikeys');
  return res.data.data!;
};

export const createApiKey = async (name: string): Promise<ApiKeyCreatedResponse> => {
  const res = await apiClient.post<ApiResponse<ApiKeyCreatedResponse>>('/apikeys', { name });
  return res.data.data!;
};

export const revokeApiKey = async (id: string): Promise<void> => {
  await apiClient.delete(`/apikeys/${id}`);
};
