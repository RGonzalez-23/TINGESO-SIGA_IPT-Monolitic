import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomeStudent = () => {
  const { user } = useAuth();

  return (
    <div className="container py-4">
      {/* Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #065f46 0%, #059669 60%, #10b981 100%)',
        }}
      >
        <div style={{ maxWidth: '700px' }}>
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-white small fw-semibold">
            <i className="bi bi-mortarboard"></i>
            <span>Portal del Estudiante IPT</span>
          </div>
          <h1 className="display-6 fw-bold mb-2">
            ¡Hola, {user?.name || 'Estudiante'}!
          </h1>
          <p className="lead mb-0 text-white-50 fs-6">
            Bienvenido/a a tu portal de autoservicio académico en el Instituto Profesional de Tecnología (USACH).
          </p>
        </div>
      </div>

      {/* Action Cards */}
      <div className="row g-4 mb-4">
        <div className="col-12 col-md-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-success"
                  style={{ backgroundColor: '#ecfdf5', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-person-badge fs-3"></i>
                </div>
                <span className="badge bg-success bg-opacity-10 text-success px-3 py-2 rounded-pill fw-semibold">
                  Mi Perfil
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Ficha Académica y Credenciales</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Consulta tu plan de estudio asignado, avance curricular, datos personales y actualiza tu contraseña de acceso a los sistemas institucionales.
              </p>
              <div className="pt-2 border-top">
                <Link to="/profile" className="btn btn-success text-white fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-arrow-right-circle me-2"></i>
                  Ir a Mi Perfil Académico
                </Link>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-secondary"
                  style={{ backgroundColor: '#f8fafc', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-journal-check fs-3 text-secondary"></i>
                </div>
                <span className="badge bg-secondary bg-opacity-10 text-secondary px-3 py-2 rounded-pill fw-semibold">
                  Planes Curriculares
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Oferta Formativa IPT</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Conoce las carreras técnicas de nivel superior impartidas por el Instituto Profesional de Tecnología y su estructura de 4 semestres.
              </p>
              <div className="pt-2 border-top">
                <Link to="/careers" className="btn btn-outline-secondary fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-journal-text me-2"></i>
                  Ver Carreras
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomeStudent;
