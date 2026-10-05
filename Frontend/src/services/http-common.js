import axios from 'axios';

/**
 * Centralized Axios instance for SIGA IPT.
 * Configured with base URL pointing to the Spring Boot backend on port 8081.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Request Interceptor:
 * Attaches the Keycloak Bearer token to each outgoing request when available.
 */
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Response Interceptor:
 * Captures authentication errors (401/403) and formats error responses.
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn('Unauthorized request. Token may be expired or missing.');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
