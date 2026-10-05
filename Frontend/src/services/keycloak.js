import Keycloak from 'keycloak-js';

/**
 * Keycloak instance configuration for SIGA IPT.
 * Connects to the local Keycloak container on port 8080 and realm 'siga-ipt-realm'.
 */
const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'siga-ipt-realm',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'siga-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
