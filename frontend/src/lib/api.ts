import axios from 'axios';
import { getToken, clearAuth } from './auth';

export const apiClient = axios.create({ baseURL: '/api/v1' });

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export const extractError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.error ?? error.message;
  }
  return 'An unexpected error occurred';
};
