import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import CareerManagementPage from './pages/CareerManagementPage';
import StudentListPage from './pages/StudentListPage';
import StudentRegisterPage from './pages/StudentRegisterPage';
import StudentDetailPage from './pages/StudentDetailPage';
import StudentProfilePage from './pages/StudentProfilePage';
import StudyPlanDetailPage from './pages/StudyPlanDetailPage';

// Protected Route Component based on User Authentication and Role
const ProtectedRoute = ({ allowedRoles, children }) => {
  const { authenticated, roles, login } = useAuth();

  if (!authenticated) {
    return (
      <div className="container py-5 text-center">
        <div className="card shadow-sm border-0 p-5 mx-auto" style={{ maxWidth: '500px' }}>
          <i className="bi bi-shield-lock-fill text-warning display-4 mb-3"></i>
          <h4 className="fw-bold mb-2">Autenticación Requerida</h4>
          <p className="text-muted small mb-4">
            Debes iniciar sesión con tu cuenta institucional en Keycloak para acceder a este módulo.
          </p>
          <button onClick={login} className="btn btn-primary fw-bold" style={{ backgroundColor: '#2947c0' }}>
            <i className="bi bi-box-arrow-in-right me-2"></i>
            Iniciar Sesión con Keycloak
          </button>
        </div>
      </div>
    );
  }

  const hasAccess = allowedRoles.some((role) => roles.includes(role));
  if (!hasAccess) {
    return (
      <div className="container py-5 text-center">
        <div className="card shadow-sm border-0 p-5 mx-auto" style={{ maxWidth: '500px' }}>
          <i className="bi bi-slash-circle-fill text-danger display-4 mb-3"></i>
          <h4 className="fw-bold mb-2 text-danger">Acceso Denegado</h4>
          <p className="text-muted small mb-3">
            Tu cuenta no posee los privilegios requeridos para acceder a este módulo.
          </p>
          <Navigate to="/" replace />
        </div>
      </div>
    );
  }

  return children;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="d-flex flex-column min-vh-100">
          <Navbar />
          <main className="flex-grow-1">
            <Routes>
              {/* Home dispatcher according to user role (Option B) */}
              <Route path="/" element={<HomePage />} />

              {/* Career and Study Plan Management: only accessible to ADMIN and TEACHER */}
              <Route
                path="/careers"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'TEACHER']}>
                    <CareerManagementPage />
                  </ProtectedRoute>
                }
              />

              {/* Individual Study Plan & Curriculum View: accessible to ADMIN, TEACHER and STUDENT */}
              <Route
                path="/study-plans/:id"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'TEACHER', 'STUDENT']}>
                    <StudyPlanDetailPage />
                  </ProtectedRoute>
                }
              />

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
