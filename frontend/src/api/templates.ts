import { apiClient } from '../lib/api';
import type { TemplateResponse, CreateTemplateRequest, ApiResponse } from '../types';

export const fetchTemplates = async (): Promise<TemplateResponse[]> => {
  const res = await apiClient.get<ApiResponse<TemplateResponse[]>>('/templates');
  return res.data.data!;
};

export const fetchTemplate = async (id: string): Promise<TemplateResponse> => {
  const res = await apiClient.get<ApiResponse<TemplateResponse>>(`/templates/${id}`);
  return res.data.data!;
};

export const createTemplate = async (data: CreateTemplateRequest): Promise<TemplateResponse> => {
  const res = await apiClient.post<ApiResponse<TemplateResponse>>('/templates', data);
  return res.data.data!;
};

export const updateTemplate = async (id: string, data: { name?: string; description?: string }): Promise<TemplateResponse> => {
  const res = await apiClient.put<ApiResponse<TemplateResponse>>(`/templates/${id}`, data);
  return res.data.data!;
};

export const deleteTemplate = async (id: string): Promise<void> => {
  await apiClient.delete(`/templates/${id}`);
};
