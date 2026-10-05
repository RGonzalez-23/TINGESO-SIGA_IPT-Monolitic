import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import StudentDataService from '../services/student.service';
import CareerDataService from '../services/career.service';
import UserService from '../services/user.service';
import { useAuth } from '../context/AuthContext';

const StudentDetailPage = () => {
  const { run } = useParams();
  const { isAdmin, isTeacher } = useAuth();
  const [student, setStudent] = useState(null);
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [modalMessage, setModalMessage] = useState(null);

  // Edit Student Modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState({
    firstName: '',
    paternalLastName: '',
    maternalLastName: '',
    careerCode: '',
    academicStatus: 'REGULAR',
  });
  // Password change inside edit modal
  const [newPassword, setNewPassword] = useState('');
  const [isTemporaryPassword, setIsTemporaryPassword] = useState(true);
  const [savingEdit, setSavingEdit] = useState(false);
  const [editError, setEditError] = useState(null);

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

  const loadCareers = useCallback(async () => {
    try {
      const response = await CareerDataService.getAll();
      setCareers(response.data);
    } catch (err) {
      console.error('Error loading careers:', err);
    }
  }, []);

  useEffect(() => {
    loadStudent();
    if (isAdmin) {
      loadCareers();
    }
  }, [loadStudent, loadCareers, isAdmin]);

  const openEditModal = () => {
    if (!student) return;
    setEditFormData({
      firstName: student.firstName,
      paternalLastName: student.paternalLastName,
      maternalLastName: student.maternalLastName,
      careerCode: student.careerCode,
      academicStatus: student.academicStatus,
    });
    setNewPassword('');
    setIsTemporaryPassword(true);
    setEditError(null);
    setIsEditModalOpen(true);
  };

  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    setEditError(null);

    // If password was entered, validate minimum length
    if (newPassword && newPassword.length < 6) {
      setEditError('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }

    try {
      setSavingEdit(true);

      // 1. Update student academic information
      await StudentDataService.update(student.run, editFormData);

      // 2. If password provided, update Keycloak password
      let pwdMsg = '';
      if (newPassword) {
        await UserService.changePassword(student.run, newPassword, isTemporaryPassword);
        pwdMsg = ' y contraseña actualizada en Keycloak';
      }

      setIsEditModalOpen(false);
      setSuccessMessage(`¡Estudiante ${student.run} modificado con éxito${pwdMsg}!`);
      setTimeout(() => setSuccessMessage(null), 4000);
      loadStudent();
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || err.response?.data || 'Error al actualizar el estudiante.';
      setEditError(msg);
    } finally {
      setSavingEdit(false);
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
      {/* Top Header & Action Buttons */}
      <div className="d-flex justify-content-between align-items-center mb-4">
        <Link to="/students" className="btn btn-outline-secondary btn-sm">
          <i className="bi bi-arrow-left me-1"></i> Volver a la lista
        </Link>
        <div className="d-flex align-items-center gap-2">
          {isAdmin && (
            <button
              type="button"
              className="btn btn-warning btn-sm fw-bold d-flex align-items-center gap-1 shadow-sm text-dark"
              onClick={openEditModal}
              title="Modificar datos y contraseña del estudiante"
            >
              <i className="bi bi-pencil-square"></i>
              <span>Modificar Estudiante</span>
            </button>
          )}
          <span className="badge bg-light text-dark border font-monospace">RUN: {student.run}</span>
        </div>
      </div>

      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {successMessage}
          <button type="button" className="btn-close" onClick={() => setSuccessMessage(null)}></button>
        </div>
      )}

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
                {student.academicStatus === 'RETIRO_TEMPORAL' ? 'RETIRO TEMPORAL' : student.academicStatus}
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
                          <i className="bi bi-collection-fill me-2"></i>
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
                          <i className="bi bi-card-checklist me-2"></i>
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

      {/* Edit Student Modal (Including Keycloak Password Update) */}
      {isEditModalOpen && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content siga-card">
              <form onSubmit={handleUpdateSubmit}>
                <div className="modal-header">
                  <h5 className="modal-title fw-bold" style={{ color: 'var(--siga-primary)' }}>
                    <i className="bi bi-pencil-square me-2"></i>
                    Modificar Estudiante ({student.run})
                  </h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setIsEditModalOpen(false)}
                    disabled={savingEdit}
                  ></button>
                </div>

                <div className="modal-body p-4">
                  {editError && (
                    <div className="alert alert-danger py-2 small mb-3">
                      <i className="bi bi-exclamation-triangle-fill me-2"></i>
                      {editError}
                    </div>
                  )}

                  {/* Section 1: Personal Information */}
                  <h6 className="fw-bold text-dark mb-3 border-bottom pb-2">
                    <i className="bi bi-person-fill text-primary me-2"></i>
                    Información Personal y Académica
                  </h6>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Nombres</label>
                    <input
                      type="text"
                      className="form-control"
                      value={editFormData.firstName}
                      onChange={(e) => setEditFormData({ ...editFormData, firstName: e.target.value })}
                      required
                    />
                  </div>

                  <div className="row mb-3">
                    <div className="col-md-6">
                      <label className="form-label small fw-semibold">Apellido Paterno</label>
                      <input
                        type="text"
                        className="form-control"
                        value={editFormData.paternalLastName}
                        onChange={(e) => setEditFormData({ ...editFormData, paternalLastName: e.target.value })}
                        required
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label small fw-semibold">Apellido Materno</label>
                      <input
                        type="text"
                        className="form-control"
                        value={editFormData.maternalLastName}
                        onChange={(e) => setEditFormData({ ...editFormData, maternalLastName: e.target.value })}
                        required
                      />
                    </div>
                  </div>

                  <div className="row mb-3">
                    <div className="col-md-6">
                      <label className="form-label small fw-semibold">Carrera</label>
                      <select
                        className="form-select"
                        value={editFormData.careerCode}
                        onChange={(e) => setEditFormData({ ...editFormData, careerCode: e.target.value })}
                      >
                        {careers.map((c) => (
                          <option key={c.code} value={c.code}>
                            {c.code} - {c.name}
                          </option>
                        ))}
                      </select>
                      <div className="form-text small">
                        * Solo modificable si no registra inscripciones ni historial.
                      </div>
                    </div>
                    <div className="col-md-6">
                      <label className="form-label small fw-semibold">Estado Académico (Admin)</label>
                      <select
                        className="form-select"
                        value={editFormData.academicStatus}
                        onChange={(e) => setEditFormData({ ...editFormData, academicStatus: e.target.value })}
                      >
                        <option value="REGULAR">Regular</option>
                        <option value="POSTERGACION">Postergación</option>
                        <option value="RETIRO_TEMPORAL">Retiro Temporal</option>
                      </select>
                      <div className="form-text small text-danger">
                        * EGRESADO y ELIMINADO están protegidos para cierre de semestre.
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Keycloak Password Change */}
                  <h6 className="fw-bold text-dark mt-4 mb-3 border-bottom pb-2">
                    <i className="bi bi-shield-lock-fill text-warning me-2"></i>
                    Cambio de Contraseña en Keycloak (Opcional)
                  </h6>

                  <p className="text-muted small mb-2">
                    Si deseas actualizar la contraseña del estudiante en Keycloak, escríbela a continuación. Si no deseas cambiarla, déjala en blanco.
                  </p>

                  <div className="row g-2 align-items-center">
                    <div className="col-md-7">
                      <label className="form-label small fw-semibold mb-1">Nueva Contraseña</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Dejar en blanco para no modificar"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                      />
                    </div>
                    <div className="col-md-5 pt-md-3">
                      <div className="form-check mt-md-2">
                        <input
                          className="form-check-input"
                          type="checkbox"
                          id="editTemporaryCheck"
                          checked={isTemporaryPassword}
                          disabled={!newPassword}
                          onChange={(e) => setIsTemporaryPassword(e.target.checked)}
                        />
                        <label className="form-check-label small" htmlFor="editTemporaryCheck">
                          Exigir cambio en el próximo inicio de sesión
                        </label>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => setIsEditModalOpen(false)}
                    disabled={savingEdit}
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="btn btn-siga-primary btn-sm fw-semibold"
                    disabled={savingEdit}
                  >
                    {savingEdit ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                        Guardando Cambios...
                      </>
                    ) : (
                      'Guardar Cambios'
                    )}
                  </button>
                </div>
              </form>
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
