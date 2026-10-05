import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import StudentDataService from '../services/student.service';
import { useAuth } from '../context/AuthContext';

const StudentDetailPage = () => {
  const { run } = useParams();
  const { isTeacher } = useAuth();
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalMessage, setModalMessage] = useState(null);

  const loadStudent = useCallback(async () => {
    try {
      setLoading(true);
      const response = await StudentDataService.getByRun(run);
      setStudent(response.data);
      setError(null);
    } catch (err) {
      setError(err.response?.data || 'No se pudo cargar la información del estudiante.');
    } finally {
      setLoading(false);
    }
  }, [run]);

  useEffect(() => {
    loadStudent();
  }, [loadStudent]);

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
        <p className="mt-2 text-muted">Cargando expediente del estudiante...</p>
      </div>
    );
  }

  if (error || !student) {
    return (
      <div className="container py-5">
        <div className="alert alert-danger" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error || 'Estudiante no encontrado.'}
        </div>
        <Link to="/students" className="btn btn-outline-secondary">
          <i className="bi bi-arrow-left me-1"></i> Volver a la lista
        </Link>
      </div>
    );
  }

  return (
    <div className="container py-4">
      {/* Breadcrumb / Back button */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <Link to="/students" className="btn btn-outline-secondary btn-sm">
          <i className="bi bi-arrow-left me-1"></i> Volver a la lista
        </Link>
        <span className="badge bg-light text-dark border font-monospace">RUN: {student.run}</span>
      </div>

      <div className="row g-4">
        {/* Left Column: Personal and Academic Info */}
        <div className="col-lg-6">
          <div className="siga-card h-100">
            <div className="siga-card-header d-flex justify-content-between align-items-center">
              <span>
                <i className="bi bi-person-vcard-fill me-2"></i>
                Ficha Personal y Matrícula
              </span>
              <span className={`badge px-3 py-1 rounded-pill ${getStatusBadgeClass(student.academicStatus)}`}>
                {student.academicStatus}
              </span>
            </div>
            <div className="card-body p-4">
              <div className="d-flex align-items-center mb-4">
                <div
                  className="rounded-circle d-flex align-items-center justify-content-center text-white me-3"
                  style={{ width: '60px', height: '60px', backgroundColor: 'var(--siga-primary)', fontSize: '1.5rem' }}
                >
                  <i className="bi bi-person-fill"></i>
                </div>
                <div>
                  <h4 className="fw-bold mb-0 text-dark">{student.fullName}</h4>
                  <small className="text-muted font-monospace">{student.email}</small>
                </div>
              </div>

              <hr className="my-3 text-muted" />

              <div className="row g-3">
                <div className="col-6">
                  <span className="text-muted small d-block">RUN Oficial:</span>
                  <span className="fw-semibold font-monospace">{student.run}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Nombres:</span>
                  <span className="fw-semibold">{student.firstName}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Apellido Paterno:</span>
                  <span className="fw-semibold">{student.paternalLastName}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Apellido Materno:</span>
                  <span className="fw-semibold">{student.maternalLastName}</span>
                </div>
                <div className="col-12">
                  <span className="text-muted small d-block">Carrera Asignada:</span>
                  <span className="fw-bold" style={{ color: 'var(--siga-primary)' }}>
                    {student.careerCode} - {student.careerName}
                  </span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Plan de Estudios:</span>
                  <span className="badge bg-secondary">{student.studyPlanCode}</span>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Duración Carrera:</span>
                  <span className="fw-semibold">4 Semestres (2 Años)</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Historial Académico */}
        <div className="col-lg-6">
          <div className="siga-card h-100">
            <div className="siga-card-header">
              <i className="bi bi-journal-bookmark-fill me-2"></i>
              Historial y Registro Académico
            </div>
            <div className="card-body p-4 d-flex flex-column justify-content-between">
              <div>
                <p className="text-muted small mb-4">
                  Acceso al registro histórico de asignaturas cursadas, estado de aprobación y calificaciones obtenidas.
                </p>

                {isTeacher ? (
                  <div className="alert alert-warning" role="alert">
                    <i className="bi bi-shield-lock-fill me-2"></i>
                    <strong>Restricción de Rol:</strong> Como Docente, solo tienes acceso a los datos de contacto y estado regular del estudiante. No tienes permisos para consultar su historial académico completo.
                  </div>
                ) : (
                  <div className="d-grid gap-3">
                    <button
                      type="button"
                      className="btn btn-outline-primary p-3 text-start d-flex justify-content-between align-items-center"
                      onClick={() =>
                        setModalMessage(
                          'El módulo de Asignaturas Cursadas se habilitará con la implementación de la Épica 6 (Inscripción Académica) y Épica 7 (Cierre de Semestre).'
                        )
                      }
                    >
                      <div>
                        <div className="fw-bold fs-6">
                          <i className="bi bi-collection-fill text-primary me-2"></i>
                          Asignaturas Cursadas
                        </div>
                        <small className="text-muted">Revisar avance curricular, asignaturas inscritas y aprobadas</small>
                      </div>
                      <i className="bi bi-chevron-right text-muted"></i>
                    </button>

                    <button
                      type="button"
                      className="btn btn-outline-primary p-3 text-start d-flex justify-content-between align-items-center"
                      onClick={() =>
                        setModalMessage(
                          'El módulo de Calificaciones Finales se habilitará con la implementación de la Épica 7 (Registro de calificaciones finales y cierre de semestre).'
                        )
                      }
                    >
                      <div>
                        <div className="fw-bold fs-6">
                          <i className="bi bi-card-checklist text-primary me-2"></i>
                          Calificaciones Finales
                        </div>
                        <small className="text-muted">Escala de 1,0 a 7,0 y condición de aprobación (≥ 4,0)</small>
                      </div>
                      <i className="bi bi-chevron-right text-muted"></i>
                    </button>
                  </div>
                )}
              </div>

              <div className="mt-4 pt-3 border-top text-muted small">
                <i className="bi bi-info-circle me-1"></i>
                La condición académica (REGULAR, EGRESADO, ELIMINADO) se actualiza de manera consolidada en cada cierre de período.
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Modal Placeholder */}
      {modalMessage && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content siga-card">
              <div className="modal-header">
                <h5 className="modal-title fw-bold" style={{ color: 'var(--siga-primary)' }}>
                  <i className="bi bi-clock-history me-2"></i> Próxima Funcionalidad
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

export default StudentDetailPage;
