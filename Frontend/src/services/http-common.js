import axios from 'axios';
import keycloak from './keycloak';

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
 * Attaches the active Keycloak Bearer token to each outgoing request.
 * Automatically refreshes the token if it expires in less than 30 seconds.
 */
apiClient.interceptors.request.use(
  async (config) => {
    if (keycloak && keycloak.authenticated && keycloak.token) {
      try {
        await keycloak.updateToken(30);
      } catch (err) {
        console.warn('Keycloak token refresh failed:', err);
      }
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Response Interceptor:
 * Captures authentication errors (401/403) and logs warnings.
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn('Unauthorized request (401). Token may be expired or invalid.');
    } else if (error.response && error.response.status === 403) {
      console.warn('Forbidden request (403). User lacks required role permissions.');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
