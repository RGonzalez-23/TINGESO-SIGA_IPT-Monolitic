import { createContext, useContext, useState } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  // During local development, allow switching roles to test all Epic 1 permissions:
  // ADMIN (Academic Administrator), TEACHER (Professor), STUDENT (Student)
  const [currentRole, setCurrentRole] = useState(localStorage.getItem('user_role') || 'ADMIN');
  const [currentUserRun, setCurrentUserRun] = useState(localStorage.getItem('user_run') || '22222222-2');

  const switchRole = (newRole, newRun = '22222222-2') => {
    setCurrentRole(newRole);
    setCurrentUserRun(newRun);
    localStorage.setItem('user_role', newRole);
    localStorage.setItem('user_run', newRun);
  };

  const value = {
    currentRole,
    currentUserRun,
    switchRole,
    isAdmin: currentRole === 'ADMIN',
    isTeacher: currentRole === 'TEACHER',
    isStudent: currentRole === 'STUDENT',
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);
