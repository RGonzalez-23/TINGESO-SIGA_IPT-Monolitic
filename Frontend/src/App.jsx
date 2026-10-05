
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import StudentListPage from './pages/StudentListPage';
import StudentRegisterPage from './pages/StudentRegisterPage';
import StudentDetailPage from './pages/StudentDetailPage';
import StudentProfilePage from './pages/StudentProfilePage';

// Protected Route Component based on User Role
const ProtectedRoute = ({ allowedRoles, children }) => {
  const { currentRole } = useAuth();
  if (!allowedRoles.includes(currentRole)) {
    return <Navigate to="/" replace />;
  }
  return children;
};

// Root Redirect based on active Role
const HomeRedirect = () => {
  const { isStudent } = useAuth();
  return isStudent ? <Navigate to="/profile" replace /> : <Navigate to="/students" replace />;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="d-flex flex-column min-vh-100">
          <Navbar />
          <main className="flex-grow-1">
            <Routes>
              <Route path="/" element={<HomeRedirect />} />

              {/* Student Management: accessible to ADMIN and TEACHER */}
              <Route
                path="/students"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'TEACHER']}>
                    <StudentListPage />
                  </ProtectedRoute>
                }
              />

              {/* Student Registration: only accessible to ADMIN */}
              <Route
                path="/students/new"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN']}>
                    <StudentRegisterPage />
                  </ProtectedRoute>
                }
              />

              {/* Student Detail: accessible to ADMIN and TEACHER */}
              <Route
                path="/students/:run"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'TEACHER']}>
                    <StudentDetailPage />
                  </ProtectedRoute>
                }
              />

              {/* Personal Student Profile: accessible to STUDENT */}
              <Route
                path="/profile"
                element={
                  <ProtectedRoute allowedRoles={['STUDENT']}>
                    <StudentProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>

          <footer className="bg-white border-top py-3 text-center text-muted small mt-auto">
            <div className="container">
              <span>SIGA IPT &bull; Sistema Integrado de Gestión Académica &bull; USACH 2026</span>
            </div>
          </footer>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
