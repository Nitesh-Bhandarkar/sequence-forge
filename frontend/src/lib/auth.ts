const TOKEN_KEY = 'sf_token';
const TENANT_KEY = 'sf_tenant';

export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY);
export const getTenantId = (): string | null => localStorage.getItem(TENANT_KEY);

export const storeAuth = (token: string, tenantId: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(TENANT_KEY, tenantId);
};

export const clearAuth = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TENANT_KEY);
};

export const isAuthenticated = (): boolean => !!getToken();
