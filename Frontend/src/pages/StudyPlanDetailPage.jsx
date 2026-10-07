import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import studyPlanService from '../services/study-plan.service';
import studentService from '../services/student.service';
import courseService from '../services/course.service';
import { useAuth } from '../context/AuthContext';

/**
 * StudyPlanDetailPage provides dedicated viewing and management of an individual study plan,
 * its curriculum across 4 semesters, subjects, TEL hours, SCT credits, and prerequisites.
 * - ADMIN: Full management (create, edit, delete subjects, activate plan).
 * - TEACHER: Read-only access to any plan.
 * - STUDENT: Read-only access restricted strictly to their own assigned plan.
 */
const StudyPlanDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAdmin, isTeacher, isStudent, currentUserRun } = useAuth();

  // Plan & courses state
  const [plan, setPlan] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [accessDenied, setAccessDenied] = useState(false);
  const [myPlanId, setMyPlanId] = useState(null);

  // Filter state (0: All, 1: Sem 1, 2: Sem 2, 3: Sem 3, 4: Sem 4)
  const [activeSemesterFilter, setActiveSemesterFilter] = useState(0);

  // Alerts
  const [alert, setAlert] = useState(null); // { type: 'success' | 'danger', message: '' }

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Course forms
  const [courseForm, setCourseForm] = useState({
    code: '',
    name: '',
    semester: 1,
    theoryHours: 2,
    exerciseHours: 2,
    laboratoryHours: 0,
    sctCredits: 5,
    prerequisiteCodes: [],
  });

  const [editForm, setEditForm] = useState({
    name: '',
    semester: 1,
    theoryHours: 2,
    exerciseHours: 2,
    laboratoryHours: 0,
    sctCredits: 5,
    prerequisiteCodes: [],
  });

  const loadPlanAndCourses = useCallback(async () => {
    setLoading(true);
    setError(null);
    setAccessDenied(false);

    try {
      // 1. If STUDENT, verify they can only view their own study plan
      if (isStudent && currentUserRun) {
        const profileRes = await studentService.getByRun(currentUserRun);
        const studentPlanId = profileRes.data?.studyPlanId;
        setMyPlanId(studentPlanId);

        if (!studentPlanId || String(studentPlanId) !== String(id)) {
          setAccessDenied(true);
          setLoading(false);
          return;
        }
      }

      // 2. Fetch plan details and courses
      const [planRes, coursesRes] = await Promise.all([
        studyPlanService.getStudyPlanById(id),
        courseService.getCoursesByStudyPlan(id),
      ]);

      setPlan(planRes.data);
      setCourses(coursesRes.data);
    } catch (err) {
      console.error('Error loading study plan or courses:', err);
      setError('No fue posible cargar la información del plan de estudios solicitado.');
    } finally {
      setLoading(false);
    }
  }, [id, isStudent, currentUserRun]);

  useEffect(() => {
    loadPlanAndCourses();
  }, [loadPlanAndCourses]);

  // Activate plan
  const handleActivatePlan = async () => {
    if (
      !window.confirm(
        `¿Confirmas activar el plan ${plan.code} como plan vigente para la carrera ${plan.careerName}?`
      )
    ) {
      return;
    }

    try {
      const res = await studyPlanService.activateStudyPlan(plan.id);
      setPlan(res.data);
      setAlert({ type: 'success', message: '¡El plan ha sido activado como vigente exitosamente!' });
    } catch (err) {
      console.error('Error activating plan:', err);
      setAlert({ type: 'danger', message: 'Error al activar el plan de estudios.' });
    }
  };

  // Open Create Modal
  const handleOpenCreate = (targetSemester = 1) => {
    setCourseForm({
      code: '',
      name: '',
      semester: targetSemester >= 1 && targetSemester <= 4 ? targetSemester : 1,
      theoryHours: 2,
      exerciseHours: 2,
      laboratoryHours: 0,
      sctCredits: 5,
      prerequisiteCodes: [],
    });
    setShowCreateModal(true);
  };

  // Open Edit Modal
  const handleOpenEdit = (course) => {
    setSelectedCourse(course);
    setEditForm({
      name: course.name,
      semester: course.semester,
      theoryHours: course.theoryHours,
      exerciseHours: course.exerciseHours,
      laboratoryHours: course.laboratoryHours,
      sctCredits: course.sctCredits,
      prerequisiteCodes: course.prerequisites ? course.prerequisites.map((p) => p.code) : [],
    });
    setShowEditModal(true);
  };

  // Submit Create Course
  const handleCreateCourse = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setAlert(null);

    const totalHours =
      Number(courseForm.theoryHours) + Number(courseForm.exerciseHours) + Number(courseForm.laboratoryHours);

    if (totalHours >= 8) {
      setAlert({
        type: 'danger',
        message: `La suma de horas TEL debe ser menor a 8 horas semanales (suma actual: ${totalHours} hrs).`,
      });
      setSubmitting(false);
      return;
    }

    if (totalHours <= 0) {
      setAlert({
        type: 'danger',
        message: 'La suma de horas TEL debe ser mayor a 0.',
      });
      setSubmitting(false);
      return;
    }

    try {
      await courseService.createCourse({
        code: courseForm.code.trim().toUpperCase(),
        name: courseForm.name.trim(),
        studyPlanId: Number(id),
        semester: Number(courseForm.semester),
        theoryHours: Number(courseForm.theoryHours),
        exerciseHours: Number(courseForm.exerciseHours),
        laboratoryHours: Number(courseForm.laboratoryHours),
        sctCredits: Number(courseForm.sctCredits),
        prerequisiteCodes: courseForm.prerequisiteCodes,
      });

      setAlert({
        type: 'success',
        message: `Asignatura ${courseForm.code} creada con éxito en el Semestre ${courseForm.semester}.`,
      });
      setShowCreateModal(false);
      loadPlanAndCourses();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al registrar la asignatura.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error de validación.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Submit Update Course
  const handleUpdateCourse = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setAlert(null);

    const totalHours =
      Number(editForm.theoryHours) + Number(editForm.exerciseHours) + Number(editForm.laboratoryHours);

    if (totalHours >= 8) {
      setAlert({
        type: 'danger',
        message: `La suma de horas TEL debe ser menor a 8 horas semanales (suma actual: ${totalHours} hrs).`,
      });
      setSubmitting(false);
      return;
    }

    try {
      await courseService.updateCourse(selectedCourse.id, {
        name: editForm.name.trim(),
        semester: Number(editForm.semester),
        theoryHours: Number(editForm.theoryHours),
        exerciseHours: Number(editForm.exerciseHours),
        laboratoryHours: Number(editForm.laboratoryHours),
        sctCredits: Number(editForm.sctCredits),
        prerequisiteCodes: editForm.prerequisiteCodes,
      });

      setAlert({
        type: 'success',
        message: `Asignatura ${selectedCourse.code} actualizada correctamente.`,
      });
      setShowEditModal(false);
      loadPlanAndCourses();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Error al actualizar la asignatura.';
      setAlert({ type: 'danger', message: typeof msg === 'string' ? msg : 'Error de validación.' });
    } finally {
      setSubmitting(false);
    }
  };

  // Delete Course
  const handleDeleteCourse = async (course) => {
    if (
      !window.confirm(
        `¿Confirmas la eliminación de la asignatura ${course.code} - ${course.name}? Esta acción no se puede deshacer.`
      )
    ) {
      return;
    }

    setAlert(null);
    try {
      await courseService.deleteCourse(course.id);
      setAlert({
        type: 'success',
        message: `Asignatura ${course.code} eliminada exitosamente.`,
      });
      loadPlanAndCourses();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'No fue posible eliminar la asignatura.';
      setAlert({
        type: 'danger',
        message: typeof msg === 'string' ? msg : 'Error al eliminar asignatura.',
      });
    }
  };

  // Helper to toggle a prerequisite in form
  const handleTogglePrerequisite = (code, isEditing = false) => {
    if (isEditing) {
      const current = editForm.prerequisiteCodes || [];
      if (current.includes(code)) {
        setEditForm({ ...editForm, prerequisiteCodes: current.filter((c) => c !== code) });
      } else {
        if (current.length >= 3) {
          alert('Una asignatura puede tener como máximo 3 prerrequisitos.');
          return;
        }
        setEditForm({ ...editForm, prerequisiteCodes: [...current, code] });
      }
    } else {
      const current = courseForm.prerequisiteCodes || [];
      if (current.includes(code)) {
        setCourseForm({ ...courseForm, prerequisiteCodes: current.filter((c) => c !== code) });
      } else {
        if (current.length >= 3) {
          alert('Una asignatura puede tener como máximo 3 prerrequisitos.');
          return;
        }
        setCourseForm({ ...courseForm, prerequisiteCodes: [...current, code] });
      }
    }
  };

  // Available prerequisite candidates for a selected semester (must be from strictly earlier semester)
  const getEligiblePrerequisites = (targetSemester, currentCourseCode = null) => {
    return courses.filter(
      (c) => c.semester < targetSemester && (!currentCourseCode || c.code !== currentCourseCode)
    );
  };

  // Filtered courses based on semester selector
  const displayedCourses =
    activeSemesterFilter === 0 ? courses : courses.filter((c) => c.semester === activeSemesterFilter);

  // Group courses by semester
  const coursesBySemester = {
    1: courses.filter((c) => c.semester === 1),
    2: courses.filter((c) => c.semester === 2),
    3: courses.filter((c) => c.semester === 3),
    4: courses.filter((c) => c.semester === 4),
  };

  // Case 1: Loading
  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status"></div>
        <p className="mt-3 text-muted">Cargando malla curricular y catálogo de asignaturas...</p>
      </div>
    );
  }

  // Case 2: Access Denied for Student trying to view someone else's plan
  if (accessDenied) {
    return (
      <div className="container py-5 text-center">
        <div className="card shadow-sm border-0 p-5 mx-auto rounded-4" style={{ maxWidth: '580px' }}>
          <i className="bi bi-shield-slash text-danger display-3 mb-3"></i>
          <h3 className="fw-bold text-dark mb-2">Acceso Restringido</h3>
          <p className="text-muted small mb-4">
            Como estudiante, únicamente tienes autorización para consultar la malla curricular correspondiente a tu propio plan de estudios asignado.
          </p>
          <div className="d-flex justify-content-center gap-3">
            {myPlanId && (
              <button
                onClick={() => navigate(`/study-plans/${myPlanId}`)}
                className="btn btn-success fw-semibold px-4 py-2 rounded-3 text-white"
              >
                <i className="bi bi-mortarboard me-2"></i>
                Ir a Mi Malla Curricular
              </button>
            )}
            <button
              onClick={() => navigate('/')}
              className="btn btn-outline-secondary fw-semibold px-4 py-2 rounded-3"
            >
              <i className="bi bi-house me-2"></i>
              Volver al Inicio
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Case 3: Error loading
  if (error || !plan) {
    return (
      <div className="container py-5 text-center">
        <div className="card shadow-sm border-0 p-5 mx-auto rounded-4" style={{ maxWidth: '550px' }}>
          <i className="bi bi-exclamation-octagon text-warning display-4 mb-3"></i>
          <h4 className="fw-bold text-dark mb-2">Plan no disponible</h4>
          <p className="text-muted small mb-4">{error || 'El plan solicitado no fue encontrado.'}</p>
          <button
            onClick={() => navigate(isStudent ? '/' : '/careers')}
            className="btn btn-primary fw-semibold px-4 py-2 rounded-3"
          >
            {isStudent ? 'Volver a Mi Inicio' : 'Volver a Carreras'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-4">
      {/* Top Navigation & Breadcrumb */}
      <div className="d-flex align-items-center justify-content-between mb-4">
        <button
          onClick={() => navigate(isStudent ? '/' : '/careers')}
          className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1 rounded-3"
        >
          <i className="bi bi-arrow-left"></i>
          <span>{isStudent ? 'Volver al Inicio' : 'Volver a Carreras y Planes'}</span>
        </button>

        <span className="badge bg-light text-dark border px-3 py-2 small">
          {isStudent
            ? 'Vista de Estudiante (Solo Lectura)'
            : isTeacher
              ? 'Vista Docente (Consulta)'
              : 'Panel de Administración (Control Total)'}
        </span>
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
          <button type="button" className="btn-close" onClick={() => setAlert(null)} aria-label="Cerrar"></button>
        </div>
      )}

      {/* Plan Header Banner */}
      <div
        className="card border-0 shadow-sm rounded-4 p-4 p-md-5 mb-4 text-white overflow-hidden position-relative"
        style={{
          background: isStudent
            ? 'linear-gradient(135deg, #065f46 0%, #059669 60%, #10b981 100%)'
            : 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%)',
        }}
      >
        <div className="row align-items-center g-3">
          <div className="col-lg-8">
            <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
              <span className="badge bg-white bg-opacity-25 text-white px-3 py-1 rounded-pill fw-semibold">
                Carrera: {plan.careerCode}
              </span>
              {plan.isActive ? (
                <span className="badge bg-success text-white px-3 py-1 rounded-pill fw-bold">
                  <i className="bi bi-star-fill me-1"></i>
                  PLAN VIGENTE
                </span>
              ) : (
                <span className="badge bg-secondary bg-opacity-50 text-white px-3 py-1 rounded-pill fw-normal">
                  PLAN NO VIGENTE
                </span>
              )}
            </div>

            <h1 className="display-6 fw-bold mb-1">
              {isStudent ? 'Mi Malla Curricular' : 'Plan de Estudios'} &bull; Versión {plan.code}
            </h1>
            <h4 className="text-white-50 mb-0">{plan.careerName}</h4>
          </div>

          <div className="col-lg-4 text-lg-end">
            <div className="d-inline-flex flex-column align-items-lg-end gap-2 bg-white bg-opacity-10 p-3 rounded-3 backdrop-blur">
              <div className="small text-white-50">
                <i className="bi bi-calendar3 me-1"></i> Duración: <strong>4 Semestres</strong>
              </div>
              <div className="small text-white-50">
                <i className="bi bi-journal-check me-1"></i> Total Asignaturas: <strong>{courses.length}</strong>
              </div>
              {isAdmin && (
                <div className="small text-white-50">
                  <i className="bi bi-people me-1"></i> Alumnos en este Plan: <strong>{plan.enrolledStudentsCount ?? 0}</strong>
                </div>
              )}
              {isAdmin && !plan.isActive && (
                <button onClick={handleActivatePlan} className="btn btn-warning btn-sm fw-bold text-dark mt-2 w-100">
                  <i className="bi bi-check2-circle me-1"></i>
                  Hacer Plan Vigente
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Curriculum Toolbar & Semester Navigation */}
      <div className="card border-0 shadow-sm rounded-4 p-3 mb-4 bg-white">
        <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <span className="text-muted small fw-semibold me-2">Filtrar Semestre:</span>
            <button
              className={`btn btn-sm ${activeSemesterFilter === 0 ? 'btn-primary fw-bold' : 'btn-outline-secondary'}`}
              onClick={() => setActiveSemesterFilter(0)}
            >
              Todos ({courses.length})
            </button>
            {[1, 2, 3, 4].map((sem) => (
              <button
                key={sem}
                className={`btn btn-sm ${activeSemesterFilter === sem ? 'btn-primary fw-bold' : 'btn-outline-secondary'}`}
                onClick={() => setActiveSemesterFilter(sem)}
              >
                Semestre {sem} ({coursesBySemester[sem].length})
              </button>
            ))}
          </div>

          {isAdmin && (
            <button
              onClick={() => handleOpenCreate(activeSemesterFilter === 0 ? 1 : activeSemesterFilter)}
              className="btn btn-primary btn-sm fw-semibold px-3 py-2 rounded-3 shadow-sm d-flex align-items-center gap-2"
            >
              <i className="bi bi-plus-circle-fill"></i>
              <span>Nueva Asignatura</span>
            </button>
          )}
        </div>
      </div>

      {/* Curriculum View (Semester Blocks or Flat List) */}
      {activeSemesterFilter === 0 ? (
        // Grid of 4 Semesters
        <div className="row g-4">
          {[1, 2, 3, 4].map((sem) => (
            <div key={sem} className="col-12 col-xl-6">
              <div className="card h-100 border-0 shadow-sm rounded-4 overflow-hidden bg-white">
                <div className="card-header bg-light border-0 py-3 px-4 d-flex justify-content-between align-items-center">
                  <div>
                    <h5 className="fw-bold mb-0 text-dark">
                      <span className="badge bg-primary me-2">{sem}°</span>
                      Semestre {sem}
                    </h5>
                    <small className="text-muted">
                      {sem <= 2 ? 'Año 1 (Ciclo Inicial)' : 'Año 2 (Ciclo Avanzado/Egreso)'} &bull;{' '}
                      {coursesBySemester[sem].length} Asignaturas
                    </small>
                  </div>
                  {isAdmin && (
                    <button
                      onClick={() => handleOpenCreate(sem)}
                      className="btn btn-outline-primary btn-sm py-1 px-2"
                      title={`Agregar asignatura al Semestre ${sem}`}
                    >
                      <i className="bi bi-plus-lg me-1"></i>
                      <span>Agregar</span>
                    </button>
                  )}
                </div>

                <div className="card-body p-3">
                  {coursesBySemester[sem].length === 0 ? (
                    <div className="p-4 text-center text-muted border border-dashed rounded-3">
                      <i className="bi bi-journal-x fs-2 text-secondary d-block mb-1"></i>
                      <span className="small">No hay asignaturas registradas en el Semestre {sem}.</span>
                      {isAdmin && sem === 4 && (
                        <div className="mt-2">
                          <button
                            onClick={() => handleOpenCreate(4)}
                            className="btn btn-outline-primary btn-sm"
                          >
                            + Agregar Asignatura de 4° Semestre
                          </button>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="d-flex flex-column gap-2">
                      {coursesBySemester[sem].map((course) => (
                        <CourseCard
                          key={course.id}
                          course={course}
                          isAdmin={isAdmin}
                          onEdit={handleOpenEdit}
                          onDelete={handleDeleteCourse}
                        />
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        // Single Filtered Semester View
        <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white">
          <div className="card-header bg-light border-0 py-3 px-4 d-flex justify-content-between align-items-center">
            <h5 className="fw-bold mb-0 text-dark">
              Asignaturas de Semestre {activeSemesterFilter} ({displayedCourses.length})
            </h5>
            {isAdmin && (
              <button
                onClick={() => handleOpenCreate(activeSemesterFilter)}
                className="btn btn-primary btn-sm fw-semibold"
              >
                <i className="bi bi-plus-lg me-1"></i>
                Nueva Asignatura
              </button>
            )}
          </div>
          <div className="card-body p-4">
            {displayedCourses.length === 0 ? (
              <div className="p-5 text-center text-muted border border-dashed rounded-3">
                <i className="bi bi-journal-x fs-1 text-secondary d-block mb-2"></i>
                <h6>No hay asignaturas en el Semestre {activeSemesterFilter}</h6>
                <p className="small text-muted mb-3">
                  Puedes registrar las asignaturas correspondientes a este semestre.
                </p>
                {isAdmin && (
                  <button
                    onClick={() => handleOpenCreate(activeSemesterFilter)}
                    className="btn btn-primary btn-sm"
                  >
                    + Agregar Asignatura
                  </button>
                )}
              </div>
            ) : (
              <div className="row g-3">
                {displayedCourses.map((course) => (
                  <div key={course.id} className="col-12 col-md-6">
                    <CourseCard
                      course={course}
                      isAdmin={isAdmin}
                      onEdit={handleOpenEdit}
                      onDelete={handleDeleteCourse}
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ================= MODAL: NUEVA ASIGNATURA ================= */}
      {showCreateModal && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                <h5 className="modal-title fw-bold text-dark mb-0">
                  <i className="bi bi-journal-plus text-primary me-2"></i>
                  Registrar Asignatura &bull; Plan {plan.code}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowCreateModal(false)}
                  aria-label="Cerrar"
                ></button>
              </div>
              <form onSubmit={handleCreateCourse}>
                <div className="modal-body p-4">
                  <div className="row g-3">
                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Código Único <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control font-monospace"
                        placeholder="Ej: TAP401"
                        maxLength="30"
                        value={courseForm.code}
                        onChange={(e) => setCourseForm({ ...courseForm, code: e.target.value })}
                        required
                      />
                      <div className="form-text small">Único dentro del plan de estudios.</div>
                    </div>

                    <div className="col-md-8">
                      <label className="form-label fw-semibold text-secondary small">
                        Nombre de la Asignatura <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="Ej: Arquitectura y Desarrollo de Software"
                        maxLength="150"
                        value={courseForm.name}
                        onChange={(e) => setCourseForm({ ...courseForm, name: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Semestre Académico <span className="text-danger">*</span>
                      </label>
                      <select
                        className="form-select"
                        value={courseForm.semester}
                        onChange={(e) =>
                          setCourseForm({
                            ...courseForm,
                            semester: Number(e.target.value),
                            prerequisiteCodes: [], // Reset prerequisites when semester changes
                          })
                        }
                        required
                      >
                        <option value={1}>1° Semestre (Año 1)</option>
                        <option value={2}>2° Semestre (Año 1)</option>
                        <option value={3}>3° Semestre (Año 2)</option>
                        <option value={4}>4° Semestre (Año 2)</option>
                      </select>
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Créditos Académicos (SCT) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        min="1"
                        max="7"
                        value={courseForm.sctCredits}
                        onChange={(e) => setCourseForm({ ...courseForm, sctCredits: e.target.value })}
                        required
                      />
                      <div className="form-text small">Rango permitido: 1 a 7 SCT.</div>
                    </div>

                    {/* Horas TEL */}
                    <div className="col-12">
                      <div className="p-3 bg-light rounded-3 border">
                        <label className="form-label fw-bold text-dark small mb-2">
                          Horas Semanales de Trabajo (TEL): Teoría, Ejercicios y Laboratorio
                        </label>
                        <div className="row g-2 align-items-center">
                          <div className="col-md-3">
                            <label className="small text-muted">Teoría (T):</label>
                            <input
                              type="number"
                              className="form-control form-control-sm"
                              min="0"
                              max="6"
                              value={courseForm.theoryHours}
                              onChange={(e) => setCourseForm({ ...courseForm, theoryHours: e.target.value })}
                              required
                            />
                          </div>
                          <div className="col-md-3">
                            <label className="small text-muted">Ejercicios (E):</label>
                            <input
                              type="number"
                              className="form-control form-control-sm"
                              min="0"
                              max="6"
                              value={courseForm.exerciseHours}
                              onChange={(e) => setCourseForm({ ...courseForm, exerciseHours: e.target.value })}
                              required
                            />
                          </div>
                          <div className="col-md-3">
                            <label className="small text-muted">Laboratorio (L):</label>
                            <input
                              type="number"
                              className="form-control form-control-sm"
                              min="0"
                              max="6"
                              value={courseForm.laboratoryHours}
                              onChange={(e) => setCourseForm({ ...courseForm, laboratoryHours: e.target.value })}
                              required
                            />
                          </div>
                          <div className="col-md-3">
                            <label className="small text-muted">Formato TEL & Total:</label>
                            <div className="fw-bold font-monospace text-primary">
                              {courseForm.theoryHours}-{courseForm.exerciseHours}-{courseForm.laboratoryHours} (
                              {Number(courseForm.theoryHours) +
                                Number(courseForm.exerciseHours) +
                                Number(courseForm.laboratoryHours)}{' '}
                              hrs)
                            </div>
                          </div>
                        </div>
                        <div className="small text-muted mt-2">
                          Regla: Ninguna hora puede ser negativa y la suma total debe ser <strong>menor a 8 horas semanales</strong>.
                        </div>
                      </div>
                    </div>

                    {/* Prerrequisitos */}
                    <div className="col-12">
                      <label className="form-label fw-semibold text-secondary small d-flex justify-content-between">
                        <span>
                          Prerrequisitos Académicos (Máximo 3){' '}
                          <span className="badge bg-light text-dark border ms-1">
                            {courseForm.prerequisiteCodes.length} / 3 seleccionados
                          </span>
                        </span>
                        <span className="text-muted small">Solo de semestres anteriores ({courseForm.semester - 1}° hacia atrás)</span>
                      </label>

                      {courseForm.semester === 1 ? (
                        <div className="alert alert-secondary py-2 px-3 small mb-0">
                          Las asignaturas de 1° Semestre no tienen prerrequisitos.
                        </div>
                      ) : (
                        <div className="border rounded-3 p-3 bg-white" style={{ maxHeight: '180px', overflowY: 'auto' }}>
                          {getEligiblePrerequisites(courseForm.semester).length === 0 ? (
                            <span className="text-muted small">No hay asignaturas disponibles en semestres anteriores.</span>
                          ) : (
                            <div className="row g-2">
                              {getEligiblePrerequisites(courseForm.semester).map((candidate) => (
                                <div key={candidate.id} className="col-md-6">
                                  <div
                                    className={`p-2 rounded-2 border d-flex align-items-center gap-2 cursor-pointer ${courseForm.prerequisiteCodes.includes(candidate.code)
                                        ? 'border-primary bg-primary bg-opacity-10'
                                        : 'bg-light'
                                      }`}
                                    onClick={() => handleTogglePrerequisite(candidate.code, false)}
                                    style={{ cursor: 'pointer' }}
                                  >
                                    <input
                                      type="checkbox"
                                      className="form-check-input mt-0"
                                      checked={courseForm.prerequisiteCodes.includes(candidate.code)}
                                      onChange={() => { }} // handled by parent div
                                    />
                                    <div className="small text-truncate">
                                      <strong className="font-monospace text-primary">{candidate.code}</strong> &bull;{' '}
                                      {candidate.name} <span className="text-muted">({candidate.semester}° Sem)</span>
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
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
                    {submitting ? 'Guardando...' : 'Crear Asignatura'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: EDITAR ASIGNATURA ================= */}
      {showEditModal && selectedCourse && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-lg modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <div className="modal-header border-0 bg-light rounded-top-4 py-3">
                <h5 className="modal-title fw-bold text-dark mb-0">
                  Editar Asignatura &bull; {selectedCourse.code}
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowEditModal(false)}
                  aria-label="Cerrar"
                ></button>
              </div>
              <form onSubmit={handleUpdateCourse}>
                <div className="modal-body p-4">
                  <div className="row g-3">
                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">Código</label>
                      <input
                        type="text"
                        className="form-control font-monospace bg-light"
                        value={selectedCourse.code}
                        disabled
                      />
                    </div>

                    <div className="col-md-8">
                      <label className="form-label fw-semibold text-secondary small">
                        Nombre de la Asignatura <span className="text-danger">*</span>
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={editForm.name}
                        maxLength="150"
                        onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Semestre Académico <span className="text-danger">*</span>
                      </label>
                      <select
                        className="form-select"
                        value={editForm.semester}
                        onChange={(e) =>
                          setEditForm({
                            ...editForm,
                            semester: Number(e.target.value),
                            prerequisiteCodes: [],
                          })
                        }
                        required
                      >
                        <option value={1}>1° Semestre</option>
                        <option value={2}>2° Semestre</option>
                        <option value={3}>3° Semestre</option>
                        <option value={4}>4° Semestre</option>
                      </select>
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-semibold text-secondary small">
                        Créditos Académicos (SCT) <span className="text-danger">*</span>
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        min="1"
                        max="7"
                        value={editForm.sctCredits}
                        onChange={(e) => setEditForm({ ...editForm, sctCredits: e.target.value })}
                        required
                      />
                    </div>

                    {/* Horas TEL */}
                    <div className="col-12">
                      <div className="p-3 bg-light rounded-3 border">
                        <label className="form-label fw-bold text-dark small mb-2">
                          Horas Semanales TEL (Teoría, Ejercicios, Laboratorio)
                        </label>
                        <div className="row g-2 align-items-center">
                          <div className="col-md-3">
                            <label className="small text-muted">Teoría:</label>
                            <input
                              type="number"
                              className="form-control form-control-sm"
                              min="0"
                              max="6"
                              value={editForm.theoryHours}
                              onChange={(e) => setEditForm({ ...editForm, theoryHours: e.target.value })}
                              required
                            />
                          </div>
                          <div className="col-md-3">
                            <label className="small text-muted">Ejercicios:</label>
                            <input
                              type="number"
                              className="form-control form-control-sm"
                              min="0"
                              max="6"
                              value={editForm.exerciseHours}
                              onChange={(e) => setEditForm({ ...editForm, exerciseHours: e.target.value })}
                              required
                            />
                          </div>
                          <div className="col-md-3">
                            <label className="small text-muted">Laboratorio:</label>
                            <input
                              type="number"
                              className="form-control form-control-sm"
                              min="0"
                              max="6"
                              value={editForm.laboratoryHours}
                              onChange={(e) => setEditForm({ ...editForm, laboratoryHours: e.target.value })}
                              required
                            />
                          </div>
                          <div className="col-md-3">
                            <label className="small text-muted">Formato TEL & Total:</label>
                            <div className="fw-bold font-monospace text-primary">
                              {editForm.theoryHours}-{editForm.exerciseHours}-{editForm.laboratoryHours} (
                              {Number(editForm.theoryHours) +
                                Number(editForm.exerciseHours) +
                                Number(editForm.laboratoryHours)}{' '}
                              hrs)
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Prerrequisitos */}
                    <div className="col-12">
                      <label className="form-label fw-semibold text-secondary small d-flex justify-content-between">
                        <span>
                          Prerrequisitos Académicos (Máximo 3){' '}
                          <span className="badge bg-light text-dark border ms-1">
                            {editForm.prerequisiteCodes.length} / 3
                          </span>
                        </span>
                        <span className="text-muted small">Semestres anteriores al {editForm.semester}°</span>
                      </label>

                      {editForm.semester === 1 ? (
                        <div className="alert alert-secondary py-2 px-3 small mb-0">
                          Las asignaturas de 1° Semestre no tienen prerrequisitos.
                        </div>
                      ) : (
                        <div className="border rounded-3 p-3 bg-white" style={{ maxHeight: '180px', overflowY: 'auto' }}>
                          <div className="row g-2">
                            {getEligiblePrerequisites(editForm.semester, selectedCourse.code).map((candidate) => (
                              <div key={candidate.id} className="col-md-6">
                                <div
                                  className={`p-2 rounded-2 border d-flex align-items-center gap-2 cursor-pointer ${editForm.prerequisiteCodes.includes(candidate.code)
                                      ? 'border-primary bg-primary bg-opacity-10'
                                      : 'bg-light'
                                    }`}
                                  onClick={() => handleTogglePrerequisite(candidate.code, true)}
                                  style={{ cursor: 'pointer' }}
                                >
                                  <input
                                    type="checkbox"
                                    className="form-check-input mt-0"
                                    checked={editForm.prerequisiteCodes.includes(candidate.code)}
                                    onChange={() => { }}
                                  />
                                  <div className="small text-truncate">
                                    <strong className="font-monospace text-primary">{candidate.code}</strong> &bull;{' '}
                                    {candidate.name} <span className="text-muted">({candidate.semester}° Sem)</span>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
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

/**
 * CourseCard renders an individual course in the semester grid.
 */
const CourseCard = ({ course, isAdmin, onEdit, onDelete }) => {
  return (
    <div className="p-3 rounded-3 border bg-light h-100 transition-hover">
      <div className="d-flex justify-content-between align-items-start mb-2">
        <div>
          <span className="badge bg-primary font-monospace me-2">{course.code}</span>
          <span className="badge bg-success bg-opacity-10 text-success fw-bold border border-success border-opacity-25">
            {course.sctCredits} SCT
          </span>
        </div>

        {isAdmin && (
          <div className="btn-group btn-group-sm">
            <button
              onClick={() => onEdit(course)}
              className="btn btn-outline-secondary btn-sm py-0 px-2"
              title="Editar Asignatura"
            >
              <i className="bi bi-pencil"></i>
            </button>
            <button
              onClick={() => onDelete(course)}
              className="btn btn-outline-danger btn-sm py-0 px-2"
              title="Eliminar Asignatura"
            >
              <i className="bi bi-trash"></i>
            </button>
          </div>
        )}
      </div>

      <h6 className="fw-bold text-dark mb-2">{course.name}</h6>

      <div className="d-flex flex-wrap align-items-center gap-2 small text-muted mb-2">
        <span className="badge bg-white text-dark border">
          <i className="bi bi-clock me-1 text-primary"></i>
          TEL: <strong>{course.tel}</strong> ({course.totalHours} hrs/sem)
        </span>
      </div>

      <div className="border-top pt-2 mt-1">
        <span className="text-muted small d-block mb-1" style={{ fontSize: '0.75rem' }}>
          Prerrequisitos:
        </span>
        {course.prerequisites && course.prerequisites.length > 0 ? (
          <div className="d-flex flex-wrap gap-1">
            {course.prerequisites.map((p) => (
              <span
                key={p.id}
                className="badge bg-secondary bg-opacity-10 text-dark border font-monospace small"
                title={`${p.name} (Semestre ${p.semester})`}
              >
                {p.code}
              </span>
            ))}
          </div>
        ) : (
          <span className="text-muted small fst-italic" style={{ fontSize: '0.75rem' }}>
            Sin prerrequisitos
          </span>
        )}
      </div>
    </div>
  );
};

export default StudyPlanDetailPage;
