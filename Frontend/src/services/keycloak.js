import Keycloak from 'keycloak-js';

/**
 * Keycloak instance configuration for SIGA IPT.
 * Connects to Keycloak on port 8080 and realm 'siga-ipt-realm'.
 * Dynamically resolves the host when accessed from a remote IP or domain (e.g., AWS EC2).
 */
const getKeycloakUrl = () => {
  if (typeof window !== 'undefined' && window.location.hostname && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }
  return import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080';
};

const keycloakConfig = {
  url: getKeycloakUrl(),
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'siga-ipt-realm',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'siga-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
