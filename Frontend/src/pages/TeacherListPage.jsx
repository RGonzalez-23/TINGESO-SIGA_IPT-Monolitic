import { useState, useEffect, useCallback } from 'react';
import teacherService from '../services/teacher.service';
import userService from '../services/user.service';
import { runValidator, sanitizeForEmail } from '../utils/runValidator';

/**
 * TeacherListPage provides complete faculty management for ADMIN.
 * Features: listing, filtering by status, live search, creation with real-time RUT
 * validation and email preview, editing, status toggling, and physical deletion protection.
 */
const TeacherListPage = () => {
  const [teachers, setTeachers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [alert, setAlert] = useState(null); // { type: 'success' | 'danger', message: '' }

  // Search & Filter
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // 'ALL' | 'ACTIVE' | 'INACTIVE'

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedTeacher, setSelectedTeacher] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Form states
  const [createForm, setCreateForm] = useState({
    run: '',
    firstName: '',
    paternalLastName: '',
    maternalLastName: '',
    professionalTitle: '',
    academicDegree: 'MAGISTER',
  });
  const [runValid, setRunValid] = useState(null);
  const [previewEmail, setPreviewEmail] = useState('');

  const [editForm, setEditForm] = useState({
    firstName: '',
    paternalLastName: '',
    maternalLastName: '',
    professionalTitle: '',
    academicDegree: 'MAGISTER',
    isActive: true,
  });

  // Password reset inside Edit Modal
  const [resetPasswordOpen, setResetPasswordOpen] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [pwdSubmitting, setPwdSubmitting] = useState(false);

  const loadTeachers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {};
      if (statusFilter === 'ACTIVE') params.active = true;
      if (statusFilter === 'INACTIVE') params.active = false;
      if (searchTerm.trim()) params.search = searchTerm.trim();

      const res = await teacherService.getAll(params);
      setTeachers(res.data);
    } catch (err) {
      console.error('Error loading teachers:', err);
      setError('No fue posible cargar el listado de docentes.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, searchTerm]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadTeachers();
    }, 250);
    return () => clearTimeout(timer);
  }, [loadTeachers]);

  // Live preview email calculation for Create Form
  useEffect(() => {
    if (createForm.firstName && createForm.paternalLastName) {
      // Immediate local preview stripped of accents while debouncing backend collision check
      const localFirst = sanitizeForEmail(createForm.firstName.split(' ')[0]);
      const localPaternal = sanitizeForEmail(createForm.paternalLastName);
      if (localFirst && localPaternal) {
        setPreviewEmail(`${localFirst}.${localPaternal}@sigaipt.cl`);
      }

      const timer = setTimeout(async () => {
        try {
          const res = await teacherService.previewEmail(
            createForm.firstName,
            createForm.paternalLastName,
            createForm.maternalLastName
          );
          if (res.data?.email) {
            setPreviewEmail(res.data.email);
          }
        } catch {
          // Fallback local preview without accents
          const first = sanitizeForEmail(createForm.firstName.split(' ')[0]);
          const last = sanitizeForEmail(createForm.paternalLastName);
          if (first && last) {
            setPreviewEmail(`${first}.${last}@sigaipt.cl`);
          }
        }
      }, 300);
      return () => clearTimeout(timer);
    } else {
      setPreviewEmail('');
    }
  }, [createForm.firstName, createForm.paternalLastName, createForm.maternalLastName]);

  // Handle RUN change in Create Form
  const handleRunChange = (e) => {
    const val = e.target.value;
    const formatted = runValidator.format(val);
    setCreateForm((prev) => ({ ...prev, run: formatted }));
    if (formatted.length >= 8) {
      setRunValid(runValidator.validate(formatted));
    } else {
      setRunValid(null);
    }
  };

  // Open Create Modal
  const handleOpenCreate = () => {
    setCreateForm({
      run: '',
      firstName: '',
      paternalLastName: '',
      maternalLastName: '',
      professionalTitle: '',
      academicDegree: 'MAGISTER',
    });
    setRunValid(null);
    setPreviewEmail('');
    setShowCreateModal(true);
  };

  // Submit Create Teacher
  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    if (!runValidator.validate(createForm.run)) {
      setAlert({ type: 'danger', message: 'El RUN ingresado no es válido según Módulo 11.' });
      return;
    }

    setSubmitting(true);
    setAlert(null);
    try {
      await teacherService.create({
        run: runValidator.clean(createForm.run),
        firstName: createForm.firstName.trim(),
        paternalLastName: createForm.paternalLastName.trim(),
        maternalLastName: createForm.maternalLastName.trim(),
        professionalTitle: createForm.professionalTitle.trim(),
        academicDegree: createForm.academicDegree,
      });

      setAlert({
        type: 'success',
        message: `Docente ${createForm.firstName} ${createForm.paternalLastName} registrado exitosamente. Se ha aprovisionado su cuenta en Keycloak con la clave temporal 'Siga2026!'.`,
      });
      setShowCreateModal(false);
      loadTeachers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al registrar el docente.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error de validación.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Open Edit Modal
  const handleOpenEdit = (teacher) => {
    setSelectedTeacher(teacher);
    setEditForm({
      firstName: teacher.firstName,
      paternalLastName: teacher.paternalLastName,
      maternalLastName: teacher.maternalLastName,
      professionalTitle: teacher.professionalTitle,
      academicDegree: teacher.academicDegree,
      isActive: teacher.isActive,
    });
    setResetPasswordOpen(false);
    setNewPassword('');
    setShowEditModal(true);
  };

  // Submit Update Teacher
  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setAlert(null);

    try {
      await teacherService.update(selectedTeacher.run, {
        firstName: editForm.firstName.trim(),
        paternalLastName: editForm.paternalLastName.trim(),
        maternalLastName: editForm.maternalLastName.trim(),
        professionalTitle: editForm.professionalTitle.trim(),
        academicDegree: editForm.academicDegree,
        isActive: editForm.isActive,
      });

      setAlert({
        type: 'success',
        message: `Docente ${selectedTeacher.run} actualizado correctamente.`,
      });
      setShowEditModal(false);
      loadTeachers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al actualizar el docente.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error de validación.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Reset Password for Teacher
  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!newPassword || newPassword.length < 6) {
      alert('La contraseña debe tener al menos 6 caracteres.');
      return;
    }

    setPwdSubmitting(true);
    try {
      await userService.changePassword(selectedTeacher.run, newPassword, true);
      alert(`Contraseña de ${selectedTeacher.fullName} actualizada exitosamente en Keycloak (requerirá cambio al próximo inicio de sesión).`);
      setResetPasswordOpen(false);
      setNewPassword('');
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Error al cambiar contraseña.';
      alert(msg);
    } finally {
      setPwdSubmitting(false);
    }
  };

  // Delete Teacher
  const handleDeleteTeacher = async (teacher) => {
    if (
      !window.confirm(
        `¿Confirmas la eliminación del docente ${teacher.run} - ${teacher.fullName}? Esta acción no se puede deshacer.`
      )
    ) {
      return;
    }

    setAlert(null);
    try {
      await teacherService.delete(teacher.run);
      setAlert({
        type: 'success',
        message: `Docente ${teacher.fullName} eliminado exitosamente.`,
      });
      loadTeachers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'No fue posible eliminar al docente.';
      setAlert({
        type: 'danger',
        message: typeof msg === 'string' ? msg : 'Error al eliminar docente.',
      });
    }
  };

  const activeCount = teachers.filter((t) => t.isActive).length;
  const inactiveCount = teachers.filter((t) => !t.isActive).length;

  return (
    <div className="container py-4">
      {/* Header Banner */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h2 className="fw-bold mb-1" style={{ color: 'var(--siga-primary)' }}>
            <i className="bi bi-person-video3 me-2"></i> Gestión de Docentes
          </h2>
          <p className="text-muted small mb-0">
            Administración del cuerpo académico de IPT, registro, situación contractual y asignaciones.
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          className="btn btn-primary fw-semibold px-4 py-2 rounded-3 shadow-sm d-flex align-items-center gap-2"
        >
          <i className="bi bi-person-plus-fill"></i>
          <span>Registrar Docente</span>
        </button>
      </div>

      {/* Global Alert Notification */}
      {alert && (
        <div
          className={`alert alert-${alert.type} alert-dismissible fade show rounded-3 shadow-sm border-0 d-flex align-items-center justify-content-between mb-4`}
          role="alert"
        >
          <div className="d-flex align-items-center gap-2">
            <i
              className={`bi ${
                alert.type === 'success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-triangle-fill text-danger'
              } fs-5`}
            ></i>
            <span>{alert.message}</span>
          </div>
          <button type="button" className="btn-close" onClick={() => setAlert(null)} aria-label="Cerrar"></button>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="row g-3 mb-4">
        <div className="col-12 col-md-4">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white">
            <div className="d-flex align-items-center gap-3">
              <div className="p-3 rounded-circle bg-primary bg-opacity-10 text-primary">
                <i className="bi bi-people-fill fs-3"></i>
              </div>
              <div>
                <span className="text-muted small fw-semibold">Total Docentes</span>
                <h3 className="fw-bold mb-0 text-dark">{teachers.length}</h3>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-4">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white">
            <div className="d-flex align-items-center gap-3">
              <div className="p-3 rounded-circle bg-success bg-opacity-10 text-success">
                <i className="bi bi-person-check-fill fs-3"></i>
              </div>
              <div>
                <span className="text-muted small fw-semibold">Docentes Activos</span>
                <h3 className="fw-bold mb-0 text-success">{activeCount}</h3>
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-md-4">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white">
            <div className="d-flex align-items-center gap-3">
              <div className="p-3 rounded-circle bg-secondary bg-opacity-10 text-secondary">
                <i className="bi bi-person-x-fill fs-3"></i>
              </div>
              <div>
                <span className="text-muted small fw-semibold">Docentes Inactivos</span>
                <h3 className="fw-bold mb-0 text-secondary">{inactiveCount}</h3>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="card border-0 shadow-sm rounded-4 p-3 mb-4 bg-white">
        <div className="row g-3 align-items-center">
          <div className="col-12 col-md-7">
            <div className="input-group">
              <span className="input-group-text bg-light border-0">
                <i className="bi bi-search text-muted"></i>
              </span>
              <input
                type="text"
                className="form-control bg-light border-0"
                placeholder="Buscar por RUN, nombre, correo o título..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              {searchTerm && (
                <button className="btn btn-light border-0" onClick={() => setSearchTerm('')}>
                  <i className="bi bi-x"></i>
                </button>
              )}
            </div>
          </div>

          <div className="col-12 col-md-5">
            <div className="d-flex align-items-center justify-content-md-end gap-2">
              <span className="text-muted small fw-semibold">Estado:</span>
              <div className="btn-group" role="group">
                <button
                  type="button"
                  className={`btn btn-sm ${statusFilter === 'ALL' ? 'btn-primary fw-bold' : 'btn-outline-secondary'}`}
                  onClick={() => setStatusFilter('ALL')}
                >
                  Todos
                </button>
                <button
                  type="button"
                  className={`btn btn-sm ${statusFilter === 'ACTIVE' ? 'btn-success fw-bold text-white' : 'btn-outline-secondary'}`}
                  onClick={() => setStatusFilter('ACTIVE')}
                >
                  Activos
                </button>
                <button
                  type="button"
                  className={`btn btn-sm ${statusFilter === 'INACTIVE' ? 'btn-secondary fw-bold text-white' : 'btn-outline-secondary'}`}
                  onClick={() => setStatusFilter('INACTIVE')}
                >
                  Inactivos
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Teachers Table */}
      <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white">
        <div className="card-body p-0">
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status"></div>
              <p className="mt-3 text-muted small">Cargando nómina docente...</p>
            </div>
          ) : error ? (
            <div className="text-center py-5 text-danger">
              <i className="bi bi-exclamation-triangle fs-1 d-block mb-2"></i>
              <p>{error}</p>
              <button className="btn btn-outline-primary btn-sm" onClick={loadTeachers}>
                Reintentar
              </button>
            </div>
          ) : teachers.length === 0 ? (
            <div className="text-center py-5 text-muted">
              <i className="bi bi-person-x fs-1 text-secondary d-block mb-2"></i>
              <h6>No se encontraron docentes</h6>
              <p className="small text-muted mb-3">
                {searchTerm || statusFilter !== 'ALL'
                  ? 'Intenta ajustar los criterios de búsqueda o filtros.'
                  : 'Registra el primer docente para comenzar.'}
              </p>
              {!searchTerm && statusFilter === 'ALL' && (
                <button onClick={handleOpenCreate} className="btn btn-primary btn-sm">
                  + Registrar Primer Docente
                </button>
              )}
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th className="ps-4">RUN</th>
                    <th>Docente</th>
                    <th>Contacto Institucional</th>
                    <th>Título Profesional</th>
                    <th>Grado Académico</th>
                    <th className="text-center">Estado</th>
                    <th className="text-end pe-4">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {teachers.map((teacher) => (
                    <tr key={teacher.run}>
                      <td className="ps-4 font-monospace fw-bold text-dark">{teacher.run}</td>
                      <td>
                        <div className="fw-semibold text-dark">{teacher.fullName}</div>
                      </td>
                      <td>
                        <span className="small text-muted font-monospace">{teacher.email}</span>
                      </td>
                      <td>
                        <span className="small text-dark">{teacher.professionalTitle}</span>
                      </td>
                      <td>
                        <span className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 px-2 py-1">
                          {teacher.academicDegreeDisplayName || teacher.academicDegree}
                        </span>
                      </td>
                      <td className="text-center">
                        {teacher.isActive ? (
                          <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-3 py-1">
                            ACTIVO
                          </span>
                        ) : (
                          <span className="badge bg-secondary bg-opacity-10 text-secondary border border-secondary border-opacity-25 px-3 py-1">
                            INACTIVO
                          </span>
                        )}
                      </td>
                      <td className="text-end pe-4">
                        <div className="btn-group btn-group-sm">
                          <button
                            onClick={() => handleOpenEdit(teacher)}
                            className="btn btn-outline-secondary btn-sm"
                            title="Editar Datos del Docente"
                          >
                            <i className="bi bi-pencil-square"></i>
                          </button>
                          <button
                            onClick={() => handleDeleteTeacher(teacher)}
                            className="btn btn-outline-danger btn-sm"
                            title="Eliminar Docente"
                          >
                            <i className="bi bi-trash"></i>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* ================= MODAL: REGISTRAR DOCENTE ================= */}
      {showCreateModal && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                <h5 className="modal-title fw-bold text-dark mb-0">
                  <i className="bi bi-person-plus-fill text-primary me-2"></i>
                  Registrar Nuevo Docente
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowCreateModal(false)}
                  aria-label="Cerrar"
                ></button>
              </div>

              <form onSubmit={handleCreateSubmit}>
                <div className="modal-body p-4">
                  <div className="row g-3">
                    {/* RUN */}
                    <div className="col-md-6">
                      <label className="form-label fw-semibold text-secondary small">
                        RUN (con guion y dígito verificador) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className={`form-control font-monospace ${
                          runValid === true ? 'is-valid' : runValid === false ? 'is-invalid' : ''
                        }`}
                        placeholder="Ej: 11111111-1"
                        maxLength="12"
                        value={createForm.run}
                        onChange={handleRunChange}
                        required
                      />
                      {runValid === false && (
                        <div className="invalid-feedback small">RUN inválido según Módulo 11 chileno.</div>
                      )}
                      {runValid === true && (
                        <div className="valid-feedback small">RUN válido.</div>
                      )}
                    </div>

                    {/* Grado Académico */}
                    <div className="col-md-6">
                      <label className="form-label fw-semibold text-secondary small">
                        Grado Académico <span className="text-danger">*</span>
                      </label>
                      <select
                        className="form-select"
                        value={createForm.academicDegree}
                        onChange={(e) => setCreateForm({ ...createForm, academicDegree: e.target.value })}
                        required
                      >
                        <option value="LICENCIATURA">Licenciatura</option>
                        <option value="MAGISTER">Magíster</option>
                        <option value="DOCTORADO">Doctorado</option>
                      </select>
                    </div>

                    {/* Nombres */}
                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Nombres <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Ej: Pedro Antonio"
                        maxLength="100"
                        value={createForm.firstName}
                        onChange={(e) => setCreateForm({ ...createForm, firstName: e.target.value })}
                        required
                      />
                    </div>

                    {/* Apellido Paterno */}
                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Apellido Paterno <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Ej: González"
                        maxLength="100"
                        value={createForm.paternalLastName}
                        onChange={(e) => setCreateForm({ ...createForm, paternalLastName: e.target.value })}
                        required
                      />
                    </div>

                    {/* Apellido Materno */}
                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Apellido Materno <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Ej: Pérez"
                        maxLength="100"
                        value={createForm.maternalLastName}
                        onChange={(e) => setCreateForm({ ...createForm, maternalLastName: e.target.value })}
                        required
                      />
                    </div>

                    {/* Título Profesional */}
                    <div className="col-12">
                      <label className="form-label fw-semibold text-secondary small">
                        Título Profesional <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Ej: Ingeniero Civil en Informática"
                        maxLength="150"
                        value={createForm.professionalTitle}
                        onChange={(e) => setCreateForm({ ...createForm, professionalTitle: e.target.value })}
                        required
                      />
                    </div>

                    {/* Live Email Preview Box */}
                    <div className="col-12">
                      <div className="p-3 bg-light rounded-3 border">
                        <div className="d-flex align-items-center justify-content-between mb-1">
                          <span className="small fw-semibold text-dark">
                            <i className="bi bi-envelope-check me-1 text-primary"></i>
                            Correo Institucional Autogenerado:
                          </span>
                          <span className="badge bg-info bg-opacity-10 text-dark border small">
                            Resolución de colisiones activa
                          </span>
                        </div>
                        <div className="font-monospace fw-bold text-primary fs-6">
                          {previewEmail || 'nombre.apellido@sigaipt.cl'}
                        </div>
                        <small className="text-muted d-block mt-1">
                          Si ya existe un correo idéntico entre estudiantes o profesores, el sistema añadirá automáticamente iniciales del apellido materno (.x) y sufijos progresivos.
                        </small>
                      </div>
                    </div>

                    {/* IAM Notice */}
                    <div className="col-12">
                      <div className="alert alert-info py-2 px-3 small mb-0 d-flex align-items-center gap-2">
                        <i className="bi bi-shield-lock-fill fs-5 text-info"></i>
                        <span>
                          Al guardar, el docente se creará en estado <strong>ACTIVO</strong> y se aprovisionará automáticamente en <strong>Keycloak</strong> con el rol <code>TEACHER</code> y la clave temporal <code>Siga2026!</code>.
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-0 bg-light rounded-bottom-4 py-2">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => setShowCreateModal(false)}
                    disabled={submitting}
                  >
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-primary btn-sm px-4 fw-semibold" disabled={submitting}>
                    {submitting ? 'Registrando...' : 'Registrar Docente'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: EDITAR DOCENTE ================= */}
      {showEditModal && selectedTeacher && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                <h5 className="modal-title fw-bold text-dark mb-0">
                  <i className="bi bi-pencil-square text-primary me-2"></i>
                  Modificar Docente &bull; {selectedTeacher.run}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowEditModal(false)}
                  aria-label="Cerrar"
                ></button>
              </div>

              <form onSubmit={handleUpdateSubmit}>
                <div className="modal-body p-4">
                  <div className="row g-3">
                    <div className="col-md-6">
                      <label className="form-label fw-semibold text-secondary small">RUN</label>
                      <input
                        type="text"
                        className="form-control font-monospace bg-light"
                        value={selectedTeacher.run}
                        disabled
                      />
                    </div>

                    <div className="col-md-6">
                      <label className="form-label fw-semibold text-secondary small">Correo Institucional</label>
                      <input
                        type="text"
                        className="form-control font-monospace bg-light"
                        value={selectedTeacher.email}
                        disabled
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Nombres <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={editForm.firstName}
                        maxLength="100"
                        onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Apellido Paterno <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={editForm.paternalLastName}
                        maxLength="100"
                        onChange={(e) => setEditForm({ ...editForm, paternalLastName: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Apellido Materno <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={editForm.maternalLastName}
                        maxLength="100"
                        onChange={(e) => setEditForm({ ...editForm, maternalLastName: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-md-8">
                      <label className="form-label fw-semibold text-secondary small">
                        Título Profesional <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={editForm.professionalTitle}
                        maxLength="150"
                        onChange={(e) => setEditForm({ ...editForm, professionalTitle: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Grado Académico <span className="text-danger">*</span>
                      </label>
                      <select
                        className="form-select"
                        value={editForm.academicDegree}
                        onChange={(e) => setEditForm({ ...editForm, academicDegree: e.target.value })}
                        required
                      >
                        <option value="LICENCIATURA">Licenciatura</option>
                        <option value="MAGISTER">Magíster</option>
                        <option value="DOCTORADO">Doctorado</option>
                      </select>
                    </div>

                    {/* Estado Activo / Inactivo */}
                    <div className="col-12">
                      <div className="p-3 bg-light rounded-3 border d-flex justify-content-between align-items-center">
                        <div>
                          <label className="form-label fw-bold text-dark small mb-0">
                            Estado Contractual del Docente
                          </label>
                          <small className="text-muted d-block">
                            No es posible pasar a INACTIVO a un docente si tiene secciones asignadas en un período académico ABIERTO.
                          </small>
                        </div>
                        <div className="form-check form-switch fs-5">
                          <input
                            className="form-check-input"
                            type="checkbox"
                            role="switch"
                            checked={editForm.isActive}
                            onChange={(e) => setEditForm({ ...editForm, isActive: e.target.checked })}
                          />
                          <span className={`small fw-bold ms-2 fs-6 ${editForm.isActive ? 'text-success' : 'text-secondary'}`}>
                            {editForm.isActive ? 'ACTIVO' : 'INACTIVO'}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Password Reset Section for Admin */}
                    <div className="col-12">
                      <div className="border rounded-3 p-3">
                        <div className="d-flex justify-content-between align-items-center">
                          <div>
                            <span className="fw-semibold text-dark small">
                              <i className="bi bi-key-fill text-warning me-1"></i>
                              Restablecer Contraseña en Keycloak
                            </span>
                            <small className="text-muted d-block">
                              Permite asignar una nueva clave temporal al docente.
                            </small>
                          </div>
                          <button
                            type="button"
                            className="btn btn-outline-warning btn-sm"
                            onClick={() => setResetPasswordOpen(!resetPasswordOpen)}
                          >
                            {resetPasswordOpen ? 'Cancelar' : 'Restablecer Clave'}
                          </button>
                        </div>

                        {resetPasswordOpen && (
                          <div className="mt-3 pt-3 border-top">
                            <div className="row g-2 align-items-center">
                              <div className="col-md-8">
                                <input
                                  type="text"
                                  className="form-control form-control-sm font-monospace"
                                  placeholder="Nueva contraseña temporal (ej: Docente2026!)"
                                  value={newPassword}
                                  onChange={(e) => setNewPassword(e.target.value)}
                                />
                              </div>
                              <div className="col-md-4">
                                <button
                                  type="button"
                                  onClick={handleResetPassword}
                                  className="btn btn-warning btn-sm w-100 fw-bold"
                                  disabled={pwdSubmitting || !newPassword}
                                >
                                  {pwdSubmitting ? 'Guardando...' : 'Aplicar en Keycloak'}
                                </button>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-0 bg-light rounded-bottom-4 py-2">
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => setShowEditModal(false)}
                    disabled={submitting}
                  >
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-primary btn-sm px-4 fw-semibold" disabled={submitting}>
                    {submitting ? 'Guardando...' : 'Guardar Cambios'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TeacherListPage;
