import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

let activeToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  activeToken = token;
};

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (activeToken) {
      config.headers.Authorization = `Bearer ${activeToken}`;
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
      friendlyMessage = 'Authentication session expired or invalid. Please log in again.';
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
