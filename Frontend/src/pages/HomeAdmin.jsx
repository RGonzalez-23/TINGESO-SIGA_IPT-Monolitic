import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomeAdmin = () => {
  const { user } = useAuth();

  return (
    <div className="container py-4">
      {/* Welcome Banner */}
      <div className="p-4 p-md-5 mb-4 rounded-4 bg-primary text-white shadow-sm position-relative overflow-hidden"
        style={{
          background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%)',
        }}
      >
        <div className="position-relative z-1" style={{ maxWidth: '750px' }}>
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 backdrop-blur mb-3 text-white small fw-semibold">
            <i className="bi bi-shield-check"></i>
            <span>Panel de Administración Institucional</span>
          </div>
          <h1 className="display-6 fw-bold mb-2">
            Bienvenido/a, {user?.name || 'Administrador'}
          </h1>
          <p className="lead mb-0 text-white-50 fs-6">
            Gestión centralizada del Instituto Profesional de Tecnología (IPT) de la Universidad de Santiago de Chile.
            Administra carreras técnicas de 2 años, mallas curriculares, asignaturas con horas TEL/créditos SCT y estudiantes.
          </p>
        </div>
      </div>

      {/* Control Panel Cards */}
      <div className="row g-4 mb-4">
        {/* Card 1: Student Management */}
        <div className="col-12 col-lg-4">
          <div className="card h-100 border-0 shadow-sm rounded-4 transition-hover overflow-hidden">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-primary"
                  style={{ backgroundColor: '#eff6ff', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-people-fill fs-3"></i>
                </div>
                <span className="badge bg-primary bg-opacity-10 text-primary px-3 py-2 rounded-pill fw-semibold">
                  Módulo Académico
                </span>
              </div>

              <h4 className="fw-bold text-dark mb-2">Gestión de Estudiantes</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Consulta la nómina general de alumnos matriculados, filtra por carrera y estado académico,
                accede a fichas integrales, actualiza antecedentes y gestiona credenciales de acceso.
              </p>

              <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                <Link
                  to="/students"
                  className="btn btn-primary fw-semibold px-4 py-2 rounded-3 d-flex align-items-center gap-2"
                >
                  <i className="bi bi-list-ul"></i>
                  <span>Ver Estudiantes</span>
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Card 2: Career and Study Plan Management */}
        <div className="col-12 col-lg-4">
          <div className="card h-100 border-0 shadow-sm rounded-4 transition-hover overflow-hidden">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-success"
                  style={{ backgroundColor: '#ecfdf5', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-journal-bookmark-fill fs-3 text-success"></i>
                </div>
                <span className="badge bg-success bg-opacity-10 text-success px-3 py-2 rounded-pill fw-semibold">
                  Módulo Curricular
                </span>
              </div>

              <h4 className="fw-bold text-dark mb-2">Carreras y Planes</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Supervisa la oferta académica técnica del IPT (4 semestres), administra planes de estudio vigentes
                e históricos, controla la admisión de nuevos postulantes y actualiza programas.
              </p>

              <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                <Link
                  to="/careers"
                  className="btn btn-success fw-semibold px-4 py-2 rounded-3 d-flex align-items-center gap-2 text-white"
                  style={{ backgroundColor: '#059669', borderColor: '#059669' }}
                >
                  <i className="bi bi-mortarboard"></i>
                  <span>Gestionar Carreras</span>
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Card 3: Subject & Prerequisite Management (Epic 3) */}
        <div className="col-12 col-lg-4">
          <div className="card h-100 border-0 shadow-sm rounded-4 transition-hover overflow-hidden">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-warning"
                  style={{ backgroundColor: '#fffbeb', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-diagram-3-fill fs-3 text-warning"></i>
                </div>
                <span className="badge bg-warning bg-opacity-10 text-dark px-3 py-2 rounded-pill fw-semibold">
                  Épica 3
                </span>
              </div>

              <h4 className="fw-bold text-dark mb-2">Gestión de Asignaturas</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Administra las asignaturas de 1° a 4° semestre, créditos SCT (1 a 7), horas pedagógicas TEL (&lt; 8 hrs)
                y hasta 3 prerrequisitos por asignatura dentro de cada plan curricular.
              </p>

              <div className="d-flex flex-wrap gap-2 pt-2 border-top">
                <Link
                  to="/careers"
                  className="btn btn-warning text-dark fw-bold px-4 py-2 rounded-3 d-flex align-items-center gap-2"
                >
                  <i className="bi bi-diagram-3"></i>
                  <span>Mallas y Asignaturas</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Info & Policy Guidelines */}
      <div className="card border-0 shadow-sm rounded-4 p-4 bg-light">
        <div className="d-flex align-items-center gap-3 mb-2">
          <i className="bi bi-info-circle-fill text-primary fs-4"></i>
          <h5 className="fw-bold mb-0 text-dark">Políticas Académicas y Curriculares IPT</h5>
        </div>
        <div className="row g-3 mt-1 text-muted small">
          <div className="col-12 col-md-4">
            <div className="p-3 bg-white rounded-3 border h-100">
              <strong className="text-dark d-block mb-1">
                <i className="bi bi-clock-history me-1 text-primary"></i> Duración Estándar (2 Años)
              </strong>
              Todas las carreras técnicas impartidas tienen una duración fija de 4 semestres académicos con asignaturas distribuidas secuencialmente.
            </div>
          </div>
          <div className="col-12 col-md-4">
            <div className="p-3 bg-white rounded-3 border h-100">
              <strong className="text-dark d-block mb-1">
                <i className="bi bi-hourglass-split me-1 text-warning"></i> Horas TEL y Créditos SCT
              </strong>
              Horas TEL (Teoría, Ejercicios, Laboratorio) no negativas y suma menor a 8 hrs semanales. Créditos SCT entre 1 y 7.
            </div>
          </div>
          <div className="col-12 col-md-4">
            <div className="p-3 bg-white rounded-3 border h-100">
              <strong className="text-dark d-block mb-1">
                <i className="bi bi-shield-check me-1 text-success"></i> Reglas de Prerrequisitos
              </strong>
              Máximo 3 prerrequisitos por asignatura, pertenecientes estrictamente al mismo plan de estudios y de semestres anteriores.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomeAdmin;
