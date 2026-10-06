import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomeTeacher = () => {
  const { user } = useAuth();

  return (
    <div className="container py-4">
      {/* Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #0e7490 0%, #0891b2 60%, #06b6d4 100%)',
        }}
      >
        <div style={{ maxWidth: '700px' }}>
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-white small fw-semibold">
            <i className="bi bi-person-workspace"></i>
            <span>Portal Docente IPT</span>
          </div>
          <h1 className="display-6 fw-bold mb-2">
            Bienvenido/a, Profesor/a {user?.name || ''}
          </h1>
          <p className="lead mb-0 text-white-50 fs-6">
            Plataforma de seguimiento académico del Instituto Profesional de Tecnología. Consulta las nóminas de
            estudiantes inscritos y programas académicos.
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
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-info"
                  style={{ backgroundColor: '#ecfeff', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-people fs-3"></i>
                </div>
                <span className="badge bg-info bg-opacity-10 text-info px-3 py-2 rounded-pill fw-semibold">
                  Alumnos
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Consulta de Estudiantes</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Visualiza el listado de alumnos por carrera técnica, verifica su estado académico y accede a fichas de consulta.
              </p>
              <div className="pt-2 border-top">
                <Link to="/students" className="btn btn-info text-white fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-search me-2"></i>
                  Consultar Estudiantes
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
                  style={{ backgroundColor: '#f1f5f9', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-journal-text fs-3 text-secondary"></i>
                </div>
                <span className="badge bg-secondary bg-opacity-10 text-secondary px-3 py-2 rounded-pill fw-semibold">
                  Carreras
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Planes Curriculares</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Consulta los planes de estudio vigentes y la oferta formativa técnica de 2 años del instituto.
              </p>
              <div className="pt-2 border-top">
                <Link to="/careers" className="btn btn-outline-secondary fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-eye me-2"></i>
                  Ver Carreras y Planes
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomeTeacher;
