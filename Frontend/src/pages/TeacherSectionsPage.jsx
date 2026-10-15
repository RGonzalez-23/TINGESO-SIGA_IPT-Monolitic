import { useCallback, useEffect, useMemo, useState } from 'react';
import sectionRosterService from '../services/section-roster.service';

const TeacherSectionsPage = () => {
  const [sections, setSections] = useState([]);
  const [rosters, setRosters] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadingRosterId, setLoadingRosterId] = useState(null);
  const [expandedSectionId, setExpandedSectionId] = useState(null);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [studentSearchTerm, setStudentSearchTerm] = useState('');
  const [copiedSectionId, setCopiedSectionId] = useState(null);

  const fetchSections = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await sectionRosterService.getMySections();
      setSections(response.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'No fue posible cargar tus secciones asignadas.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSections();
  }, [fetchSections]);

  const toggleRoster = async (sectionId) => {
    if (expandedSectionId === sectionId) {
      setExpandedSectionId(null);
      setStudentSearchTerm('');
      return;
    }

    try {
      if (!rosters[sectionId]) {
        setLoadingRosterId(sectionId);
        const response = await sectionRosterService.getEnrolledStudents(sectionId);
        setRosters((prev) => ({ ...prev, [sectionId]: response.data || [] }));
      }
      setExpandedSectionId(sectionId);
      setStudentSearchTerm('');
    } catch (err) {
      setError(err.response?.data?.message || 'No fue posible cargar la nómina de estudiantes.');
    } finally {
      setLoadingRosterId(null);
    }
  };

  const handleCopyEmails = (sectionId) => {
    const students = rosters[sectionId] || [];
    const emails = students.map((s) => s.email).filter(Boolean).join(', ');
    if (emails) {
      navigator.clipboard.writeText(emails);
      setCopiedSectionId(sectionId);
      setTimeout(() => setCopiedSectionId(null), 3000);
    }
  };

  // Filter sections by search term
  const filteredSections = useMemo(() => {
    if (!searchTerm.trim()) return sections;
    const term = searchTerm.toLowerCase();
    return sections.filter((sec) =>
      sec.courseCode?.toLowerCase().includes(term) ||
      sec.courseName?.toLowerCase().includes(term) ||
      sec.sectionCode?.toLowerCase().includes(term) ||
      sec.careerName?.toLowerCase().includes(term)
    );
  }, [sections, searchTerm]);

  // Total metrics
  const totalEnrolled = useMemo(() => {
    return sections.reduce((sum, sec) => sum + (sec.enrolledCount || 0), 0);
  }, [sections]);

  return (
    <div className="container py-4">
      {/* Header Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #0e7490 0%, #0891b2 50%, #06b6d4 100%)',
        }}
      >
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-black small fw-semibold">
              <i className="bi bi-person-workspace"></i>
              <span>Docencia &bull; Instituto Profesional de Tecnología</span>
            </div>
            <h1 className="display-6 fw-bold mb-2">Mis Secciones Asignadas</h1>
            <p className="lead mb-0 text-white-75 fs-6">
              Revisa los horarios de tus cursos, la capacidad y la nómina oficial de estudiantes inscritos.
            </p>
          </div>

          <div className="d-flex flex-wrap gap-2">
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{sections.length}</div>
              <div className="small text-black">Secciones</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{totalEnrolled}</div>
              <div className="small text-black">Alumnos Inscritos</div>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
          <button type="button" className="btn-close" onClick={() => setError(null)}></button>
        </div>
      )}

      {/* Search & Actions Bar */}
      <div className="card border-0 shadow-sm rounded-4 mb-4">
        <div className="card-body p-3 p-md-4">
          <div className="row g-3 align-items-center justify-content-between">
            <div className="col-12 col-md-6 col-lg-5">
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-search text-muted"></i>
                </span>
                <input
                  type="text"
                  className="form-control border-start-0 bg-light"
                  placeholder="Buscar por código, asignatura o sección..."
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

            <div className="col-12 col-md-auto d-flex align-items-center gap-2">
              <button
                className="btn btn-outline-secondary btn-sm rounded-3 d-flex align-items-center gap-2"
                onClick={fetchSections}
                disabled={loading}
              >
                <i className={`bi bi-arrow-clockwise ${loading ? 'spin' : ''}`}></i>
                <span>Actualizar</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      {loading ? (
        <div className="py-5 text-center">
          <div className="spinner-border text-info" role="status"></div>
          <p className="mt-3 text-muted">Cargando tus secciones asignadas...</p>
        </div>
      ) : filteredSections.length === 0 ? (
        <div className="card border-0 shadow-sm rounded-4 p-5 text-center">
          <i className="bi bi-inbox text-muted display-4 mb-3"></i>
          <h5 className="fw-bold">No se encontraron secciones</h5>
          <p className="text-muted small mb-0">
            {searchTerm
              ? 'No hay secciones que coincidan con el término de búsqueda ingresado.'
              : 'Actualmente no tienes secciones asignadas en períodos académicos abiertos.'}
          </p>
        </div>
      ) : (
        <div className="row g-4">
          {filteredSections.map((sec) => {
            const isExpanded = expandedSectionId === sec.id;
            const currentRoster = rosters[sec.id] || [];
            const filteredRoster = currentRoster.filter((st) => {
              if (!studentSearchTerm.trim()) return true;
              const term = studentSearchTerm.toLowerCase();
              return (
                st.fullName?.toLowerCase().includes(term) ||
                st.run?.toLowerCase().includes(term) ||
                st.email?.toLowerCase().includes(term)
              );
            });

            const quotaPercentage = sec.capacity > 0
              ? Math.min(100, Math.round((sec.enrolledCount / sec.capacity) * 100))
              : 0;

            return (
              <div className="col-12" key={sec.id}>
                <div className="card border-0 shadow-sm rounded-4 overflow-hidden">
                  <div className="card-body p-4">
                    {/* Section Top Header */}
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
                      <div>
                        <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
                          <span className="badge bg-dark bg-opacity-75 font-monospace fs-6 px-3 py-1">
                            {sec.sectionCode}
                          </span>
                          <span className="badge bg-info bg-opacity-10 text-info fw-semibold border border-info border-opacity-25">
                            Período {sec.academicPeriodId}
                          </span>
                          <span className="badge bg-secondary bg-opacity-10 text-secondary fw-semibold">
                            Semestre {sec.semester}
                          </span>
                          {sec.academicPeriodOpen ? (
                            <span className="badge bg-success bg-opacity-10 text-success fw-semibold">
                              <i className="bi bi-check-circle-fill me-1"></i>Período Abierto
                            </span>
                          ) : (
                            <span className="badge bg-secondary text-white fw-semibold">
                              Cerrado
                            </span>
                          )}
                        </div>
                        <h4 className="fw-bold text-dark mb-1">
                          {sec.courseCode} &bull; {sec.courseName}
                        </h4>
                        <div className="small text-muted">
                          <i className="bi bi-mortarboard me-1"></i>
                          {sec.careerName || 'Plan General'}
                        </div>
                      </div>

                      {/* Action Button */}
                      <button
                        className={`btn btn-sm px-3 py-2 rounded-3 fw-semibold d-flex align-items-center gap-2 transition-all ${isExpanded
                          ? 'btn-info text-white'
                          : 'btn-outline-info text-dark border-info'
                          }`}
                        onClick={() => toggleRoster(sec.id)}
                        disabled={loadingRosterId === sec.id}
                      >
                        {loadingRosterId === sec.id ? (
                          <>
                            <span className="spinner-border spinner-border-sm me-1"></span>
                            <span>Cargando nómina...</span>
                          </>
                        ) : (
                          <>
                            <i className={`bi ${isExpanded ? 'bi-chevron-up' : 'bi-people-fill'}`}></i>
                            <span>{isExpanded ? 'Ocultar Nómina' : `Ver Nómina (${sec.enrolledCount})`}</span>
                          </>
                        )}
                      </button>
                    </div>

                    {/* Section Details Grid */}
                    <div className="row g-3 py-3 my-1 border-top border-bottom bg-light bg-opacity-50 rounded-3 px-2">
                      <div className="col-12 col-md-4">
                        <span className="text-muted small d-block mb-1">Horario Asignado:</span>
                        <div className="d-flex flex-wrap gap-1">
                          {sec.timeBlocks && sec.timeBlocks.length > 0 ? (
                            sec.timeBlocks.map((b) => (
                              <span
                                key={b.code || b}
                                className="badge bg-white text-dark border font-monospace shadow-2xs"
                                title={b.startTime ? `${b.startTime} - ${b.endTime}` : ''}
                              >
                                {b.code || b}
                              </span>
                            ))
                          ) : (
                            <span className="text-muted small">Sin módulos horarios definidos</span>
                          )}
                        </div>
                      </div>

                      <div className="col-6 col-md-4">
                        <span className="text-muted small d-block mb-1">Carga Académica:</span>
                        <span className="fw-semibold small">
                          {sec.sctCredits ?? 0} SCT &bull; TEL: {sec.theoryHours ?? 0}T - {sec.exerciseHours ?? 0}E - {sec.laboratoryHours ?? 0}L
                        </span>
                      </div>

                      <div className="col-6 col-md-4">
                        <span className="text-muted small d-block mb-1">
                          Cupos: <strong>{sec.enrolledCount}</strong> / {sec.capacity}
                          <span className="ms-1 text-muted">({sec.availableQuota} disponibles)</span>
                        </span>
                        <div className="progress" style={{ height: '8px' }}>
                          <div
                            className={`progress-bar ${quotaPercentage >= 100
                              ? 'bg-danger'
                              : quotaPercentage >= 80
                                ? 'bg-warning'
                                : 'bg-info'
                              }`}
                            role="progressbar"
                            style={{ width: `${quotaPercentage}%` }}
                            aria-valuenow={quotaPercentage}
                            aria-valuemin="0"
                            aria-valuemax="100"
                          ></div>
                        </div>
                      </div>
                    </div>

                    {/* Collapsible Enrolled Students Roster */}
                    {isExpanded && (
                      <div className="mt-4 pt-2">
                        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                          <div>
                            <h6 className="fw-bold mb-0">
                              <i className="bi bi-people-fill text-info me-2"></i>
                              Nómina Oficial de Estudiantes
                            </h6>
                            <span className="text-muted small">
                              {currentRoster.length} {currentRoster.length === 1 ? 'alumno inscrito' : 'alumnos inscritos'}
                            </span>
                          </div>

                          <div className="d-flex flex-wrap gap-2">
                            {currentRoster.length > 0 && (
                              <button
                                className="btn btn-outline-secondary btn-sm rounded-3 d-flex align-items-center gap-1"
                                onClick={() => handleCopyEmails(sec.id)}
                                title="Copiar lista de correos para enviar email"
                              >
                                <i className={`bi ${copiedSectionId === sec.id ? 'bi-check-lg text-success' : 'bi-clipboard'}`}></i>
                                <span>{copiedSectionId === sec.id ? '¡Copiados!' : 'Copiar Correos'}</span>
                              </button>
                            )}

                            {currentRoster.length > 0 && (
                              <div style={{ minWidth: '220px', maxWidth: '280px' }}>
                                <div className="input-group input-group-sm">
                                  <span className="input-group-text bg-white border-end-0">
                                    <i className="bi bi-search text-muted"></i>
                                  </span>
                                  <input
                                    type="text"
                                    className="form-control border-start-0 ps-0"
                                    placeholder="Buscar en nómina (nombre, RUN)..."
                                    value={studentSearchTerm}
                                    onChange={(e) => setStudentSearchTerm(e.target.value)}
                                  />
                                  {studentSearchTerm && (
                                    <button
                                      className="btn btn-outline-secondary border-start-0"
                                      type="button"
                                      onClick={() => setStudentSearchTerm('')}
                                      title="Limpiar búsqueda"
                                    >
                                      <i className="bi bi-x"></i>
                                    </button>
                                  )}
                                </div>
                              </div>
                            )}
                          </div>
                        </div>

                        {currentRoster.length === 0 ? (
                          <div className="alert alert-light border text-center py-4 text-muted mb-0">
                            <i className="bi bi-person-x fs-3 d-block mb-1"></i>
                            Actualmente no hay estudiantes inscritos en esta sección.
                          </div>
                        ) : (
                          <div className="table-responsive rounded-3 border">
                            <table className="table table-hover align-middle mb-0">
                              <thead className="table-light">
                                <tr>
                                  <th style={{ width: '40px' }} className="text-center">#</th>
                                  <th>RUN</th>
                                  <th>Estudiante</th>
                                  <th>Correo Institucional</th>
                                  <th>Carrera</th>
                                  <th className="text-end">Fecha Inscripción</th>
                                </tr>
                              </thead>
                              <tbody>
                                {filteredRoster.map((st, idx) => (
                                  <tr key={st.run || idx}>
                                    <td className="text-center text-muted small">{idx + 1}</td>
                                    <td className="font-monospace fw-semibold small text-primary">
                                      {st.run}
                                    </td>
                                    <td>
                                      <div className="fw-semibold text-dark">{st.fullName}</div>
                                    </td>
                                    <td>
                                      <a
                                        href={`mailto:${st.email}`}
                                        className="text-decoration-none small text-secondary"
                                      >
                                        <i className="bi bi-envelope me-1"></i>
                                        {st.email}
                                      </a>
                                    </td>
                                    <td>
                                      <span className="badge bg-light text-dark border">
                                        {st.careerName || 'Técnica'}
                                      </span>
                                    </td>
                                    <td className="text-end text-muted small font-monospace">
                                      {st.enrollmentDate
                                        ? new Date(st.enrollmentDate).toLocaleDateString('es-CL', {
                                          day: '2-digit',
                                          month: '2-digit',
                                          year: 'numeric',
                                          hour: '2-digit',
                                          minute: '2-digit',
                                        })
                                        : '-'}
                                    </td>
                                  </tr>
                                ))}
                                {filteredRoster.length === 0 && (
                                  <tr>
                                    <td colSpan="6" className="text-center py-3 text-muted">
                                      No hay alumnos en la nómina que coincidan con "{studentSearchTerm}".
                                    </td>
                                  </tr>
                                )}
                              </tbody>
                            </table>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default TeacherSectionsPage;