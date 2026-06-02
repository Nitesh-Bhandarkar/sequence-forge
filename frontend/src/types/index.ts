export type PlaceholderType = 'STATIC' | 'DATE' | 'COUNTER';

export type DateFormat =
  | 'FINANCIAL_YEAR' | 'FINANCIAL_YEAR_FULL' | 'FINANCIAL_QUARTER'
  | 'YEAR_4' | 'YEAR_2' | 'MONTH_2' | 'DAY_2'
  | 'QUARTER' | 'HALF_YEAR' | 'WEEK_OF_YEAR'
  | 'YYYYMM' | 'YYYYMMDD';

export interface PlaceholderConfigRequest {
  placeholderName: string;
  placeholderType: PlaceholderType;
  dateFormat?: string;
  description?: string;
  isRequired: boolean;
}

export interface PlaceholderConfigResponse {
  id: string;
  placeholderName: string;
  placeholderType: PlaceholderType;
  dateFormat?: string;
  description?: string;
  isRequired: boolean;
  sortOrder: number;
}

export interface TemplateResponse {
  id: string;
  name: string;
  description?: string;
  formatString: string;
  maxCounterValue: number;
  counterPadding: number;
  placeholders: PlaceholderConfigResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTemplateRequest {
  name: string;
  description?: string;
  formatString: string;
  maxCounterValue: number;
  placeholders: PlaceholderConfigRequest[];
}

export interface GenerateSequenceRequest {
  templateId: string;
  params: Record<string, string>;
}

export interface GenerateSequenceResponse {
  sequence: string;
  templateId: string;
  resolvedKey: string;
  counterValue: number;
  generatedAt: string;
}

export interface CounterStatusResponse {
  resolvedKey: string;
  currentValue: number;
  maxValue: number;
  remaining: number;
}

export interface ApiKeyResponse {
  id: string;
  name: string;
  keyPrefix: string;
  isActive: boolean;
  createdAt: string;
  lastUsedAt?: string;
}

export interface ApiKeyCreatedResponse {
  id: string;
  name: string;
  keyPrefix: string;
  plainKey: string;
  createdAt: string;
}

export interface AuditResponse {
  id: string;
  templateId: string;
  resolvedKey: string;
  counterValue: number;
  fullSequence: string;
  requestParams: string;
  generatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}
