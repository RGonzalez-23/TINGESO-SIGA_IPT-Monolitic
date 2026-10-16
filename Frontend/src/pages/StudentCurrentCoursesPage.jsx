import { useEffect, useState, useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import enrollmentService from '../services/enrollment.service';
import studentService from '../services/student.service';

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

const StudentCurrentCoursesPage = () => {
  const { run } = useParams();
  const [targetStudent, setTargetStudent] = useState(null);
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('list'); // 'list' | 'schedule'

  useEffect(() => {
    const fetchCurrentCourses = async () => {
      try {
        setLoading(true);
        if (run) {
          const [enrollmentsRes, studentRes] = await Promise.all([
            enrollmentService.getStudentActiveEnrollments(run),
            studentService.getByRun(run).catch(() => ({ data: null })),
          ]);
          setEnrollments(enrollmentsRes.data || []);
          setTargetStudent(studentRes.data);
        } else {
          const res = await enrollmentService.getMyEnrollments();
          setEnrollments(res.data || []);
        }
      } catch (err) {
        console.error('Error fetching current courses:', err);
        setError('No fue posible cargar las asignaturas inscritas actualmente.');
      } finally {
        setLoading(false);
      }
    };

    fetchCurrentCourses();
  }, [run]);

  const totalCredits = useMemo(
    () => enrollments.reduce((sum, e) => sum + (e.sctCredits || 0), 0),
    [enrollments]
  );

  // Map schedule grid: key "L1" -> enrollment object
  const scheduleGrid = useMemo(() => {
    const grid = {};
    enrollments.forEach((e) => {
      (e.scheduleTimeBlocks || []).forEach((block) => {
        grid[block] = e;
      });
    });
    return grid;
  }, [enrollments]);

  return (
    <div className="container py-4">
      {/* Admin Back Link if viewing a specific student */}
      {run && (
        <div className="mb-3">
          <Link to={`/students/${run}`} className="btn btn-outline-secondary btn-sm rounded-3">
            <i className="bi bi-arrow-left me-1"></i>
            Volver a la ficha de {targetStudent?.fullName || run}
          </Link>
        </div>
      )}

      {/* Header Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm"
        style={{
          background: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%)',
        }}
      >
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 mb-3 text-black small fw-semibold">
              <i className="bi bi-calendar-check-fill"></i>
              <span>Período Académico Activo &bull; IPT</span>
            </div>
            <h1 className="display-6 fw-bold mb-2">
              {run ? `Asignaturas en Curso • ${targetStudent?.fullName || run}` : 'Mis Asignaturas en Curso'}
            </h1>
            <p className="lead mb-0 text-white-75 fs-6">
              {run
                ? `Horarios de clases, secciones inscritas y distribución semanal de ${targetStudent?.fullName || 'estudiante'} (RUN: ${run}).`
                : 'Revisa los horarios de tus clases, secciones asignadas y distribución de módulos semanales.'}
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <div className="bg-white bg-opacity-15 backdrop-blur px-4 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{enrollments.length}</div>
              <div className="small text-black">Asignaturas</div>
            </div>
            <div className="bg-white bg-opacity-15 backdrop-blur px-4 py-2 rounded-3 text-center border border-white border-opacity-25">
              <div className="fs-4 fw-bold text-black">{totalCredits}</div>
              <div className="small text-black">Créditos SCT</div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs and Actions */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div className="btn-group p-1 bg-light rounded-3 border" role="group">
          <button
            type="button"
            className={`btn btn-sm rounded-2 fw-semibold px-3 ${activeTab === 'list' ? 'btn-primary shadow-sm text-white' : 'btn-light text-muted'
              }`}
            onClick={() => setActiveTab('list')}
          >
            <i className="bi bi-list-ul me-2"></i>
            Lista de Asignaturas ({enrollments.length})
          </button>
          <button
            type="button"
            className={`btn btn-sm rounded-2 fw-semibold px-3 ${activeTab === 'schedule' ? 'btn-primary shadow-sm text-white' : 'btn-light text-muted'
              }`}
            onClick={() => setActiveTab('schedule')}
          >
            <i className="bi bi-calendar3 me-2"></i>
            Horario Semanal
          </button>
        </div>

        <Link to="/enrollment" className="btn btn-outline-primary btn-sm fw-semibold rounded-3">
          <i className="bi bi-pencil-square me-2"></i>
          Modificar Inscripción
        </Link>
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
          <p className="text-muted mt-2">Cargando tus asignaturas en curso...</p>
        </div>
      ) : enrollments.length === 0 ? (
        <div className="card border-0 shadow-sm rounded-4 text-center py-5 p-4">
          <i className="bi bi-inbox fs-1 text-muted mb-2"></i>
          <h5 className="fw-bold text-dark">No tienes asignaturas inscritas en este período</h5>
          <p className="text-muted small mb-3">
            Accede al módulo de inscripción para seleccionar las asignaturas disponibles para tu plan.
          </p>
          <div>
            <Link to="/enrollment" className="btn btn-primary fw-bold px-4 py-2 rounded-3">
              <i className="bi bi-pencil-square me-2"></i>
              Ir a Inscripción Académica
            </Link>
          </div>
        </div>
      ) : activeTab === 'list' ? (
        /* List View */
        <div className="row g-3">
          {enrollments.map((item) => (
            <div className="col-12" key={item.id}>
              <div className="card border-0 shadow-sm rounded-4 overflow-hidden border-start border-4 border-primary">
                <div className="card-body p-4">
                  <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                    <div className="flex-grow-1">
                      <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
                        <span className="badge bg-secondary bg-opacity-10 text-dark border">
                          Semestre {item.semester}
                        </span>
                        <span className="badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25 font-monospace">
                          {item.courseCode}
                        </span>
                        <span className="badge bg-light text-dark border">
                          {item.sctCredits} SCT
                        </span>
                        <span className="badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 rounded-pill fw-semibold">
                          <i className="bi bi-check-circle-fill me-1"></i>
                          INSCRITA
                        </span>
                      </div>

                      <h4 className="fw-bold text-dark mb-1">{item.courseName}</h4>

                      <div className="small text-muted mb-3">
                        <strong className="text-dark font-monospace me-2">{item.sectionCode}</strong>
                        &bull; Docente: {item.teacherName || 'Docente asignado'}
                      </div>

                      <div className="d-flex flex-wrap align-items-center gap-2">
                        <span className="text-muted small me-1">Horario Módulos:</span>
                        {(item.scheduleTimeBlocks || []).map((b) => (
                          <span
                            key={b}
                            className="badge bg-light text-primary border font-monospace px-2 py-1"
                          >
                            {b}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        /* Schedule Matrix View */
        <div className="card border-0 shadow-sm rounded-4 overflow-hidden">
          <div className="card-header bg-white p-3 border-bottom d-flex justify-content-between align-items-center">
            <h6 className="fw-bold mb-0 text-dark">
              <i className="bi bi-calendar-week-fill text-primary me-2"></i>
              Malla Horaria de Clases Semanales
            </h6>
            <span className="text-muted small">6 módulos diarios estandarizados</span>
          </div>
          <div className="table-responsive">
            <table
              className="table table-bordered mb-0 text-center align-middle schedule-matrix-table"
              style={{ minWidth: '700px' }}
            >
              <thead className="table-light">
                <tr>
                  <th style={{ width: '130px' }}>Módulo</th>
                  {DAYS.map((d) => (
                    <th key={d.key} className="py-2">
                      <span className="fw-bold">{d.label}</span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {MODULES.map((m) => (
                  <tr key={m}>
                    <td className="bg-light p-2">
                      <div className="fw-bold text-dark">Módulo {m}</div>
                      <div className="small text-muted font-monospace">{MODULE_TIMES[m]}</div>
                    </td>
                    {DAYS.map((d) => {
                      const code = `${d.key}${m}`;
                      const enr = scheduleGrid[code];

                      return (
                        <td
                          key={code}
                          className="p-2"
                          style={{
                            verticalAlign: 'middle',
                            backgroundColor: enr ? '#eff6ff' : 'white',
                            height: '80px',
                          }}
                        >
                          {enr ? (
                            <div className="p-2 rounded-3 bg-white border border-primary border-opacity-25 shadow-2xs text-start">
                              <span className="badge bg-primary font-monospace me-1" style={{ fontSize: '11px' }}>
                                {enr.courseCode}
                              </span>
                              <div className="fw-bold text-dark mt-1 text-truncate" style={{ fontSize: '12px' }}>
                                {enr.courseName}
                              </div>
                              <div className="text-muted text-truncate font-monospace" style={{ fontSize: '11px' }}>
                                {enr.sectionCode}
                              </div>
                            </div>
                          ) : (
                            <span className="text-muted small opacity-25 font-monospace">{code}</span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default StudentCurrentCoursesPage;
