import { useCallback, useEffect, useMemo, useState } from 'react';
import enrollmentService from '../services/enrollment.service';

const getErrorMessage = (error, fallback) =>
  error.response?.data?.message || error.response?.data?.error || fallback;

const DAYS = [
  { key: 'L', label: 'Lunes' },
  { key: 'M', label: 'Martes' },
  { key: 'W', label: 'Miércoles' },
  { key: 'J', label: 'Jueves' },
  { key: 'V', label: 'Viernes' },
];

const MODULE_TIMES = {
  1: '08:15 - 09:35',
  2: '09:50 - 11:10',
  3: '11:25 - 12:45',
  4: '13:45 - 15:05',
  5: '15:20 - 16:40',
  6: '16:55 - 18:15',
};

const MODULES = [1, 2, 3, 4, 5, 6];

const EnrollmentPage = () => {
  const [offer, setOffer] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [selectedSectionIds, setSelectedSectionIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [cancellingId, setCancellingId] = useState(null);
  const [alert, setAlert] = useState(null);
  const [semesterFilter, setSemesterFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showSchedule, setShowSchedule] = useState(true);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [offerRes, enrollmentsRes] = await Promise.all([
        enrollmentService.getAvailableOffer(),
        enrollmentService.getMyEnrollments(),
      ]);
      setOffer(offerRes.data || []);
      setEnrollments(enrollmentsRes.data || []);
      setAlert(null);
    } catch (error) {
      setAlert({
        type: 'danger',
        message: getErrorMessage(error, 'No fue posible cargar la información académica de inscripción.'),
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Set of already enrolled course codes in current open period
  const enrolledCourseCodes = useMemo(
    () => new Set(enrollments.map((e) => e.courseCode)),
    [enrollments]
  );

  // Remaining plan courses (courses not yet approved)
  const remainingPlanCourses = useMemo(() => {
    return offer.filter((item) => !item.isAlreadyApproved).length;
  }, [offer]);

  const minRequiredCourses = remainingPlanCourses < 3 ? 1 : 3;
  const maxAllowedCourses = remainingPlanCourses < 3 ? remainingPlanCourses : 6;

  // Selected sections list
  const selectedSectionsList = useMemo(() => {
    return offer.filter((item) => item.sectionId && selectedSectionIds.has(item.sectionId));
  }, [offer, selectedSectionIds]);

  const totalResultingCourses = enrollments.length + selectedSectionIds.size;
  const isCountValid =
    totalResultingCourses >= minRequiredCourses && totalResultingCourses <= maxAllowedCourses;

  // Total SCT credits in pending selection
  const selectedCredits = useMemo(() => {
    return selectedSectionsList.reduce((sum, item) => sum + (item.sctCredits || 0), 0);
  }, [selectedSectionsList]);

  // Total SCT credits already enrolled
  const enrolledCredits = useMemo(() => {
    return enrollments.reduce((sum, item) => sum + (item.sctCredits || 0), 0);
  }, [enrollments]);

  // Schedule map & conflict detection
  const { scheduleGrid, scheduleConflicts } = useMemo(() => {
    const grid = {}; // key: "L1" -> { code, name, type: 'enrolled' | 'selected' | 'conflict', conflictingWith: [] }
    const conflicts = [];

    // 1. Add already enrolled blocks
    enrollments.forEach((item) => {
      (item.scheduleTimeBlocks || []).forEach((block) => {
        grid[block] = {
          code: item.courseCode,
          name: item.courseName,
          type: 'enrolled',
        };
      });
    });

    // 2. Add selected blocks and check collisions
    selectedSectionsList.forEach((item) => {
      (item.timeBlocks || []).forEach((block) => {
        if (grid[block]) {
          // Conflict detected!
          const existing = grid[block];
          conflicts.push({
            block,
            courseA: existing.code,
            courseB: item.courseCode,
            courseBName: item.courseName,
          });
          grid[block] = {
            code: `${existing.code} / ${item.courseCode}`,
            name: 'Conflicto de Horario',
            type: 'conflict',
            conflictingWith: [existing.code, item.courseCode],
          };
        } else {
          grid[block] = {
            code: item.courseCode,
            name: item.courseName,
            type: 'selected',
          };
        }
      });
    });

    return { scheduleGrid: grid, scheduleConflicts: conflicts };
  }, [enrollments, selectedSectionsList]);

  // Lower semester progression check (Rule 8 preview)
  const progressionWarning = useMemo(() => {
    if (selectedSectionsList.length === 0) return null;

    const maxSelectedSemester = Math.max(
      ...selectedSectionsList.map((s) => s.semester || 1),
      ...enrollments.map((e) => e.semester || 1)
    );

    const pendingLowerCourses = offer.filter(
      (item) =>
        item.semester < maxSelectedSemester &&
        !item.isAlreadyApproved &&
        !enrolledCourseCodes.has(item.courseCode) &&
        !selectedSectionIds.has(item.sectionId) &&
        item.isSelectable
    );

    if (pendingLowerCourses.length > 0) {
      return pendingLowerCourses.map((c) => `${c.courseCode} (${c.courseName}, Semestre ${c.semester})`);
    }
    return null;
  }, [selectedSectionsList, enrollments, offer, enrolledCourseCodes, selectedSectionIds]);

  // Toggle selection of a section
  const handleToggleSection = (sectionId) => {
    setSelectedSectionIds((prev) => {
      const next = new Set(prev);
      if (next.has(sectionId)) {
        next.delete(sectionId);
      } else {
        if (enrollments.length + next.size >= maxAllowedCourses) {
          setAlert({
            type: 'warning',
            message: `Límite alcanzado: no puedes inscribir más de ${maxAllowedCourses} asignaturas en el período.`,
          });
          return next;
        }
        next.add(sectionId);
      }
      return next;
    });
  };

  // Submit batch registration
  const handleConfirmEnrollment = async () => {
    if (selectedSectionIds.size === 0) return;

    if (!isCountValid) {
      setAlert({
        type: 'danger',
        message: `Debes seleccionar entre ${minRequiredCourses} y ${maxAllowedCourses} asignaturas (actual: ${totalResultingCourses}).`,
      });
      return;
    }

    if (scheduleConflicts.length > 0) {
      setAlert({
        type: 'danger',
        message: `Existe un tope de horario en los módulos seleccionados. Resuelve el conflicto antes de confirmar.`,
      });
      return;
    }

    try {
      setSubmitting(true);
      setAlert(null);
      const sectionIdsArray = Array.from(selectedSectionIds);
      await enrollmentService.register(undefined, sectionIdsArray);
      setAlert({
        type: 'success',
        message: `¡Inscripción confirmada exitosamente! Se registraron ${sectionIdsArray.length} asignaturas en el período actual.`,
      });
      setSelectedSectionIds(new Set());
      await loadData();
    } catch (error) {
      setAlert({
        type: 'danger',
        message: getErrorMessage(error, 'No fue posible registrar la inscripción.'),
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Cancel an active enrollment
  const handleCancelEnrollment = async (enrollment) => {
    const confirmed = window.confirm(
      `¿Confirmas la anulación de la inscripción en "${enrollment.courseCode} - ${enrollment.courseName}"?\nSe liberará un cupo en la sección.`
    );
    if (!confirmed) return;

    try {
      setCancellingId(enrollment.id);
      await enrollmentService.cancel(enrollment.id);
      setAlert({
        type: 'success',
        message: `Inscripción de ${enrollment.courseCode} anulada correctamente y cupo liberado.`,
      });
      await loadData();
    } catch (error) {
      setAlert({
        type: 'danger',
        message: getErrorMessage(error, 'No fue posible anular la inscripción.'),
      });
    } finally {
      setCancellingId(null);
    }
  };

  // Filtered offer list
  const filteredOffer = useMemo(() => {
    return offer.filter((item) => {
      // Semester filter
      if (semesterFilter !== 'ALL' && item.semester !== Number(semesterFilter)) {
        return false;
      }
      // Search query
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const matchCode = item.courseCode?.toLowerCase().includes(query);
        const matchName = item.courseName?.toLowerCase().includes(query);
        const matchSec = item.sectionCode?.toLowerCase().includes(query);
        if (!matchCode && !matchName && !matchSec) return false;
      }
      return true;
    });
  }, [offer, semesterFilter, searchQuery]);

  // Available semesters in study plan
  const availableSemesters = useMemo(() => {
    const sems = new Set(offer.map((o) => o.semester).filter(Boolean));
    return Array.from(sems).sort((a, b) => a - b);
  }, [offer]);

  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status" style={{ width: '3rem', height: '3rem' }}></div>
        <h5 className="fw-bold mt-3">Cargando Oferta Académica e Inscripciones...</h5>
        <p className="text-muted small">Validando tu plan de estudios, estado curricular y prerrequisitos formativos.</p>
      </div>
    );
  }

  return (
    <div className="container py-4">
      {/* Top Header Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 50%, #3b82f6 100%)',
        }}
      >
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-black small fw-semibold">
              <i className="bi bi-pencil-square"></i>
              <span>Inscripción de Asignaturas</span>
            </div>
            <h1 className="display-6 fw-bold mb-2">Inscripción Académica IPT</h1>
            <p className="lead mb-0 text-white-75 fs-6">
              Selecciona las asignaturas que cursarás durante el período académico abierto.
            </p>
          </div>

          <div className="d-flex flex-wrap gap-2">
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{enrollments.length}</div>
              <div className="small text-black-50">Inscritas</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-warning">+{selectedSectionIds.size}</div>
              <div className="small text-black-50">Seleccionadas</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">
                {minRequiredCourses} - {maxAllowedCourses}
              </div>
              <div className="small text-black-50">Meta Asignaturas</div>
            </div>
          </div>
        </div>
      </div>

      {/* Global Alerts */}
      {alert && (
        <div className={`alert alert-${alert.type} alert-dismissible fade show shadow-sm mb-4`} role="alert">
          <i
            className={`bi ${alert.type === 'success'
              ? 'bi-check-circle-fill'
              : alert.type === 'warning'
                ? 'bi-exclamation-triangle-fill'
                : 'bi-x-circle-fill'
              } me-2`}
          ></i>
          {alert.message}
          <button type="button" className="btn-close" onClick={() => setAlert(null)}></button>
        </div>
      )}

      {/* Schedule Conflicts Warning */}
      {scheduleConflicts.length > 0 && (
        <div className="alert alert-danger shadow-sm mb-4" role="alert">
          <div className="d-flex align-items-center gap-2 mb-1">
            <i className="bi bi-exclamation-octagon-fill fs-5"></i>
            <strong className="fs-6">Conflicto de horario detectado en tu selección</strong>
          </div>
          <ul className="mb-0 small ps-4">
            {scheduleConflicts.map((c, i) => (
              <li key={i}>
                Bloque <strong>{c.block}</strong>: Choque entre <strong>{c.courseA}</strong> y <strong>{c.courseB}</strong> ({c.courseBName}).
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Progression Advisory Warning */}
      {progressionWarning && (
        <div className="alert alert-warning shadow-sm mb-4" role="alert">
          <div className="d-flex align-items-center gap-2 mb-1">
            <i className="bi bi-diagram-3-fill fs-5"></i>
            <strong>Regla de Progresión por Semestre</strong>
          </div>
          <p className="small mb-1">
            Has seleccionado asignaturas de un semestre superior. El reglamento institucional establece que debes incluir
            las asignaturas pendientes con cupo disponible de semestres anteriores:
          </p>
          <ul className="small mb-0 ps-4">
            {progressionWarning.map((item, idx) => (
              <li key={idx}><strong>{item}</strong></li>
            ))}
          </ul>
        </div>
      )}

      {/* Active Enrollments Section (If any) */}
      {enrollments.length > 0 && (
        <div className="card border-0 shadow-sm rounded-4 mb-4">
          <div className="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
            <div className="d-flex align-items-center gap-2">
              <span className="badge bg-success p-2">
                <i className="bi bi-check2-circle fs-6"></i>
              </span>
              <div>
                <h5 className="fw-bold mb-0 text-success">Asignaturas Oficialmente Inscritas</h5>
                <span className="text-muted small">
                  {enrollments.length} asignaturas &bull; {enrolledCredits} Créditos SCT
                </span>
              </div>
            </div>
            <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-3 py-2 fw-semibold">
              Período Activo
            </span>
          </div>

          <div className="card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Código</th>
                    <th>Asignatura</th>
                    <th>Sección</th>
                    <th>Docente</th>
                    <th>Horario</th>
                    <th className="text-center">SCT</th>
                    <th className="text-end">Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {enrollments.map((enr) => (
                    <tr key={enr.id}>
                      <td className="font-monospace fw-semibold text-primary">{enr.courseCode}</td>
                      <td>
                        <div className="fw-semibold text-dark">{enr.courseName}</div>
                        <span className="text-muted small">Semestre {enr.semester}</span>
                      </td>
                      <td>
                        <span className="badge bg-light text-dark border font-monospace">
                          {enr.sectionCode || 'Sección'}
                        </span>
                      </td>
                      <td className="small text-muted">{enr.teacherName || 'Docente asignado'}</td>
                      <td>
                        <div className="d-flex flex-wrap gap-1">
                          {(enr.scheduleTimeBlocks || []).map((b) => (
                            <span key={b} className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 font-monospace">
                              {b}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="text-center fw-semibold">{enr.sctCredits}</td>
                      <td className="text-end">
                        <button
                          className="btn btn-outline-danger btn-sm rounded-3 d-inline-flex align-items-center gap-1"
                          onClick={() => handleCancelEnrollment(enr)}
                          disabled={cancellingId === enr.id}
                          title="Anular inscripción de esta asignatura"
                        >
                          {cancellingId === enr.id ? (
                            <span className="spinner-border spinner-border-sm"></span>
                          ) : (
                            <i className="bi bi-trash"></i>
                          )}
                          <span>Anular</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Main Two-Column Layout */}
      <div className="row g-4">
        {/* Left Column: Course Catalog */}
        <div className="col-12 col-lg-8">
          <div className="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
            <div className="card-header bg-white border-bottom p-3 p-md-4">
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
                <div>
                  <h4 className="fw-bold mb-1 text-dark">
                    <i className="bi bi-collection-fill text-primary me-2"></i>
                    Oferta Académica Disponible
                  </h4>
                  <p className="text-muted small mb-0">
                    Marca las asignaturas que deseas inscribir ({selectedSectionIds.size} seleccionadas).
                  </p>
                </div>

                {/* Confirm Enrollment Button (Prominent) */}
                <button
                  className="btn btn-primary fw-bold px-4 py-2 rounded-3 shadow-sm d-flex align-items-center gap-2"
                  onClick={handleConfirmEnrollment}
                  disabled={
                    submitting ||
                    selectedSectionIds.size === 0 ||
                    !isCountValid ||
                    scheduleConflicts.length > 0
                  }
                  style={{ backgroundColor: '#2563eb' }}
                >
                  {submitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm"></span>
                      <span>Procesando...</span>
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check2-all fs-5"></i>
                      <span>Confirmar Inscripción ({selectedSectionIds.size})</span>
                    </>
                  )}
                </button>
              </div>

              {/* Filters and Search Bar */}
              <div className="row g-2 align-items-center">
                <div className="col-12 col-md-7">
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-search text-muted"></i>
                    </span>
                    <input
                      type="text"
                      className="form-control border-start-0 bg-light"
                      placeholder="Buscar por código, asignatura o sección..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />
                    {searchQuery && (
                      <button
                        className="btn btn-light border"
                        type="button"
                        onClick={() => setSearchQuery('')}
                      >
                        <i className="bi bi-x"></i>
                      </button>
                    )}
                  </div>
                </div>

                <div className="col-12 col-md-5">
                  <div className="d-flex gap-1 overflow-auto">
                    <button
                      className={`btn btn-sm rounded-pill px-3 ${semesterFilter === 'ALL' ? 'btn-primary' : 'btn-outline-secondary'
                        }`}
                      onClick={() => setSemesterFilter('ALL')}
                    >
                      Todos
                    </button>
                    {availableSemesters.map((sem) => (
                      <button
                        key={sem}
                        className={`btn btn-sm rounded-pill px-3 ${semesterFilter === String(sem) ? 'btn-primary' : 'btn-outline-secondary'
                          }`}
                        onClick={() => setSemesterFilter(String(sem))}
                      >
                        Sem {sem}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Offer List */}
            <div className="card-body p-0">
              {filteredOffer.length === 0 ? (
                <div className="p-5 text-center text-muted">
                  <i className="bi bi-inbox fs-1 d-block mb-2 text-muted"></i>
                  <p className="mb-0">No se encontraron asignaturas con los filtros seleccionados.</p>
                </div>
              ) : (
                <div className="list-group list-group-flush">
                  {filteredOffer.map((item) => {
                    const isEnrolled = enrolledCourseCodes.has(item.courseCode) || item.isCurrentlyEnrolled;
                    const isSelected = item.sectionId && selectedSectionIds.has(item.sectionId);
                    const canSelect = item.isSelectable && !isEnrolled && item.sectionId;

                    return (
                      <div
                        key={item.courseId}
                        className={`list-group-item p-4 transition-all bg-white ${isSelected
                          ? 'bg-primary bg-opacity-10 border-start border-4 border-primary'
                          : isEnrolled
                            ? 'border-start border-4 border-success'
                            : ''
                          }`}
                      >
                        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                          <div className="flex-grow-1" style={{ maxWidth: '75%' }}>
                            {/* Badges Row */}
                            <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
                              <span className="badge bg-secondary bg-opacity-15 text-white border">
                                Semestre {item.semester}
                              </span>
                              <span className="badge bg-light text-primary border font-monospace">
                                {item.courseCode}
                              </span>
                              <span className="badge bg-light text-dark border">
                                {item.sctCredits} SCT
                              </span>
                              <span className="badge bg-light text-muted border">
                                TEL: {item.totalTelHours} hrs ({item.theoryHours}T-{item.exerciseHours}E-{item.laboratoryHours}L)
                              </span>
                            </div>

                            {/* Subject Name */}
                            <h5 className="fw-bold mb-1 text-dark">{item.courseName}</h5>

                            {/* Section & Teacher */}
                            <div className="small text-muted mb-2">
                              {item.sectionCode ? (
                                <>
                                  <strong className="text-dark font-monospace me-2">{item.sectionCode}</strong>
                                  &bull; Docente: {item.teacherName || 'Por asignar'}
                                </>
                              ) : (
                                <span className="text-danger">Sin sección aperturada en este período</span>
                              )}
                            </div>

                            {/* Schedule & Quota Indicators */}
                            {item.sectionId && (
                              <div className="d-flex flex-wrap align-items-center gap-3 small mb-2">
                                <span>
                                  <i className="bi bi-people-fill text-muted me-1"></i>
                                  Cupos: <strong>{item.availableQuota}</strong> / {item.capacity} disponibles
                                </span>
                                <span>
                                  <i className="bi bi-clock-fill text-muted me-1"></i>
                                  Horario:{' '}
                                  {item.timeBlocks && item.timeBlocks.length > 0 ? (
                                    item.timeBlocks.map((b) => (
                                      <span key={b} className="badge bg-light text-dark border font-monospace me-1">
                                        {b}
                                      </span>
                                    ))
                                  ) : (
                                    <span className="text-muted">Sin horario</span>
                                  )}
                                </span>
                              </div>
                            )}

                            {/* Prerequisites */}
                            {item.prerequisiteCodes && item.prerequisiteCodes.length > 0 && (
                              <div className="small mt-2">
                                <span className="text-muted me-1">Prerrequisitos:</span>
                                {item.prerequisiteCodes.map((code, idx) => (
                                  <span
                                    key={code}
                                    className={`badge me-1 ${item.isPrerequisitesMet
                                      ? 'bg-success bg-opacity-10 text-success border border-success border-opacity-25'
                                      : 'bg-warning bg-opacity-15 text-dark border border-warning'
                                      }`}
                                    title={item.prerequisiteNames?.[idx] || ''}
                                  >
                                    <i className={`bi ${item.isPrerequisitesMet ? 'bi-check' : 'bi-lock'} me-1`}></i>
                                    {code}
                                  </span>
                                ))}
                              </div>
                            )}

                            {/* Ineligibility Reason Callout */}
                            {!item.isSelectable && item.ineligibilityReason && !isEnrolled && (
                              <div className="alert alert-warning py-1 px-3 mt-2 mb-0 small border-0 bg-warning bg-opacity-10 text-dark">
                                <i className="bi bi-info-circle me-1"></i>
                                {item.ineligibilityReason}
                              </div>
                            )}
                          </div>

                          {/* Action Button */}
                          <div className="text-end">
                            {isEnrolled ? (
                              <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-3 py-2 fs-6 rounded-pill d-inline-flex align-items-center gap-1 fw-semibold">
                                <i className="bi bi-check-circle-fill"></i>
                                <span>Inscrita</span>
                              </span>
                            ) : item.isAlreadyApproved ? (
                              <span className="badge bg-secondary px-3 py-2 fs-6 text-white">
                                <i className="bi bi-award-fill me-1"></i>
                                Aprobada
                              </span>
                            ) : isSelected ? (
                              <button
                                className="btn btn-primary fw-semibold btn-sm px-3 py-2 rounded-3 shadow-2xs d-flex align-items-center gap-1"
                                onClick={() => handleToggleSection(item.sectionId)}
                              >
                                <i className="bi bi-check-lg"></i>
                                <span>Seleccionada</span>
                              </button>
                            ) : (
                              <button
                                className={`btn btn-sm px-3 py-2 rounded-3 fw-semibold ${canSelect
                                  ? 'btn-outline-primary'
                                  : 'btn-light text-muted border'
                                  }`}
                                disabled={!canSelect}
                                onClick={() => handleToggleSection(item.sectionId)}
                              >
                                <i className="bi bi-plus-circle me-1"></i>
                                Añadir a selección
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Floating Summary & Schedule Grid */}
        <div className="col-12 col-lg-4">
          <div
            className="sticky-lg-top"
            style={{
              top: '5rem',
              maxHeight: 'calc(100vh - 5.5rem)',
              overflowY: 'auto',
              paddingRight: '4px',
            }}
          >
            {/* Summary / Cart Card */}
            <div className="card border-0 shadow-sm rounded-4 mb-3">
              <div className="card-header bg-white border-bottom py-3">
                <div className="d-flex justify-content-between align-items-center">
                  <h5 className="fw-bold mb-0 text-dark">
                    <i className="bi bi-calendar-check-fill text-primary me-2"></i>
                    Resumen de Inscripción
                  </h5>
                  <span
                    className={`badge px-3 py-2 fw-semibold ${isCountValid
                      ? 'bg-success bg-opacity-10 text-success border border-success'
                      : 'bg-warning bg-opacity-15 text-dark border border-warning'
                      }`}
                  >
                    {totalResultingCourses} / {minRequiredCourses}-{maxAllowedCourses} Asignaturas
                  </span>
                </div>
              </div>

              <div className="card-body p-3">
                {/* Progress Bar of Courses Count */}
                <div className="mb-2">
                  <div className="d-flex justify-content-between small text-muted mb-1">
                    <span>Progreso de selección:</span>
                    <span>
                      <strong>{totalResultingCourses}</strong> de {minRequiredCourses} mín. (máx. {maxAllowedCourses})
                    </span>
                  </div>
                  <div className="progress" style={{ height: '6px' }}>
                    <div
                      className={`progress-bar ${totalResultingCourses > maxAllowedCourses
                        ? 'bg-danger'
                        : isCountValid
                          ? 'bg-success'
                          : 'bg-warning'
                        }`}
                      role="progressbar"
                      style={{
                        width: `${Math.min(100, (totalResultingCourses / maxAllowedCourses) * 100)}%`,
                      }}
                    ></div>
                  </div>
                </div>

                {/* Selection Details */}
                <div className="bg-light rounded-3 p-2 px-3 mb-2 small">
                  <div className="d-flex justify-content-between py-1">
                    <span className="text-muted">Inscritas / Nuevas:</span>
                    <span className="fw-semibold">
                      {enrollments.length} <span className="text-muted">|</span>{' '}
                      <span className="text-primary">+{selectedSectionIds.size}</span>
                    </span>
                  </div>
                  <div className="d-flex justify-content-between py-1 border-top">
                    <span className="text-muted">Créditos a sumar:</span>
                    <span className="fw-semibold text-primary">+{selectedCredits} SCT</span>
                  </div>
                </div>

                {/* Selected List Chips with scroll */}
                {selectedSectionsList.length > 0 && (
                  <div className="mb-3">
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <span className="text-muted small">Por inscribir ({selectedSectionsList.length}):</span>
                      <button
                        className="btn btn-link btn-sm text-danger p-0 text-decoration-none small"
                        onClick={() => setSelectedSectionIds(new Set())}
                      >
                        Limpiar
                      </button>
                    </div>
                    <div
                      className="d-flex flex-column gap-2 pe-1"
                      style={{ maxHeight: '135px', overflowY: 'auto' }}
                    >
                      {selectedSectionsList.map((item) => (
                        <div
                          key={item.sectionId}
                          className="d-flex justify-content-between align-items-center bg-white border rounded-3 p-2 shadow-2xs"
                        >
                          <div className="small text-truncate me-2">
                            <strong className="text-primary font-monospace me-1">{item.courseCode}</strong>
                            <span className="text-muted small">{item.courseName}</span>
                          </div>
                          <button
                            className="btn btn-outline-danger btn-sm p-1 rounded-circle border-0 flex-shrink-0"
                            onClick={() => handleToggleSection(item.sectionId)}
                            title="Quitar"
                          >
                            <i className="bi bi-x-circle-fill"></i>
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Submit Button */}
                <div className="pt-1">
                  <button
                    className="btn btn-primary w-100 fw-bold py-2 rounded-3 shadow-sm d-flex align-items-center justify-content-center gap-2"
                    onClick={handleConfirmEnrollment}
                    disabled={
                      submitting ||
                      selectedSectionIds.size === 0 ||
                      !isCountValid ||
                      scheduleConflicts.length > 0
                    }
                    style={{ backgroundColor: '#2563eb' }}
                  >
                    {submitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm"></span>
                        <span>Procesando inscripción...</span>
                      </>
                    ) : (
                      <>
                        <i className="bi bi-check-circle-fill"></i>
                        <span>Confirmar Inscripción ({selectedSectionIds.size})</span>
                      </>
                    )}
                  </button>
                </div>

                {/* Requirement Hint */}
                <div className="text-muted small text-center mt-2">
                  {!isCountValid && totalResultingCourses < minRequiredCourses && (
                    <span className="text-danger">
                      <i className="bi bi-info-circle me-1"></i>
                      Debes seleccionar al menos {minRequiredCourses - totalResultingCourses} más.
                    </span>
                  )}
                  {!isCountValid && totalResultingCourses > maxAllowedCourses && (
                    <span className="text-danger">
                      <i className="bi bi-exclamation-triangle me-1"></i>
                      Excedes el máximo de {maxAllowedCourses} asignaturas.
                    </span>
                  )}
                  {isCountValid && scheduleConflicts.length === 0 && (
                    <span className="text-success">
                      <i className="bi bi-check-circle me-1"></i>
                      Selección válida para confirmar.
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Weekly Schedule Grid (5x6) */}
            <div className="card border-0 shadow-sm rounded-4 mb-3">
              <div
                className="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center"
                role="button"
                onClick={() => setShowSchedule((prev) => !prev)}
                style={{ cursor: 'pointer' }}
              >
                <div>
                  <h6 className="fw-bold mb-0 text-dark">
                    <i className="bi bi-calendar3 text-primary me-2"></i>
                    Horario Semanal
                  </h6>
                  <span className="text-muted small">Módulos inscritos y preseleccionados</span>
                </div>
                <span className="badge bg-light text-muted border">
                  <i className={`bi bi-chevron-${showSchedule ? 'up' : 'down'}`}></i>
                </span>
              </div>

              {showSchedule && (
                <div className="card-body p-2">
                  <div className="table-responsive">
                    <table
                      className="table table-bordered table-sm text-center align-middle mb-0"
                      style={{ tableLayout: 'fixed', fontSize: '0.75rem' }}
                    >
                      <thead className="table-light">
                        <tr>
                          <th style={{ width: '45px' }} className="text-muted">Mód.</th>
                          {DAYS.map((d) => (
                            <th key={d.key} className="fw-semibold">
                              {d.key}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {MODULES.map((mod) => (
                          <tr key={mod}>
                            <th className="table-light text-muted fw-normal" title={MODULE_TIMES[mod]}>
                              M{mod}
                            </th>
                            {DAYS.map((d) => {
                              const blockKey = `${d.key}${mod}`;
                              const cell = scheduleGrid[blockKey];

                              let cellClass = 'text-muted';
                              let content = '-';

                              if (cell) {
                                if (cell.type === 'conflict') {
                                  cellClass = 'bg-danger text-white fw-bold';
                                  content = '! Tope';
                                } else if (cell.type === 'enrolled') {
                                  cellClass = 'bg-success text-white fw-bold';
                                  content = cell.code;
                                } else if (cell.type === 'selected') {
                                  cellClass = 'bg-primary text-white fw-bold';
                                  content = cell.code;
                                }
                              }

                              return (
                                <td
                                  key={d.key}
                                  className={cellClass}
                                  style={{ padding: '6px 2px', minHeight: '32px' }}
                                  title={cell ? `${cell.name} (${blockKey})` : `${d.label} M${mod} (${MODULE_TIMES[mod]})`}
                                >
                                  {content}
                                </td>
                              );
                            })}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Schedule Legend */}
                  <div className="d-flex flex-wrap justify-content-center gap-3 pt-3 small text-muted border-top mt-2">
                    <div className="d-flex align-items-center gap-1">
                      <span className="d-inline-block bg-success rounded-1" style={{ width: '12px', height: '12px' }}></span>
                      <span>Inscrita</span>
                    </div>
                    <div className="d-flex align-items-center gap-1">
                      <span className="d-inline-block bg-primary rounded-1" style={{ width: '12px', height: '12px' }}></span>
                      <span>Preseleccionada</span>
                    </div>
                    <div className="d-flex align-items-center gap-1">
                      <span className="d-inline-block bg-danger rounded-1" style={{ width: '12px', height: '12px' }}></span>
                      <span>Conflicto / Tope</span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnrollmentPage;