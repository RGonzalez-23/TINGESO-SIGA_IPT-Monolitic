import { createContext, useContext, useState, useEffect, useRef } from 'react';
import keycloak from '../services/keycloak';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const isRun = useRef(false);

  useEffect(() => {
    if (isRun.current) return;
    isRun.current = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        pkceMethod: 'S256',
      })
      .then((auth) => {
        setAuthenticated(!!auth);
        if (auth) {
          const parsed = keycloak.tokenParsed || {};
          const realmRoles = parsed.realm_access?.roles || [];
          setUser({
            run: parsed.preferred_username || '',
            name: parsed.name || parsed.preferred_username || 'Usuario',
            email: parsed.email || '',
            roles: realmRoles,
          });
        }
        setInitialized(true);
      })
      .catch((err) => {
        console.error('Failed to initialize Keycloak:', err);
        setInitialized(true);
      });
  }, []);

  const login = () => keycloak.login();
  const logout = () => keycloak.logout({ redirectUri: window.location.origin });

  const roles = user?.roles || [];
  const isAdmin = roles.includes('ADMIN');
  const isTeacher = roles.includes('TEACHER');
  const isStudent = roles.includes('STUDENT');

  const currentRole = isAdmin ? 'ADMIN' : isTeacher ? 'TEACHER' : isStudent ? 'STUDENT' : null;
  const currentUserRun = user?.run || '';

  const value = {
    initialized,
    authenticated,
    user,
    roles,
    currentRole,
    currentUserRun,
    isAdmin,
    isTeacher,
    isStudent,
    login,
    logout,
  };

  if (!initialized) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 bg-light">
        <div className="text-center">
          <div className="spinner-border text-primary" role="status" style={{ width: '3rem', height: '3rem' }}>
            <span className="visually-hidden">Cargando...</span>
          </div>
          <p className="mt-3 text-muted fw-bold">Iniciando sesión segura con Keycloak...</p>
        </div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext);
