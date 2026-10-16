import { useEffect, useState } from 'react';
import academicHistoryService from '../services/academic-history.service';

const StudentAcademicHistoryPage = () => {
  const [historyData, setHistoryData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        setLoading(true);
        const res = await academicHistoryService.getMyAcademicHistory();
        setHistoryData(res.data);
      } catch (err) {
        console.error('Error fetching academic history:', err);
        setError('No fue posible cargar tu historial académico.');
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, []);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'APROBADA':
        return (
          <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-2 py-1 rounded-pill fw-semibold">
            <i className="bi bi-check-circle-fill me-1"></i> Aprobada
          </span>
        );
      case 'REPROBADA':
        return (
          <span className="badge bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 px-2 py-1 rounded-pill fw-semibold">
            <i className="bi bi-x-circle-fill me-1"></i> Reprobada
          </span>
        );
      case 'INSCRITA':
        return (
          <span className="badge bg-warning bg-opacity-15 text-dark border border-warning px-2 py-1 rounded-pill fw-semibold">
            <i className="bi bi-hourglass-split me-1"></i> En Curso
          </span>
        );
      case 'ANULADA':
        return (
          <span className="badge bg-secondary bg-opacity-10 text-muted border px-2 py-1 rounded-pill">
            Anulada
          </span>
        );
      default:
        return <span className="badge bg-light text-dark border">{status}</span>;
    }
  };

  const getAcademicStatusBadge = (status) => {
    switch (status) {
      case 'REGULAR':
        return <span className="badge bg-success px-3 py-2 fs-6">REGULAR</span>;
      case 'EGRESADO':
        return <span className="badge bg-info px-3 py-2 fs-6 text-dark fw-bold">EGRESADO</span>;
      case 'ELIMINADO':
        return <span className="badge bg-danger px-3 py-2 fs-6">ELIMINADO</span>;
      case 'POSTERGACION':
        return <span className="badge bg-warning px-3 py-2 fs-6 text-dark">POSTERGACIÓN</span>;
      case 'RETIRO_TEMPORAL':
        return <span className="badge bg-secondary px-3 py-2 fs-6">RETIRO TEMPORAL</span>;
      default:
        return <span className="badge bg-light text-dark border">{status}</span>;
    }
  };

  const periods = historyData?.periods || [];

  return (
    <div className="container py-4">
      {/* Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 60%, #4338ca 100%)',
        }}
      >
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-dark small fw-semibold">
              <i className="bi bi-clock-history"></i>
              <span>Expediente Académico IPT</span>
            </div>
            <h1 className="display-6 fw-bold mb-2">Mi Historial Académico</h1>
            <p className="lead mb-0 text-white-75 fs-6">
              {historyData?.studentFullName || 'Estudiante'} &bull; RUN: {historyData?.studentRun} &bull; Carrera: {historyData?.careerName}
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center text-black border border-white border-opacity-25">
              <div className="fs-4 fw-bold">
                {historyData?.cumulativeGpa ? historyData.cumulativeGpa.toFixed(1) : '-'}
              </div>
              <div className="small text-black">Promedio Acumulado</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center text-black border border-white border-opacity-25">
              <div className="fs-4 fw-bold">
                {historyData?.totalApprovedCredits || 0} / {historyData?.totalPlanCredits || 0}
              </div>
              <div className="small text-black">Créditos SCT</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-3 py-2 rounded-3 text-center border border-white border-opacity-25 d-flex flex-column justify-content-center">
              <div className="small text-black mb-1">Situación Académica</div>
              <div>{getAcademicStatusBadge(historyData?.academicStatus)}</div>
            </div>
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
          <div className="spinner-border text-primary" role="status"></div>
          <p className="text-muted mt-2">Cargando tu historial académico completo...</p>
        </div>
      ) : periods.length === 0 ? (
        <div className="card border-0 shadow-sm rounded-4 text-center py-5 p-4">
          <i className="bi bi-folder2-open fs-1 text-muted mb-2"></i>
          <h5 className="fw-bold text-dark">No posees registros académicos aún</h5>
          <p className="text-muted small mb-0">
            Tus calificaciones y asignaturas cursadas se visualizarán aquí una vez que inscribas y cierren los períodos.
          </p>
        </div>
      ) : (
        /* Periods Chronological List */
        <div className="d-flex flex-column gap-4">
          {periods.map((period) => (
            <div className="card border-0 shadow-sm rounded-4 overflow-hidden" key={period.periodId}>
              {/* Period Header */}
              <div className="card-header bg-white p-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
                <div className="d-flex align-items-center gap-2">
                  <span className="badge bg-primary fs-6 px-3 py-1 font-monospace">
                    Período {period.periodId}
                  </span>
                  <span className="fw-bold text-dark">
                    Año {period.year} &bull; Semestre {period.semester}
                  </span>
                  {period.isOpen ? (
                    <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25">
                      <i className="bi bi-check-circle-fill me-1"></i> Período Abierto
                    </span>
                  ) : (
                    <span className="badge bg-secondary bg-opacity-10 text-secondary border">
                      <i className="bi bi-lock-fill me-1"></i> Período Cerrado
                    </span>
                  )}
                </div>

                <div className="d-flex align-items-center gap-3 small">
                  <div>
                    <span className="text-muted me-1">Créditos aprobados:</span>
                    <strong className="text-dark">
                      {period.approvedCredits} / {period.enrolledCredits} SCT
                    </strong>
                  </div>
                  {period.periodGpa && (
                    <div className="border-start ps-3">
                      <span className="text-muted me-1">Promedio Período:</span>
                      <span className="badge bg-dark font-monospace fs-6 px-2 py-1">
                        {period.periodGpa.toFixed(1)}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Table of Courses */}
              <div className="card-body p-0">
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead className="table-light">
                      <tr>
                        <th style={{ width: '120px' }}>Código</th>
                        <th>Asignatura</th>
                        <th>Sección</th>
                        <th>Docente</th>
                        <th className="text-center">SCT</th>
                        <th className="text-center" style={{ width: '100px' }}>Nota Final</th>
                        <th className="text-end" style={{ width: '150px' }}>Resultado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {period.courses.map((c) => (
                        <tr key={c.enrollmentId}>
                          <td className="font-monospace fw-bold text-primary">{c.courseCode}</td>
                          <td>
                            <div className="fw-semibold text-dark">{c.courseName}</div>
                            <span className="text-muted small">Semestre {c.semester}</span>
                          </td>
                          <td>
                            <span className="badge bg-light text-dark border font-monospace">
                              {c.sectionCode}
                            </span>
                          </td>
                          <td className="small text-muted">{c.teacherName}</td>
                          <td className="text-center fw-semibold">{c.sctCredits}</td>
                          <td className="text-center">
                            {c.finalGrade != null ? (
                              <span
                                className={`badge font-monospace fs-6 px-2 py-1 ${c.finalGrade >= 4.0
                                  ? 'bg-success text-white'
                                  : 'bg-danger text-white'
                                  }`}
                              >
                                {c.finalGrade.toFixed(1)}
                              </span>
                            ) : (
                              <span className="text-muted font-monospace small">Pendiente</span>
                            )}
                          </td>
                          <td className="text-end">{getStatusBadge(c.status)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default StudentAcademicHistoryPage;
