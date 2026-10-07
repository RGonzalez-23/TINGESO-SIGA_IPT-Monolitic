import { useState, useEffect } from 'react';
import careerService from '../services/career.service';
import studyPlanService from '../services/study-plan.service';
import { useAuth } from '../context/AuthContext';

const CareerManagementPage = () => {
  const { isAdmin } = useAuth();

  // Career state
  const [careers, setCareers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, ACTIVE, INACTIVE

  // Alert & feedback state
  const [alert, setAlert] = useState(null); // { type: 'success' | 'danger', message: '' }

  // Modals state
  const [showCreateCareerModal, setShowCreateCareerModal] = useState(false);
  const [showEditCareerModal, setShowEditCareerModal] = useState(false);
  const [showPlansModal, setShowPlansModal] = useState(false);
  const [showCreatePlanModal, setShowCreatePlanModal] = useState(false);

  // Selected entities
  const [selectedCareer, setSelectedCareer] = useState(null);
  const [careerPlans, setCareerPlans] = useState([]);
  const [loadingPlans, setLoadingPlans] = useState(false);

  // Forms state
  const [careerForm, setCareerForm] = useState({
    code: '',
    name: '',
    description: '',
    durationSemesters: 4,
    initialPlanCode: '',
  });

  const [editCareerForm, setEditCareerForm] = useState({
    name: '',
    description: '',
    isActive: true,
  });

  const [planForm, setPlanForm] = useState({
    code: '',
    isActive: true,
  });

  // Action loading state
  const [submitting, setSubmitting] = useState(false);

  const fetchCareers = async () => {
    setLoading(true);
    try {
      const response = await careerService.getAllCareers();
      setCareers(response.data);
    } catch (err) {
      console.error('Error fetching careers:', err);
      setAlert({
        type: 'danger',
        message: 'No fue posible cargar el catálogo de carreras. Por favor reintente más tarde.',
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchPlans = async (careerCode) => {
    setLoadingPlans(true);
    try {
      const response = await studyPlanService.getPlansByCareer(careerCode);
      setCareerPlans(response.data);
    } catch (err) {
      console.error('Error fetching study plans:', err);
      setAlert({
        type: 'danger',
        message: 'Error al cargar los planes de estudio de la carrera.',
      });
    } finally {
      setLoadingPlans(false);
    }
  };

  useEffect(() => {
    fetchCareers();
  }, []);

  // Filtered careers
  const filteredCareers = careers.filter((c) => {
    const matchesSearch =
      c.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.description && c.description.toLowerCase().includes(searchTerm.toLowerCase()));

    if (statusFilter === 'ACTIVE') return matchesSearch && c.isActive;
    if (statusFilter === 'INACTIVE') return matchesSearch && !c.isActive;
    return matchesSearch;
  });

  // Open Plans Modal
  const handleOpenPlans = (career) => {
    setSelectedCareer(career);
    fetchPlans(career.code);
    setShowPlansModal(true);
  };

  // Open Edit Career Modal
  const handleOpenEdit = (career) => {
    setSelectedCareer(career);
    setEditCareerForm({
      name: career.name,
      description: career.description,
      isActive: career.isActive,
    });
    setShowEditCareerModal(true);
  };

  // Submit Career Registration
  const handleCreateCareer = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setAlert(null);
    try {
      await careerService.registerCareer(careerForm);
      setAlert({
        type: 'success',
        message: `Carrera ${careerForm.code} creada con éxito con su plan inicial ${careerForm.initialPlanCode}.`,
      });
      setShowCreateCareerModal(false);
      setCareerForm({
        code: '',
        name: '',
        description: '',
        durationSemesters: 4,
        initialPlanCode: '',
      });
      fetchCareers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al registrar la carrera.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error de validación.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Submit Career Edit
  const handleUpdateCareer = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setAlert(null);
    try {
      await careerService.updateCareer(selectedCareer.code, editCareerForm);
      setAlert({
        type: 'success',
        message: `Carrera ${selectedCareer.code} actualizada correctamente.`,
      });
      setShowEditCareerModal(false);
      fetchCareers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al actualizar la carrera.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error al guardar cambios.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Delete Career
  const handleDeleteCareer = async (career) => {
    if (
      !window.confirm(
        `¿Confirmas la eliminación física de la carrera ${career.code} - ${career.name}? Esta acción no se puede deshacer.`
      )
    ) {
      return;
    }
    setAlert(null);
    try {
      await careerService.deleteCareer(career.code);
      setAlert({
        type: 'success',
        message: `Carrera ${career.code} eliminada exitosamente.`,
      });
      fetchCareers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'No fue posible eliminar la carrera.';
      setAlert({
        type: 'danger',
        message: typeof msg === 'string' ? msg : 'Error de restricción al eliminar carrera.',
      });
    }
  };

  // Submit Plan Registration
  const handleCreatePlan = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await studyPlanService.createStudyPlan({
        code: planForm.code,
        careerCode: selectedCareer.code,
        isActive: planForm.isActive,
      });
      setAlert({
        type: 'success',
        message: `Plan de estudio ${planForm.code} creado para la carrera ${selectedCareer.code}.`,
      });
      setShowCreatePlanModal(false);
      setPlanForm({ code: '', isActive: true });
      fetchPlans(selectedCareer.code);
      fetchCareers(); // update active plan code in table
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al crear el plan de estudio.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error de validación.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Activate Plan
  const handleActivatePlan = async (plan) => {
    if (
      !window.confirm(
        `¿Deseas activar el plan ${plan.code} como plan vigente para ${selectedCareer.name}? El plan actualmente vigente pasará a no vigente, sin alterar a los alumnos ya matriculados.`
      )
    ) {
      return;
    }
    try {
      await studyPlanService.activateStudyPlan(plan.id);
      setAlert({
        type: 'success',
        message: `Plan ${plan.code} activado como vigente exitosamente.`,
      });
      fetchPlans(selectedCareer.code);
      fetchCareers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al activar el plan de estudio.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error al activar plan.' });
    }
  };

  // Delete Plan
  const handleDeletePlan = async (plan) => {
    if (
      !window.confirm(
        `¿Confirmas la eliminación del plan de estudio ${plan.code}? Esta acción es irreversible.`
      )
    ) {
      return;
    }
    try {
      await studyPlanService.deleteStudyPlan(plan.id);
      setAlert({
        type: 'success',
        message: `Plan ${plan.code} eliminado exitosamente.`,
      });
      fetchPlans(selectedCareer.code);
      fetchCareers();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'No fue posible eliminar el plan.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error al eliminar plan.' });
    }
  };

  return (
    <div className="container py-4">
      {/* Page Header */}
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <div className="d-flex align-items-center gap-2 mb-1">
            <h2 className="fw-bold mb-0 text-dark">Gestión de Carreras y Planes de Estudio</h2>
            <span className="badge bg-primary bg-opacity-10 text-primary rounded-pill px-3 py-1 fw-bold">
              {careers.length} Carreras
            </span>
          </div>
          <p className="text-muted small mb-0">
            Administración de programas técnicos de 4 semestres académicos y planes curriculares.
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={() => setShowCreateCareerModal(true)}
            className="btn btn-primary fw-semibold px-4 py-2 rounded-3 shadow-sm d-flex align-items-center gap-2"
          >
            <i className="bi bi-plus-circle-fill"></i>
            <span>Nueva Carrera</span>
          </button>
        )}
      </div>

      {/* Global Alert Notification */}
      {alert && (
        <div
          className={`alert alert-${alert.type} alert-dismissible fade show rounded-3 shadow-sm border-0 d-flex align-items-center justify-content-between mb-4`}
          role="alert"
        >
          <div className="d-flex align-items-center gap-2">
            <i
              className={`bi ${alert.type === 'success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-triangle-fill text-danger'
                } fs-5`}
            ></i>
            <span>{alert.message}</span>
          </div>
          <button
            type="button"
            className="btn-close"
            onClick={() => setAlert(null)}
            aria-label="Cerrar"
          ></button>
        </div>
      )}

      {/* Filters Toolbar */}
      <div className="card border-0 shadow-sm rounded-4 p-3 mb-4 bg-white">
        <div className="row g-3 align-items-center">
          <div className="col-12 col-md-6">
            <div className="input-group">
              <span className="input-group-text bg-light border-end-0 text-muted">
                <i className="bi bi-search"></i>
              </span>
              <input
                type="text"
                className="form-control bg-light border-start-0"
                placeholder="Buscar por código, nombre o descripción..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              {searchTerm && (
                <button
                  className="btn btn-light border"
                  type="button"
                  onClick={() => setSearchTerm('')}
                >
                  <i className="bi bi-x"></i>
                </button>
              )}
            </div>
          </div>

          <div className="col-12 col-md-4 ms-auto">
            <div className="d-flex align-items-center gap-2">
              <span className="text-muted small fw-semibold">Estado:</span>
              <select
                className="form-select form-select-sm"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">Todas las carreras</option>
                <option value="ACTIVE">Solo Activas (con admisión)</option>
                <option value="INACTIVE">Solo Inactivas (sin admisión)</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Careers Table */}
      <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white">
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light text-secondary small fw-bold">
              <tr>
                <th scope="col" className="ps-4">Código</th>
                <th scope="col">Nombre del Programa</th>
                <th scope="col">Duración</th>
                <th scope="col">Plan Vigente</th>
                <th scope="col" className="text-center">Planes</th>
                <th scope="col" className="text-center">Alumnos</th>
                <th scope="col">Estado</th>
                <th scope="col" className="text-end pe-4">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="8" className="text-center py-5 text-muted">
                    <div className="spinner-border text-primary spinner-border-sm me-2" role="status"></div>
                    Cargando carreras académicas...
                  </td>
                </tr>
              ) : filteredCareers.length === 0 ? (
                <tr>
                  <td colSpan="8" className="text-center py-5 text-muted">
                    <i className="bi bi-journal-x fs-2 d-block mb-2 text-secondary"></i>
                    No se encontraron carreras con los filtros especificados.
                  </td>
                </tr>
              ) : (
                filteredCareers.map((career) => (
                  <tr key={career.code}>
                    <td className="ps-4 fw-bold font-monospace text-primary">
                      {career.code}
                    </td>
                    <td>
                      <div className="fw-semibold text-dark">{career.name}</div>
                      <small className="text-muted text-truncate d-block" style={{ maxWidth: '350px' }}>
                        {career.description}
                      </small>
                    </td>
                    <td>
                      <span className="badge bg-light text-dark border">
                        {career.durationSemesters} semestres (2 años)
                      </span>
                    </td>
                    <td>
                      {career.activeStudyPlanCode ? (
                        <span className="badge bg-success bg-opacity-10 text-success fw-bold px-2 py-1 rounded-pill">
                          <i className="bi bi-check-circle-fill me-1"></i>
                          {career.activeStudyPlanCode}
                        </span>
                      ) : (
                        <span className="badge bg-secondary bg-opacity-10 text-secondary fw-normal">
                          Sin plan activo
                        </span>
                      )}
                    </td>
                    <td className="text-center">
                      <span className="badge bg-info bg-opacity-10 text-info fw-semibold">
                        {career.totalStudyPlans ?? 0}
                      </span>
                    </td>
                    <td className="text-center">
                      <span className="badge bg-secondary bg-opacity-10 text-dark fw-semibold">
                        {career.totalEnrolledStudents ?? 0}
                      </span>
                    </td>
                    <td>
                      {career.isActive ? (
                        <span className="badge bg-success fw-semibold px-2 py-1">
                          ACTIVA
                        </span>
                      ) : (
                        <span className="badge bg-secondary fw-semibold px-2 py-1">
                          INACTIVA
                        </span>
                      )}
                    </td>
                    <td className="text-end pe-4">
                      <div className="btn-group btn-group-sm">
                        <button
                          onClick={() => handleOpenPlans(career)}
                          className="btn btn-outline-primary"
                          title="Gestionar Planes de Estudio"
                        >
                          <i className="bi bi-journal-bookmark me-1"></i>
                          <span>Planes</span>
                        </button>
                        {isAdmin && (
                          <>
                            <button
                              onClick={() => handleOpenEdit(career)}
                              className="btn btn-outline-secondary"
                              title="Editar Carrera"
                            >
                              <i className="bi bi-pencil"></i>
                            </button>
                            <button
                              onClick={() => handleDeleteCareer(career)}
                              className="btn btn-outline-danger"
                              title="Eliminar Carrera"
                            >
                              <i className="bi bi-trash"></i>
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ================= MODAL: GESTIÓN DE PLANES DE ESTUDIO ================= */}
      {showPlansModal && selectedCareer && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                <div>
                  <h5 className="modal-title fw-bold text-dark mb-0">
                    Planes de Estudio &bull; {selectedCareer.name}
                  </h5>
                  <small className="text-muted">
                    Código de Carrera: <code>{selectedCareer.code}</code> &bull; Duración: {selectedCareer.durationSemesters} semestres
                  </small>
                </div>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowPlansModal(false)}
                  aria-label="Cerrar"
                ></button>
              </div>

              <div className="modal-body p-4">
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <span className="small text-left">
                    Regla: Solo <strong>un plan de estudios puede estar vigente a la vez</strong>. Los estudiantes ya matriculados conservan su plan histórico.
                  </span>

                </div>

                {loadingPlans ? (
                  <div className="text-center py-4 text-muted">
                    <div className="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                    Cargando planes curriculares...
                  </div>
                ) : careerPlans.length === 0 ? (
                  <div className="alert alert-warning mb-0 small">
                    Esta carrera no tiene planes de estudio registrados.
                  </div>
                ) : (
                  <div className="table-responsive border rounded-3 overflow-hidden">
                    <table className="table table-hover align-middle mb-0">
                      <thead className="table-light small">
                        <tr>
                          <th>Código del Plan</th>
                          <th>Estado</th>
                          <th className="text-center">Alumnos Matriculados</th>
                          <th className="text-end">Acciones</th>
                        </tr>
                      </thead>
                      <tbody>
                        {careerPlans.map((plan) => (
                          <tr key={plan.id}>
                            <td className="fw-bold font-monospace text-dark">
                              {plan.code}
                            </td>
                            <td>
                              {plan.isActive ? (
                                <span className="badge bg-success fw-bold px-2 py-1 rounded-pill">
                                  <i className="bi bi-star-fill me-1"></i>
                                  VIGENTE
                                </span>
                              ) : (
                                <span className="badge bg-secondary bg-opacity-75 fw-normal px-2 py-1 rounded-pill">
                                  NO VIGENTE
                                </span>
                              )}
                            </td>
                            <td className="text-center">
                              <span className="badge bg-light text-dark border">
                                {plan.enrolledStudentsCount ?? 0} estudiantes
                              </span>
                            </td>
                            <td className="text-end">
                              <div className="btn-group btn-group-sm">
                                {isAdmin && !plan.isActive && (
                                  <button
                                    onClick={() => handleActivatePlan(plan)}
                                    className="btn btn-outline-success"
                                    title="Activar como plan vigente"
                                  >
                                    <i className="bi bi-check2-circle me-1"></i>
                                    <span>Hacer Vigente</span>
                                  </button>
                                )}
                                {isAdmin && (
                                  <button
                                    onClick={() => handleDeletePlan(plan)}
                                    className="btn btn-outline-danger"
                                    disabled={plan.enrolledStudentsCount > 0}
                                    title={
                                      plan.enrolledStudentsCount > 0
                                        ? 'No se puede eliminar: tiene alumnos asociados'
                                        : 'Eliminar plan'
                                    }
                                  >
                                    <i className="bi bi-trash"></i>
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                {isAdmin && (
                  <div className="modal-footer justify-content-center">
                    <button
                      onClick={() => setShowCreatePlanModal(true)}
                      className="btn btn-sm btn-primary fw-semibold d-flex align-items-center gap-1"
                    >
                      <i className="bi bi-plus-lg"></i>
                      <span>Nuevo Plan</span>
                    </button>
                  </div>
                )}
              </div>
              <div className="modal-footer border-0 bg-light rounded-bottom-4 py-2">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm px-4"
                  onClick={() => setShowPlansModal(false)}
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        </div >
      )
      }

      {/* ================= MODAL: CREAR PLAN DE ESTUDIO ================= */}
      {
        showCreatePlanModal && selectedCareer && (
          <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 1060 }}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content border-0 shadow-lg rounded-4">
                <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                  <h5 className="modal-title fw-bold text-dark mb-0">
                    Registrar Plan de Estudio para {selectedCareer.code}
                  </h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setShowCreatePlanModal(false)}
                    aria-label="Cerrar"
                  ></button>
                </div>
                <form onSubmit={handleCreatePlan}>
                  <div className="modal-body p-4">
                    <div className="mb-3">
                      <label className="form-label fw-semibold text-secondary small">
                        Código / Versión del Plan <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Ej: 2026.1 o 2026.2"
                        value={planForm.code}
                        onChange={(e) => setPlanForm({ ...planForm, code: e.target.value })}
                        required
                      />
                      <div className="form-text small">
                        Formato usual en el IPT: Año.Semestre (p.ej. 2026.1).
                      </div>
                    </div>

                    <div className="form-check form-switch mb-3">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        role="switch"
                        id="planActiveSwitch"
                        checked={planForm.isActive}
                        onChange={(e) => setPlanForm({ ...planForm, isActive: e.target.checked })}
                      />
                      <label className="form-check-label fw-semibold text-dark small" htmlFor="planActiveSwitch">
                        Establecer como Plan Vigente de la carrera
                      </label>
                      <div className="form-text small">
                        Al activarlo, el plan anterior pasará automáticamente a ser histórico, sin alterar a los alumnos ya matriculados.
                      </div>
                    </div>
                  </div>

                  <div className="modal-footer border-0 bg-light rounded-bottom-4 py-2">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => setShowCreatePlanModal(false)}
                      disabled={submitting}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary btn-sm px-4 fw-semibold"
                      disabled={submitting}
                    >
                      {submitting ? 'Guardando...' : 'Crear Plan'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )
      }

      {/* ================= MODAL: NUEVA CARRERA ================= */}
      {
        showCreateCareerModal && (
          <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content border-0 shadow-lg rounded-4">
                <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                  <h5 className="modal-title fw-bold text-dark mb-0">
                    <i className="bi bi-mortarboard-fill text-primary me-2"></i>
                    Registrar Nueva Carrera Técnica
                  </h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setShowCreateCareerModal(false)}
                    aria-label="Cerrar"
                  ></button>
                </div>
                <form onSubmit={handleCreateCareer}>
                  <div className="modal-body p-4">
                    <div className="row g-3">
                      <div className="col-md-5">
                        <label className="form-label fw-semibold text-secondary small">
                          Código Único <span className="text-danger">*</span>
                        </label>
                        <input
                          type="text"
                          className="form-control font-monospace"
                          placeholder="Ej: 10453"
                          maxLength="20"
                          value={careerForm.code}
                          onChange={(e) => setCareerForm({ ...careerForm, code: e.target.value })}
                          required
                        />
                      </div>

                      <div className="col-md-7">
                        <label className="form-label fw-semibold text-secondary small">
                          Plan de Estudio Inicial <span className="text-danger">*</span>
                        </label>
                        <input
                          type="text"
                          className="form-control font-monospace"
                          placeholder="Ej: 2026.1"
                          maxLength="50"
                          value={careerForm.initialPlanCode}
                          onChange={(e) => setCareerForm({ ...careerForm, initialPlanCode: e.target.value })}
                          required
                        />
                      </div>

                      <div className="col-12">
                        <label className="form-label fw-semibold text-secondary small">
                          Nombre Oficial de la Carrera <span className="text-danger">*</span>
                        </label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="Ej: Técnico en Ciberseguridad"
                          maxLength="150"
                          value={careerForm.name}
                          onChange={(e) => setCareerForm({ ...careerForm, name: e.target.value })}
                          required
                        />
                      </div>

                      <div className="col-12">
                        <label className="form-label fw-semibold text-secondary small">
                          Descripción del Perfil <span className="text-danger">*</span>
                        </label>
                        <textarea
                          className="form-control"
                          rows="3"
                          placeholder="Breve reseña del perfil de egreso y objetivos del programa formativo..."
                          maxLength="500"
                          value={careerForm.description}
                          onChange={(e) => setCareerForm({ ...careerForm, description: e.target.value })}
                          required
                        ></textarea>
                      </div>

                      <div className="col-12">
                        <div className="p-3 bg-light rounded-3 border">
                          <div className="d-flex align-items-center gap-2">
                            <i className="bi bi-clock-history text-primary"></i>
                            <span className="small fw-semibold text-dark">
                              Duración del Programa: 4 semestres académicos (2 años)
                            </span>
                          </div>
                          <p className="text-muted small mb-0 mt-1">
                            Estándar curricular fijado para todas las carreras del Instituto Profesional de Tecnología (IPT).
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="modal-footer border-0 bg-light rounded-bottom-4 py-2">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => setShowCreateCareerModal(false)}
                      disabled={submitting}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary btn-sm px-4 fw-semibold"
                      disabled={submitting}
                    >
                      {submitting ? 'Registrando...' : 'Registrar Carrera'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )
      }

      {/* ================= MODAL: EDITAR CARRERA ================= */}
      {
        showEditCareerModal && selectedCareer && (
          <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content border-0 shadow-lg rounded-4">
                <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                  <h5 className="modal-title fw-bold text-dark mb-0">
                    Editar Carrera &bull; {selectedCareer.code}
                  </h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setShowEditCareerModal(false)}
                    aria-label="Cerrar"
                  ></button>
                </div>
                <form onSubmit={handleUpdateCareer}>
                  <div className="modal-body p-4">
                    <div className="mb-3">
                      <label className="form-label fw-semibold text-secondary small">
                        Nombre de la Carrera <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={editCareerForm.name}
                        maxLength="150"
                        onChange={(e) => setEditCareerForm({ ...editCareerForm, name: e.target.value })}
                        required
                      />
                    </div>

                    <div className="mb-3">
                      <label className="form-label fw-semibold text-secondary small">
                        Descripción <span className="text-danger">*</span>
                      </label>
                      <textarea
                        className="form-control"
                        rows="3"
                        value={editCareerForm.description}
                        maxLength="500"
                        onChange={(e) => setEditCareerForm({ ...editCareerForm, description: e.target.value })}
                        required
                      ></textarea>
                    </div>

                    <div className="form-check form-switch mb-2">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        role="switch"
                        id="careerActiveSwitch"
                        checked={editCareerForm.isActive}
                        onChange={(e) => setEditCareerForm({ ...editCareerForm, isActive: e.target.checked })}
                      />
                      <label className="form-check-label fw-semibold text-dark small" htmlFor="careerActiveSwitch">
                        Carrera Activa (Admisión Habilitada)
                      </label>
                    </div>
                    {!editCareerForm.isActive && (
                      <div className="alert alert-warning py-2 px-3 small mb-0">
                        <i className="bi bi-exclamation-triangle-fill me-1"></i>
                        Una carrera inactiva no podrá admitir nuevos estudiantes durante el proceso de matrícula.
                      </div>
                    )}
                  </div>

                  <div className="modal-footer border-0 bg-light rounded-bottom-4 py-2">
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      onClick={() => setShowEditCareerModal(false)}
                      disabled={submitting}
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary btn-sm px-4 fw-semibold"
                      disabled={submitting}
                    >
                      {submitting ? 'Guardando...' : 'Guardar Cambios'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        )
      }
    </div >
  );
};

export default CareerManagementPage;
