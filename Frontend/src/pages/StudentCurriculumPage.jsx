import { useEffect, useState, useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import academicHistoryService from '../services/academic-history.service';

const StudentCurriculumPage = () => {
  const { run } = useParams();
  const [progressData, setProgressData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedSemester, setSelectedSemester] = useState('ALL');

  useEffect(() => {
    const fetchCurriculum = async () => {
      try {
        setLoading(true);
        const res = run
          ? await academicHistoryService.getStudentCurriculumProgress(run)
          : await academicHistoryService.getMyCurriculumProgress();
        setProgressData(res.data);
      } catch (err) {
        console.error('Error fetching curriculum progress:', err);
        setError('No fue posible cargar la malla curricular con el avance actual.');
      } finally {
        setLoading(false);
      }
    };

    fetchCurriculum();
  }, [run]);

  const courses = useMemo(() => progressData?.courses || [], [progressData]);

  const filteredCourses = useMemo(() => {
    if (selectedSemester === 'ALL') return courses;
    return courses.filter((c) => c.semester === Number(selectedSemester));
  }, [courses, selectedSemester]);

  const semesters = [1, 2, 3, 4];

  const getStatusBadge = (status, failCount, grade) => {
    switch (status) {
      case 'APPROVED':
        return (
          <span className="badge bg-success bg-opacity-15 text-success border border-success border-opacity-25 px-2 py-1 text-white rounded-pill fw-semibold">
            <i className="bi bi-check-circle-fill me-1"></i>
            Aprobada {grade ? `(${grade.toFixed(1)})` : ''}
          </span>
        );
      case 'FAILED':
        return (
          <span className="badge bg-danger bg-opacity-15 text-danger border border-danger border-opacity-25 px-2 py-1 text-white rounded-pill fw-semibold">
            <i className="bi bi-x-circle-fill me-1"></i>
            Reprobada {failCount > 0 ? `(${failCount} ${failCount === 1 ? 'vez' : 'veces'})` : ''}
          </span>
        );
      case 'ENROLLED':
        return (
          <span className="badge bg-warning bg-opacity-25 text-dark border border-warning px-2 py-1 rounded-pill fw-semibold">
            <i className="bi bi-hourglass-split me-1"></i>
            Inscrita en Curso
          </span>
        );
      default:
        return (
          <span className="badge bg-light text-muted border px-2 py-1 rounded-pill">
            Pendiente
          </span>
        );
    }
  };

  const getCardStyle = (status) => {
    switch (status) {
      case 'APPROVED':
        return {
          backgroundColor: '#f0fdf4', // Verde suave
          borderColor: '#86efac',
          borderLeft: '5px solid #16a34a',
        };
      case 'FAILED':
        return {
          backgroundColor: '#fef2f2', // Rojo suave
          borderColor: '#fca5a5',
          borderLeft: '5px solid #dc2626',
        };
      case 'ENROLLED':
        return {
          backgroundColor: '#fffbeb', // Amarillo suave
          borderColor: '#fde68a',
          borderLeft: '5px solid #d97706',
        };
      default:
        return {
          backgroundColor: '#ffffff', // Blanco
          borderColor: '#e2e8f0',
          borderLeft: '5px solid #cbd5e1',
        };
    }
  };

  const progressPercentage = useMemo(() => {
    if (!progressData || !progressData.totalPlanCoursesCount) return 0;
    return Math.round(
      (progressData.approvedCoursesCount / progressData.totalPlanCoursesCount) * 100
    );
  }, [progressData]);

  return (
    <div className="container py-4">
      {/* Admin Back Link if viewing a specific student */}
      {run && (
        <div className="mb-3">
          <Link to={`/students/${run}`} className="btn btn-outline-secondary btn-sm rounded-3">
            <i className="bi bi-arrow-left me-1"></i>
            Volver a la ficha de {progressData?.studentFullName || run}
          </Link>
        </div>
      )}

      {/* Header Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #065f46 0%, #059669 60%, #10b981 100%)',
        }}
      >
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-black small fw-semibold">
              <i className="bi bi-diagram-3-fill"></i>
              <span>Plan de Estudios {progressData?.studyPlanCode || '2021/2022'}</span>
            </div>
            <h1 className="display-6 fw-bold mb-2">
              {run ? `Malla Curricular • ${progressData?.studentFullName || run}` : 'Mi Malla Curricular'}
            </h1>
            <p className="lead mb-0 text-white-75 fs-6">
              {progressData?.careerName || 'Carrera Técnica IPT'} &bull; {run ? `Estudiante: ${progressData?.studentFullName || run} (RUN: ${run})` : 'Visualiza tu avance formativo, asignaturas aprobadas, en curso y prerrequisitos.'}
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{progressData?.approvedCoursesCount || 0} / {progressData?.totalPlanCoursesCount || 0}</div>
              <div className="small text-black">Aprobadas ({progressPercentage}%)</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{progressData?.enrolledCoursesCount || 0}</div>
              <div className="small text-black">En Curso</div>
            </div>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="mt-4">
          <div className="d-flex justify-content-between small text-white-75 mb-1">
            <span>Progreso de Titulación:</span>
            <span>{progressPercentage}% completado</span>
          </div>
          <div className="progress bg-white bg-opacity-25" style={{ height: '8px' }}>
            <div
              className="progress-bar bg-white"
              role="progressbar"
              style={{ width: `${progressPercentage}%` }}
              aria-valuenow={progressPercentage}
              aria-valuemin="0"
              aria-valuemax="100"
            ></div>
          </div>
        </div>
      </div>

      {/* Color Legend (as requested) */}
      <div className="card border-0 shadow-sm rounded-4 p-3 mb-4 bg-white">
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div className="d-flex flex-wrap align-items-center gap-3 small">
            <span className="fw-bold text-dark me-1">Leyenda de Estados:</span>
            <span className="badge bg-success bg-opacity-15 text-white border border-success border-opacity-25 px-2 py-1 rounded-pill">
              <i className="bi bi-check-circle-fill me-1"></i> Aprobada
            </span>
            <span className="badge bg-danger bg-opacity-15 text-white border border-danger border-opacity-25 px-2 py-1 rounded-pill">
              <i className="bi bi-x-circle-fill me-1"></i> Reprobada con n° repitencias
            </span>
            <span className="badge bg-warning bg-opacity-25 text-black border border-warning px-2 py-1 rounded-pill">
              <i className="bi bi-hourglass-split me-1"></i> Inscrita en Curso
            </span>
            <span className="badge bg-white text-muted border px-2 py-1 rounded-pill">
              <i className="bi bi-circle me-1"></i> Pendiente
            </span>
          </div>

          {/* Semester Filter */}
          <div className="d-flex align-items-center gap-2">
            <label className="small text-muted fw-semibold">Semestre:</label>
            <select
              className="form-select form-select-sm rounded-3"
              style={{ width: 'auto' }}
              value={selectedSemester}
              onChange={(e) => setSelectedSemester(e.target.value)}
            >
              <option value="ALL">Todos los semestres (1 al 4)</option>
              <option value="1">Semestre 1</option>
              <option value="2">Semestre 2</option>
              <option value="3">Semestre 3</option>
              <option value="4">Semestre 4</option>
            </select>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger py-3 rounded-4 shadow-sm mb-4">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border text-success" role="status"></div>
          <p className="text-muted mt-2">Cargando malla curricular interactiva...</p>
        </div>
      ) : (
        /* Semester-by-semester layout */
        <div className="row g-4">
          {semesters
            .filter((sem) => selectedSemester === 'ALL' || Number(selectedSemester) === sem)
            .map((sem) => {
              const semCourses = filteredCourses.filter((c) => c.semester === sem);
              if (semCourses.length === 0) return null;

              return (
                <div className="col-12" key={sem}>
                  <div className="d-flex align-items-center gap-2 mb-3">
                    <span className="badge bg-dark fs-6 px-3 py-1 font-monospace">
                      Semestre {sem}
                    </span>
                    <span className="text-muted small">
                      {semCourses.length} {semCourses.length === 1 ? 'asignatura' : 'asignaturas'} &bull;{' '}
                      {semCourses.reduce((sum, c) => sum + (c.sctCredits || 0), 0)} SCT
                    </span>
                    <div className="flex-grow-1 border-bottom ms-2"></div>
                  </div>

                  <div className="row g-3">
                    {semCourses.map((course) => {
                      const cardStyle = getCardStyle(course.status);

                      return (
                        <div className="col-12 col-md-6 col-lg-4" key={course.courseId}>
                          <div
                            className="card h-100 rounded-4 shadow-sm transition-all"
                            style={cardStyle}
                          >
                            <div className="card-body p-3 d-flex flex-column">
                              {/* Top Bar: Code and Status Badge */}
                              <div className="d-flex justify-content-between align-items-center mb-2">
                                <span className="badge bg-white text-dark border font-monospace fw-bold">
                                  {course.code}
                                </span>
                                {getStatusBadge(course.status, course.failCount, course.latestGrade)}
                              </div>

                              {/* Course Name */}
                              <h6 className="fw-bold text-dark mb-2 flex-grow-1">
                                {course.name}
                              </h6>

                              {/* SCT & TEL Hours */}
                              <div className="d-flex flex-wrap gap-1 mb-2 small">
                                <span className="badge bg-white text-primary border">
                                  {course.sctCredits} SCT
                                </span>
                                <span className="badge bg-white text-muted border">
                                  TEL: {course.theoryHours}T-{course.exerciseHours}E-{course.laboratoryHours}L
                                </span>
                              </div>

                              {/* Prerequisites (as requested) */}
                              <div className="pt-2 border-top mt-auto small">
                                <span className="text-muted d-block mb-1" style={{ fontSize: '0.75rem' }}>
                                  <i className="bi bi-link-45deg me-1"></i>
                                  Prerrequisitos:
                                </span>
                                {course.prerequisites && course.prerequisites.length > 0 ? (
                                  <div className="d-flex flex-wrap gap-1">
                                    {course.prerequisites.map((p) => (
                                      <span
                                        key={p.code}
                                        className="badge bg-white text-dark border font-monospace"
                                        title={p.name}
                                        style={{ fontSize: '0.72rem' }}
                                      >
                                        {p.code}
                                      </span>
                                    ))}
                                  </div>
                                ) : (
                                  <span className="text-muted fst-italic" style={{ fontSize: '0.75rem' }}>
                                    Sin prerrequisitos
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
        </div>
      )}
    </div>
  );
};

export default StudentCurriculumPage;
