import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

let activeToken: string | null = null;
let tokenSupplier: (() => Promise<string | null>) | null = null;

export const setAuthToken = (token: string | null) => {
  activeToken = token;
};

export const setTokenSupplier = (supplier: (() => Promise<string | null>) | null) => {
  tokenSupplier = supplier;
};

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    let token = activeToken;
    if (tokenSupplier) {
      try {
        const freshToken = await tokenSupplier();
        if (freshToken) {
          token = freshToken;
          activeToken = freshToken;
        }
      } catch {
        // Fall back to activeToken if refresh fails
      }
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; validationErrors?: Record<string, string> }>) => {
    const status = error.response?.status;
    const errorData = error.response?.data;

    let friendlyMessage = 'An unexpected error occurred';
    if (errorData?.message) {
      friendlyMessage = errorData.message;
    } else if (status === 401) {
      friendlyMessage = 'Authentication session expired or invalid. Please sign in again.';
    } else if (status === 403) {
      friendlyMessage = 'You do not have permission to access or modify this resource.';
    } else if (status === 404) {
      friendlyMessage = 'The requested resource was not found.';
    } else if (!error.response) {
      friendlyMessage = 'Unable to connect to the backend server. Please verify backend availability.';
    }

    return Promise.reject({
      status,
      message: friendlyMessage,
      validationErrors: errorData?.validationErrors,
      raw: error,
    });
  }
);
