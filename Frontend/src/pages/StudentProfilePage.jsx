import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import StudentDataService from '../services/student.service';
import UserService from '../services/user.service';
import { useAuth } from '../context/AuthContext';

const StudentProfilePage = () => {
  const { currentUserRun, user } = useAuth();
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalMessage, setModalMessage] = useState(null);

  // Password change state
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdLoading, setPwdLoading] = useState(false);
  const [pwdSuccess, setPwdSuccess] = useState(null);
  const [pwdError, setPwdError] = useState(null);

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

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    setPwdSuccess(null);
    setPwdError(null);

    if (newPassword.length < 6) {
      setPwdError('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPwdError('Las contraseñas no coinciden. Por favor verifica.');
      return;
    }

    try {
      setPwdLoading(true);
      await UserService.changePassword(currentUserRun, newPassword, false);
      setPwdSuccess('¡Contraseña actualizada exitosamente en Keycloak!');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || 'Error al actualizar la contraseña.';
      setPwdError(msg);
    } finally {
      setPwdLoading(false);
    }
  };

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
            Usuario autenticado en Keycloak: <strong>{user?.name}</strong> ({currentUserRun})
          </p>
        </div>
      ) : (
        <div className="row g-4">
          {/* Columna Izquierda: Ficha Académica */}
          <div className="col-lg-6">
            <div className="siga-card p-4 mb-4">
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
                    {student.academicStatus === 'RETIRO_TEMPORAL' ? 'RETIRO TEMPORAL' : student.academicStatus}
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
                  <div className="d-flex align-items-center gap-2">
                    <span className="badge bg-secondary">{student.studyPlanCode}</span>
                    {student.studyPlanId && (
                      <Link
                        to={`/study-plans/${student.studyPlanId}`}
                        className="btn btn-outline-primary btn-sm py-0 px-2 fw-semibold"
                        style={{ fontSize: '0.75rem' }}
                      >
                        <i className="bi bi-diagram-3 me-1"></i>
                        Ver Malla
                      </Link>
                    )}
                  </div>
                </div>
                <div className="col-6">
                  <span className="text-muted small d-block">Duración:</span>
                  <span>4 Semestres</span>
                </div>
              </div>
            </div>

            {/* Tarjeta de Seguridad / Cambio de Contraseña */}
            <div className="siga-card p-4">
              <h5 className="fw-bold mb-3" style={{ color: 'var(--siga-primary)' }}>
                <i className="bi bi-shield-lock-fill me-2"></i> Seguridad: Cambiar mi Contraseña
              </h5>
              <p className="text-muted small mb-3">
                Actualiza tu contraseña de acceso a Keycloak. El cambio se aplicará de inmediato.
              </p>

              {pwdSuccess && (
                <div className="alert alert-success py-2 small mb-3" role="alert">
                  <i className="bi bi-check-circle-fill me-2"></i>
                  {pwdSuccess}
                </div>
              )}

              {pwdError && (
                <div className="alert alert-danger py-2 small mb-3" role="alert">
                  <i className="bi bi-exclamation-triangle-fill me-2"></i>
                  {pwdError}
                </div>
              )}

              <form onSubmit={handlePasswordChange}>
                <div className="mb-3">
                  <label className="form-label small fw-semibold">Nueva Contraseña</label>
                  <input
                    type="password"
                    className="form-control form-control-sm"
                    placeholder="Mínimo 6 caracteres"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                  />
                </div>

                <div className="mb-3">
                  <label className="form-label small fw-semibold">Confirmar Nueva Contraseña</label>
                  <input
                    type="password"
                    className="form-control form-control-sm"
                    placeholder="Repite la contraseña"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-primary btn-sm fw-semibold"
                  style={{ backgroundColor: 'var(--siga-primary)' }}
                  disabled={pwdLoading}
                >
                  {pwdLoading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                      Actualizando...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-key-fill me-1"></i> Actualizar Contraseña
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>

          {/* Columna Derecha: Historial Curricular */}
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
