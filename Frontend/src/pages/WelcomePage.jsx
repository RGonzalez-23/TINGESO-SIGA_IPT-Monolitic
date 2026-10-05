import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';

const WelcomePage = () => {
  const { authenticated, isStudent, login } = useAuth();

  if (authenticated) {
    return <Navigate to={isStudent ? '/profile' : '/students'} replace />;
  }

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8 text-center mb-5">
          <div className="d-inline-flex p-3 rounded-circle bg-primary bg-opacity-10 text-primary mb-3">
            <i className="bi bi-mortarboard-fill fs-1" style={{ color: '#2947c0' }}></i>
          </div>
          <h1 className="display-5 fw-bold text-dark mb-2">Sistema Integrado de Gestión Académica</h1>
          <p className="lead text-muted">Instituto Profesional de Tecnología (IPT) &bull; USACH</p>
          <div className="d-flex justify-content-center gap-3 mt-4">
            <button
              onClick={login}
              className="btn btn-primary btn-lg px-4 py-2 fw-semibold shadow-sm d-flex align-items-center gap-2"
              style={{ backgroundColor: '#2947c0', borderColor: '#2947c0' }}
            >
              <i className="bi bi-box-arrow-in-right fs-5"></i>
              Ingresar con SSO Keycloak
            </button>
          </div>
        </div>

        <div className="col-12 col-lg-10">
          <div className="card shadow-sm border-0 rounded-4 overflow-hidden">
            <div className="card-header bg-light py-3 border-0">
              <h5 className="mb-0 fw-bold text-dark d-flex align-items-center">
                <i className="bi bi-shield-check text-primary me-2"></i>
                Cuentas de Prueba Preconfiguradas (Keycloak Realm)
              </h5>
            </div>
            <div className="card-body p-4">
              <p className="text-muted small mb-4">
                Puedes iniciar sesión en Keycloak utilizando cualquiera de las siguientes cuentas de prueba creadas
                en el entorno de desarrollo:
              </p>
              <div className="row g-3">
                {/* Admin Card */}
                <div className="col-md-4">
                  <div className="p-3 border rounded-3 h-100 bg-white shadow-sm border-start border-4 border-warning">
                    <span className="badge bg-warning text-dark mb-2 fw-bold">ADMINISTRADOR</span>
                    <h6 className="fw-bold mb-1">Administrador Académico</h6>
                    <div className="text-muted small mt-2">
                      <div><strong>Usuario:</strong> <code>admin</code></div>
                      <div><strong>Contraseña:</strong> <code>admin123</code></div>
                      <div className="mt-2 text-secondary">
                        <i className="bi bi-check2-circle text-success me-1"></i>
                        Gestión total de estudiantes, carreras y notas.
                      </div>
                    </div>
                  </div>
                </div>

                {/* Teacher Card */}
                <div className="col-md-4">
                  <div className="p-3 border rounded-3 h-100 bg-white shadow-sm border-start border-4 border-info">
                    <span className="badge bg-info text-dark mb-2 fw-bold">DOCENTE</span>
                    <h6 className="fw-bold mb-1">Profesor de Asignatura</h6>
                    <div className="text-muted small mt-2">
                      <div><strong>Usuario:</strong> <code>11111111-1</code></div>
                      <div><strong>Contraseña:</strong> <code>docente123</code></div>
                      <div className="mt-2 text-secondary">
                        <i className="bi bi-check2-circle text-success me-1"></i>
                        Consulta de alumnos y registro de notas.
                      </div>
                    </div>
                  </div>
                </div>

                {/* Student Card */}
                <div className="col-md-4">
                  <div className="p-3 border rounded-3 h-100 bg-white shadow-sm border-start border-4 border-success">
                    <span className="badge bg-success mb-2 fw-bold">ESTUDIANTE</span>
                    <h6 className="fw-bold mb-1">Ana Rojas (Estudiante)</h6>
                    <div className="text-muted small mt-2">
                      <div><strong>Usuario:</strong> <code>22222222-2</code></div>
                      <div><strong>Contraseña:</strong> <code>alumno123</code></div>
                      <div className="mt-2 text-secondary">
                        <i className="bi bi-check2-circle text-success me-1"></i>
                        Consulta de perfil académico e inscripciones.
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WelcomePage;
