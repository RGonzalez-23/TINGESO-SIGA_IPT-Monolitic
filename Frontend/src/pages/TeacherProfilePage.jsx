import { useState, useEffect, useCallback } from 'react';
import teacherService from '../services/teacher.service';
import userService from '../services/user.service';
import { useAuth } from '../context/AuthContext';

/**
 * TeacherProfilePage provides teachers with a dedicated view of their personal
 * and academic information, along with self-service password changing.
 * Layer: Frontend Page.
 */
const TeacherProfilePage = () => {
  const { currentUserRun, user } = useAuth();
  const [teacher, setTeacher] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Password change state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdLoading, setPwdLoading] = useState(false);
  const [pwdSuccess, setPwdSuccess] = useState(null);
  const [pwdError, setPwdError] = useState(null);

  const loadMyProfile = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await teacherService.getByRun(currentUserRun);
      setTeacher(response.data);
    } catch (err) {
      console.error('Error loading teacher profile:', err);
      setError('No se pudo encontrar tu ficha docente con el RUN actual de sesión.');
    } finally {
      setLoading(false);
    }
  }, [currentUserRun]);

  useEffect(() => {
    if (currentUserRun) {
      loadMyProfile();
    }
  }, [currentUserRun, loadMyProfile]);

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    setPwdSuccess(null);
    setPwdError(null);

    if (!currentPassword) {
      setPwdError('Debes ingresar tu contraseña actual.');
      return;
    }

    if (newPassword.length < 6) {
      setPwdError('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }

    if (newPassword === currentPassword) {
      setPwdError('La nueva contraseña debe ser distinta a la contraseña actual.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPwdError('Las contraseñas no coinciden. Por favor verifica.');
      return;
    }

    try {
      setPwdLoading(true);
      await userService.changePassword(currentUserRun, newPassword, false, currentPassword);
      setPwdSuccess('¡Contraseña actualizada exitosamente en Keycloak!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || 'Error al actualizar la contraseña.';
      setPwdError(msg);
    } finally {
      setPwdLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status"></div>
        <p className="mt-2 text-muted">Cargando tu información docente...</p>
      </div>
    );
  }

  if (error || !teacher) {
    return (
      <div className="container py-5">
        <div className="alert alert-danger rounded-4 shadow-sm p-4 text-center">
          <i className="bi bi-exclamation-triangle-fill fs-1 d-block mb-2"></i>
          <h4>Error al cargar perfil</h4>
          <p className="mb-3">{error || 'No fue posible recuperar los datos del docente.'}</p>
          <button onClick={loadMyProfile} className="btn btn-outline-danger btn-sm">
            Reintentar
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-1" style={{ color: 'var(--siga-primary)' }}>
            <i className="bi bi-person-badge-fill me-2"></i> Mi Perfil Docente
          </h2>
          <p className="text-muted small mb-0">
            Ficha académica del profesor autenticado, datos de contacto y administración de credenciales.
          </p>
        </div>
        <span className="badge bg-info text-dark px-3 py-2 fw-bold">ROL: DOCENTE</span>
      </div>

      <div className="row g-4">
        {/* Left Column: Teacher Identity Card */}
        <div className="col-12 col-lg-7">
          <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white h-100">
            {/* Banner */}
            <div
              className="p-4 text-white position-relative"
              style={{
                background: 'linear-gradient(135deg, #1d3557 0%, #2947c0 100%)',
              }}
            >
              <div className="d-flex align-items-center gap-3">
                <div
                  className="rounded-circle bg-white text-primary d-flex align-items-center justify-content-center shadow-sm fw-bold"
                  style={{ width: '64px', height: '64px', fontSize: '1.5rem' }}
                >
                  {teacher.firstName ? teacher.firstName.charAt(0) : 'D'}
                </div>
                <div>
                  <h4 className="fw-bold mb-1">{teacher.fullName}</h4>
                  <span className="badge bg-light text-dark font-monospace">RUN: {teacher.run}</span>
                </div>
              </div>
            </div>

            {/* Details Body */}
            <div className="card-body p-4">
              <h6 className="fw-bold text-dark border-bottom pb-2 mb-3">
                <i className="bi bi-person-lines-fill me-2 text-primary"></i>
                Antecedentes Académicos y de Contacto
              </h6>

              <div className="row g-3">
                <div className="col-md-6">
                  <label className="text-muted small d-block mb-1">Correo Institucional</label>
                  <span className="fw-semibold font-monospace text-primary">{teacher.email}</span>
                </div>

                <div className="col-md-6">
                  <label className="text-muted small d-block mb-1">Estado Contractual</label>
                  {teacher.isActive ? (
                    <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-3 py-1">
                      ACTIVO
                    </span>
                  ) : (
                    <span className="badge bg-secondary bg-opacity-10 text-secondary border border-secondary border-opacity-25 px-3 py-1">
                      INACTIVO
                    </span>
                  )}
                </div>

                <div className="col-md-6">
                  <label className="text-muted small d-block mb-1">Título Profesional</label>
                  <span className="fw-semibold text-dark">{teacher.professionalTitle}</span>
                </div>

                <div className="col-md-6">
                  <label className="text-muted small d-block mb-1">Grado Académico</label>
                  <span className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 px-2 py-1">
                    {teacher.academicDegreeDisplayName || teacher.academicDegree}
                  </span>
                </div>

                <div className="col-12 mt-4 pt-3 border-top">
                  <div className="p-3 bg-light rounded-3 d-flex align-items-center gap-2">
                    <i className="bi bi-info-circle-fill text-primary fs-5"></i>
                    <small className="text-muted">
                      Como docente de IPT, estás habilitado para ser asignado a secciones académicas en los períodos regulares y registrar calificaciones finales de los estudiantes.
                    </small>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Password Management */}
        <div className="col-12 col-lg-5">
          <div className="card border-0 shadow-sm rounded-4 p-4 bg-white h-100">
            <h5 className="fw-bold text-dark mb-2">
              <i className="bi bi-key-fill text-warning me-2"></i> Cambiar Mi Contraseña
            </h5>
            <p className="text-muted small mb-4">
              Actualiza directamente tu contraseña de acceso institucional en <strong>Keycloak</strong>.
            </p>

            {pwdSuccess && (
              <div className="alert alert-success alert-dismissible fade show rounded-3 small" role="alert">
                <i className="bi bi-check-circle-fill me-2"></i>
                {pwdSuccess}
                <button type="button" className="btn-close" onClick={() => setPwdSuccess(null)}></button>
              </div>
            )}

            {pwdError && (
              <div className="alert alert-danger alert-dismissible fade show rounded-3 small" role="alert">
                <i className="bi bi-exclamation-triangle-fill me-2"></i>
                {pwdError}
                <button type="button" className="btn-close" onClick={() => setPwdError(null)}></button>
              </div>
            )}

            <form onSubmit={handlePasswordChange}>
              <div className="mb-3">
                <label className="form-label small fw-semibold text-secondary">
                  Contraseña Actual <span className="text-danger">*</span>
                </label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="Ingresa tu contraseña actual"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
              </div>

              <div className="mb-3">
                <label className="form-label small fw-semibold text-secondary">
                  Nueva Contraseña <span className="text-danger">*</span>
                </label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="Mínimo 6 caracteres"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>

              <div className="mb-4">
                <label className="form-label small fw-semibold text-secondary">
                  Confirmar Nueva Contraseña <span className="text-danger">*</span>
                </label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="Repite la contraseña"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary w-100 fw-semibold py-2 rounded-3 shadow-sm"
                disabled={pwdLoading}
              >
                {pwdLoading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                    Actualizando en Keycloak...
                  </>
                ) : (
                  <>
                    <i className="bi bi-shield-check me-2"></i>
                    Actualizar Contraseña
                  </>
                )}
              </button>
            </form>

            <div className="mt-4 pt-3 border-top text-center">
              <small className="text-muted">
                Tu usuario de autenticación corresponde a tu RUN (<code>{user?.preferred_username || currentUserRun}</code>).
              </small>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TeacherProfilePage;
