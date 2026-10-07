import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import studentService from '../services/student.service';

const HomeStudent = () => {
  const { user, currentUserRun } = useAuth();
  const [student, setStudent] = useState(null);
  const [loadingStudent, setLoadingStudent] = useState(true);

  useEffect(() => {
    const fetchStudentData = async () => {
      if (!currentUserRun) return;
      try {
        setLoadingStudent(true);
        const res = await studentService.getByRun(currentUserRun);
        setStudent(res.data);
      } catch (err) {
        console.error('Error fetching student details:', err);
      } finally {
        setLoadingStudent(false);
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
          <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-white small fw-semibold">
            <i className="bi bi-mortarboard"></i>
            <span>Portal del Estudiante IPT</span>
          </div>
          <h1 className="display-6 fw-bold mb-2">
            ¡Hola, {student?.firstName || user?.name || 'Estudiante'}!
          </h1>
          <p className="lead mb-0 text-white-50 fs-6">
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
        {/* Card 1: Perfil y Seguridad */}
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
                Consulta tu situación académica oficial, datos personales de matrícula y actualiza tu contraseña de acceso a los sistemas institucionales.
              </p>
              <div className="pt-2 border-top">
                <Link to="/profile" className="btn btn-outline-success fw-semibold px-4 py-2 rounded-3">
                  <i className="bi bi-arrow-right-circle me-2"></i>
                  Ir a Mi Perfil Académico
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Card 2: Mi Malla Curricular */}
        <div className="col-12 col-md-6">
          <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
            <div className="card-body p-4 d-flex flex-column">
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div
                  className="rounded-3 p-3 d-flex align-items-center justify-content-center text-primary"
                  style={{ backgroundColor: '#eff6ff', width: '56px', height: '56px' }}
                >
                  <i className="bi bi-diagram-3-fill fs-3 text-primary"></i>
                </div>
                <span className="badge bg-primary bg-opacity-10 text-primary px-3 py-2 rounded-pill fw-semibold">
                  Plan de Estudios
                </span>
              </div>
              <h4 className="fw-bold text-dark mb-2">Mi Malla Curricular</h4>
              <p className="text-muted small mb-4 flex-grow-1">
                Accede a la estructura formativa de 4 semestres (2 años) de tu plan de estudios ({student?.studyPlanCode || 'Vigente'}), requisitos y asignaturas correspondientes.
              </p>
              <div className="pt-2 border-top">
                {loadingStudent ? (
                  <button className="btn btn-primary fw-semibold px-4 py-2 rounded-3" disabled>
                    <span className="spinner-border spinner-border-sm me-2"></span> Cargando...
                  </button>
                ) : student?.studyPlanId ? (
                  <Link
                    to={`/study-plans/${student.studyPlanId}`}
                    className="btn btn-primary fw-semibold px-4 py-2 rounded-3 text-white"
                  >
                    <i className="bi bi-mortarboard-fill me-2"></i>
                    Mi malla
                  </Link>
                ) : (
                  <span className="text-muted small">No hay plan de estudios asociado.</span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomeStudent;
