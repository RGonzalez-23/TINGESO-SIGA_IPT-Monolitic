import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import studentService from '../services/student.service';

const HomeStudent = () => {
  const { user, currentUserRun } = useAuth();
  const [student, setStudent] = useState(null);

  useEffect(() => {
    const fetchStudentData = async () => {
      if (!currentUserRun) return;
      try {
        const res = await studentService.getByRun(currentUserRun);
        setStudent(res.data);
      } catch (err) {
        console.error('Error fetching student details:', err);
      }
    };

    fetchStudentData();
  }, [currentUserRun]);

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
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-black small fw-semibold">
            <i className="bi bi-mortarboard"></i>
            <span>Portal del Estudiante IPT</span>
          </div>
          <h1 className="display-6 fw-bold mb-2">
            ¡Hola, {student?.firstName || user?.name || 'Estudiante'}!
          </h1>
          <p className="lead mb-0 text-white-75 fs-6">
            Bienvenido/a a tu portal de autoservicio académico en el Instituto Profesional de Tecnología.
            {student?.careerName && (
              <span className="d-block mt-1 text-white">
                Carrera: <strong>{student.careerName}</strong> &bull; Plan de Estudios: <strong>{student.studyPlanCode}</strong>
              </span>
            )}
          </p>
        </div>
      </div>

      {/* Action Cards */}
      <div className="row g-4 mb-4">
        {/* Banner Card: Inscripción Académica */}
        <div className="col-12">
          <div className="card border-0 shadow-sm rounded-4 border-start border-4 border-success">
            <div className="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-3">
              <div>
                <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 mb-2">
                  Inscripciones Abiertas
                </span>
                <h4 className="fw-bold mb-1">Inscripción Académica IPT</h4>
                <p className="text-muted small mb-0">
                  Revisa la oferta elegible según tu avance curricular, cupos disponibles y selecciona tus asignaturas.
                </p>
              </div>
              <Link to="/enrollment" className="btn btn-success fw-bold px-4 py-2 rounded-3 shadow-sm">
                <i className="bi bi-pencil-square me-2"></i>
                Inscribir asignaturas
              </Link>
            </div>
          </div>
        </div>

        {/* Card 1: Mis Asignaturas en Curso y Horario */}
        <div className="col-12 col-md-6 col-lg-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden border-top border-4 border-primary">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-primary"
                  style={{ backgroundColor: '#eff6ff', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-calendar-week-fill fs-3 text-primary"></i>
                </div>
                <span className="badge bg-primary bg-opacity-10 text-primary px-3 py-2 rounded-pill fw-semibold">
                  Semestre Activo
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Mis Asignaturas en Curso</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Consulta en detalle las asignaturas que estás cursando en este período (estado <strong>INSCRITA</strong>), docentes asignados y tu <strong>malla horaria semanal</strong>.
              </p>
              <div className="pt-2 border-top">
                <Link to="/student/my-courses" className="btn btn-outline-primary fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-arrow-right-circle me-2"></i>
                  Ver Asignaturas y Horario
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Card 2: Mi Malla Curricular */}
        <div className="col-12 col-md-6 col-lg-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden border-top border-4 border-success">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-success"
                  style={{ backgroundColor: '#ecfdf5', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-diagram-3-fill fs-3 text-success"></i>
                </div>
                <span className="badge bg-success bg-opacity-10 text-success px-3 py-2 rounded-pill fw-semibold">
                  Avance Formativo
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Mi Malla Curricular</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Visualiza tu plan formativo con código de colores: <span className="text-success fw-bold">Aprobadas</span>, <span className="text-danger fw-bold">Reprobadas (con n° de veces)</span>, <span className="text-warning fw-bold">En Curso</span> y prerrequisitos.
              </p>
              <div className="pt-2 border-top">
                <Link to="/student/curriculum" className="btn btn-outline-success fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-mortarboard-fill me-2"></i>
                  Ver Mi Malla Curricular
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Card 3: Mi Historial Académico */}
        <div className="col-12 col-md-6 col-lg-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden border-top border-4 border-indigo" style={{ borderTopColor: '#6366f1' }}>
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center"
                  style={{ backgroundColor: '#eef2ff', width: '56px', height: '56px', color: '#4f46e5' }}
                >
                  <i className="bi bi-clock-history fs-3"></i>
                </div>
                <span className="badge bg-opacity-10 px-3 py-2 rounded-pill fw-semibold" style={{ backgroundColor: '#eef2ff', color: '#4f46e5' }}>
                  Expediente Oficial
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Mi Historial Académico</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Accede a tu registro histórico consolidado <strong>dividido por períodos académicos</strong>, con notas finales (1.0 a 7.0), condición de aprobación y promedio acumulado.
              </p>
              <div className="pt-2 border-top">
                <Link to="/student/academic-history" className="btn fw-semibold px-4 py-2 rounded-3 text-white" style={{ backgroundColor: '#4f46e5' }}>
                  <i className="bi bi-journal-text me-2"></i>
                  Ver Mi Historial Académico
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Card 4: Ficha y Credenciales */}
        <div className="col-12 col-md-6 col-lg-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden border-top border-4 border-secondary">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-secondary"
                  style={{ backgroundColor: '#f1f5f9', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-person-badge fs-3"></i>
                </div>
                <span className="badge bg-secondary bg-opacity-10 text-secondary px-3 py-2 rounded-pill fw-semibold">
                  Ficha y Seguridad
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Mi Perfil y Seguridad</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Consulta tus datos personales institucionales, situación de matrícula y actualiza tu contraseña de acceso a Keycloak.
              </p>
              <div className="pt-2 border-top">
                <Link to="/profile" className="btn btn-outline-secondary fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-shield-lock-fill me-2"></i>
                  Ir a Mi Perfil
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
