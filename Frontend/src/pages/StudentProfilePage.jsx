import { useState, useEffect, useCallback } from 'react';
import StudentDataService from '../services/student.service';
import { useAuth } from '../context/AuthContext';

const StudentProfilePage = () => {
  const { currentUserRun } = useAuth();
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalMessage, setModalMessage] = useState(null);

  const loadMyProfile = useCallback(async () => {
    try {
      setLoading(true);
      const response = await StudentDataService.getByRun(currentUserRun);
      setStudent(response.data);
      setError(null);
    } catch {
      setError('No se pudo encontrar tu ficha académica con el RUN actual de sesión.');
    } finally {
      setLoading(false);
    }
  }, [currentUserRun]);

  useEffect(() => {
    loadMyProfile();
  }, [loadMyProfile]);

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'REGULAR': return 'badge-status-regular';
      case 'POSTERGACION': return 'badge-status-postergacion';
      case 'RETIRO_TEMPORAL': return 'badge-status-retiro';
      case 'EGRESADO': return 'badge-status-egresado';
      case 'ELIMINADO': return 'badge-status-eliminado';
      default: return 'bg-secondary';
    }
  };

  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status"></div>
        <p className="mt-2 text-muted">Cargando tu información académica...</p>
      </div>
    );
  }

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-1" style={{ color: 'var(--siga-primary)' }}>
            <i className="bi bi-person-circle me-2"></i> Mi Perfil de Estudiante
          </h2>
          <p className="text-muted small mb-0">
            Vista personal del alumno autenticado. Acceso a situación curricular e historial.
          </p>
        </div>
        <span className="badge bg-primary px-3 py-2">Rol: ESTUDIANTE</span>
      </div>

      {error ? (
        <div className="alert alert-warning siga-card p-4 text-center">
          <i className="bi bi-person-x-fill fs-2 text-warning d-block mb-2"></i>
          <h5>No hay estudiante asociado al RUN {currentUserRun}</h5>
          <p className="text-muted small mb-3">
            Puedes cambiar a rol <strong>ADMIN</strong> en la barra superior para registrar un nuevo estudiante con ese RUN o con otro de tu preferencia.
          </p>
        </div>
      ) : (
        <div className="row g-4">
          <div className="col-lg-6">
            <div className="siga-card p-4">
              <div className="d-flex align-items-center mb-3">
                <div
                  className="rounded-circle d-flex align-items-center justify-content-center text-white me-3"
                  style={{ width: '60px', height: '60px', backgroundColor: 'var(--siga-primary)', fontSize: '1.5rem' }}
                >
                  <i className="bi bi-person-fill"></i>
                </div>
                <div>
                  <h4 className="fw-bold mb-0">{student.fullName}</h4>
                  <small className="text-muted font-monospace">{student.email}</small>
                </div>
              </div>

              <hr />

              <div className="row g-3">
                <div className="col-6">
                  <span className="text-muted small d-block">RUN:</span>
                  <span className="fw-semibold font-monospace">{student.run}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Estado Académico:</span>
                  <span className={`badge ${getStatusBadgeClass(student.academicStatus)}`}>
                    {student.academicStatus === "RETIRO_TEMPORAL" ? "RETIRO TEMPORAL" : student.academicStatus}
                  </span>
                </div>
                <div className="col-12">
                  <span className="text-muted small d-block">Carrera:</span>
                  <span className="fw-bold" style={{ color: 'var(--siga-primary)' }}>
                    {student.careerName}
                  </span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Plan de Estudios:</span>
                  <span className="badge bg-secondary">{student.studyPlanCode}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Duración:</span>
                  <span>4 Semestres</span>
                </div>
              </div>
            </div>
          </div>

          <div className="col-lg-6">
            <div className="siga-card p-4">
              <h5 className="fw-bold mb-3" style={{ color: 'var(--siga-primary)' }}>
                <i className="bi bi-clock-history me-2"></i> Mi Historial Curricular
              </h5>
              <p className="text-muted small mb-4">
                Consulta los avances de tus asignaturas cursadas y calificaciones de períodos cerrados.
              </p>

              <div className="d-grid gap-3">
                <button
                  type="button"
                  className="btn btn-outline-primary p-3 text-start d-flex justify-content-between align-items-center"
                  onClick={() =>
                    setModalMessage(
                      'Podrás revisar las asignaturas cursadas y el avance de tu plan cuando se active el módulo de inscripciones y notas.'
                    )
                  }
                >
                  <div>
                    <div className="fw-bold">
                      <i className="bi bi-journal-check me-2"></i> Asignaturas Cursadas
                    </div>
                    <small className="text-muted">Asignaturas de tu plan 2021/2022</small>
                  </div>
                  <i className="bi bi-chevron-right text-muted"></i>
                </button>

                <button
                  type="button"
                  className="btn btn-outline-primary p-3 text-start d-flex justify-content-between align-items-center"
                  onClick={() =>
                    setModalMessage(
                      'Tus calificaciones finales estarán disponibles una vez que los docentes las ingresen y se cierre el período académico.'
                    )
                  }
                >
                  <div>
                    <div className="fw-bold">
                      <i className="bi bi-card-checklist me-2"></i> Mis Calificaciones
                    </div>
                    <small className="text-muted">Historial de notas finales y condición de aprobación</small>
                  </div>
                  <i className="bi bi-chevron-right text-muted"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Placeholder */}
      {modalMessage && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content siga-card">
              <div className="modal-header">
                <h5 className="modal-title fw-bold" style={{ color: 'var(--siga-primary)' }}>
                  <i className="bi bi-info-circle-fill me-2"></i> Próxima Funcionalidad
                </h5>
                <button type="button" className="btn-close" onClick={() => setModalMessage(null)}></button>
              </div>
              <div className="modal-body py-4">
                <p className="mb-0 text-muted">{modalMessage}</p>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-siga-primary" onClick={() => setModalMessage(null)}>
                  Entendido
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StudentProfilePage;
