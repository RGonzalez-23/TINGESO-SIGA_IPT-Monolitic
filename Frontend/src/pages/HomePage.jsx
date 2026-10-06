import { useAuth } from '../context/AuthContext';
import WelcomePage from './WelcomePage';
import HomeAdmin from './HomeAdmin';
import HomeTeacher from './HomeTeacher';
import HomeStudent from './HomeStudent';

/**
 * HomePage acts as a role-based dispatcher.
 * Unauthenticated visitors see the WelcomePage.
 * Authenticated users are presented with their role-specific dashboard.
 */
const HomePage = () => {
  const { authenticated, isAdmin, isTeacher, isStudent } = useAuth();

  if (!authenticated) {
    return <WelcomePage />;
  }

  if (isAdmin) {
    return <HomeAdmin />;
  }

  if (isTeacher) {
    return <HomeTeacher />;
  }

  if (isStudent) {
    return <HomeStudent />;
  }

  // Fallback for authenticated users without specific role
  return <HomeStudent />;
};

export default HomePage;
