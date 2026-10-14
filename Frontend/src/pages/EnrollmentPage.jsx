import { useCallback, useEffect, useMemo, useState } from 'react';
import enrollmentService from '../services/enrollment.service';

const getErrorMessage = (error, fallback) =>
  error.response?.data?.message || error.response?.data?.error || fallback;

const DAYS = ['L', 'M', 'W', 'J', 'V'];
const MODULES = [1, 2, 3, 4, 5, 6];

const EnrollmentPage = () => {
  const [offer, setOffer] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [alert, setAlert] = useState(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [offerResponse, enrollmentResponse] = await Promise.all([
        enrollmentService.getAvailableOffer(),
        enrollmentService.getMyEnrollments(),
      ]);
      setOffer(offerResponse.data || []);
      setEnrollments(enrollmentResponse.data || []);
      setAlert(null);
    } catch (error) {
      setAlert({ type: 'danger', message: getErrorMessage(error, 'No fue posible cargar la inscripción académica.') });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const handleRegister = async (sectionId) => {
    try {
      setBusyId(sectionId);
      await enrollmentService.register(undefined, [sectionId]);
      setAlert({ type: 'success', message: 'Asignatura inscrita correctamente.' });
      await loadData();
    } catch (error) {
      setAlert({ type: 'danger', message: getErrorMessage(error, 'No fue posible inscribir la asignatura.') });
    } finally {
      setBusyId(null);
    }
  };

  const handleCancel = async (enrollment) => {
    if (!window.confirm(`¿Anular la inscripción de ${enrollment.courseCode}?`)) return;
    try {
      setBusyId(enrollment.id);
      await enrollmentService.cancel(enrollment.id);
      setAlert({ type: 'success', message: 'Inscripción anulada y cupo liberado.' });
      await loadData();
    } catch (error) {
      setAlert({ type: 'danger', message: getErrorMessage(error, 'No fue posible anular la inscripción.') });
    } finally {
      setBusyId(null);
    }
  };

  const selectedCodes = useMemo(() => new Set(enrollments.map((item) => item.courseCode)), [enrollments]);
  const sortedOffer = useMemo(() => [...offer].sort((a, b) => a.semester - b.semester || a.courseCode.localeCompare(b.courseCode)), [offer]);
  const schedule = useMemo(() => enrollments.reduce((result, item) => {
    (item.scheduleTimeBlocks || []).forEach((block) => {
      const match = String(block).match(/^([LMWJV])(\d)$/);
      if (match) result[`${match[1]}${match[2]}`] = item.courseCode;
    });
    return result;
  }, {}), [enrollments]);

  if (loading) return <div className="container py-5 text-center"><div className="spinner-border text-primary" role="status" /><p className="mt-2 text-muted">Cargando oferta de inscripción...</p></div>;

  return (
    <div className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div><h1 className="h3 fw-bold mb-1">Inscripción Académica</h1><p className="text-muted mb-0">Selecciona tus asignaturas del período abierto.</p></div>
        <span className="badge bg-primary fs-6 px-3 py-2">{enrollments.length} inscritas</span>
      </div>
      {alert && <div className={`alert alert-${alert.type} alert-dismissible`} role="alert">{alert.message}<button type="button" className="btn-close" onClick={() => setAlert(null)} /></div>}

      <div className="row g-4">
        <div className="col-lg-8">
          <div className="siga-card">
            <div className="siga-card-header"><i className="bi bi-book-half me-2" />Oferta disponible</div>
            <div className="card-body p-0">
              {sortedOffer.length === 0 && <p className="p-4 text-muted mb-0">No hay asignaturas disponibles para tu situación académica.</p>}
              <div className="list-group list-group-flush">
                {sortedOffer.map((item) => {
                  const alreadySelected = selectedCodes.has(item.courseCode) || item.isCurrentlyEnrolled;
                  const canSelect = item.isSelectable && !alreadySelected && item.sectionId;
                  return (
                    <div className="list-group-item p-4" key={`${item.courseId}-${item.sectionId}`}>
                      <div className="d-flex justify-content-between gap-3">
                        <div className="flex-grow-1">
                          <div className="d-flex flex-wrap align-items-center gap-2 mb-1"><span className="badge bg-light text-dark border">Semestre {item.semester}</span><strong>{item.courseCode} · {item.courseName}</strong></div>
                          <div className="small text-muted mb-2">{item.teacherName || 'Docente por confirmar'} · {item.sectionCode || 'Sin sección'}</div>
                          <div className="small d-flex flex-wrap gap-3"><span><i className="bi bi-people me-1" />{item.availableQuota ?? 0} cupos</span><span><i className="bi bi-clock me-1" />{item.timeBlocks?.join(', ') || 'Sin horario'}</span><span><i className="bi bi-award me-1" />{item.sctCredits} SCT</span></div>
                          {item.prerequisiteCodes?.length > 0 && <div className="small text-muted mt-2">Prerrequisitos: {item.prerequisiteCodes.join(', ')}</div>}
                          {!item.isSelectable && item.ineligibilityReason && <div className="alert alert-warning py-2 px-3 mt-3 mb-0 small"><i className="bi bi-info-circle me-1" />{item.ineligibilityReason}</div>}
                        </div>
                        <div className="text-end">
                          {alreadySelected ? <span className="badge bg-success">Inscrita</span> : <button className="btn btn-primary btn-sm" disabled={!canSelect || busyId === item.sectionId} onClick={() => handleRegister(item.sectionId)}>{busyId === item.sectionId ? <span className="spinner-border spinner-border-sm" /> : <><i className="bi bi-plus-circle me-1" />Inscribir</>}</button>}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
        <div className="col-lg-4">
          <div className="siga-card sticky-lg-top" style={{ top: '5rem' }}>
            <div className="siga-card-header"><i className="bi bi-calendar-week me-2" />Mis inscripciones</div>
            <div className="card-body p-3">
              {enrollments.length === 0 ? <p className="text-muted small mb-0">Aún no tienes asignaturas inscritas.</p> : enrollments.map((item) => <div className="border-bottom py-3" key={item.id}><div className="d-flex justify-content-between gap-2"><strong className="small">{item.courseCode}</strong><button className="btn btn-outline-danger btn-sm" title="Anular inscripción" disabled={busyId === item.id} onClick={() => handleCancel(item)}><i className="bi bi-trash" /></button></div><div className="small">{item.courseName}</div><div className="small text-muted">{item.scheduleTimeBlocks?.join(', ') || 'Sin horario'}</div></div>)}
            </div>
          </div>
          <div className="siga-card mt-4">
            <div className="siga-card-header"><i className="bi bi-grid-3x3-gap me-2" />Horario semanal</div>
            <div className="card-body p-2">
              <div className="table-responsive">
                <table className="table table-bordered table-sm text-center align-middle mb-0" style={{ tableLayout: 'fixed' }}>
                  <thead><tr><th className="small">Mód.</th>{DAYS.map((day) => <th key={day} className="small">{day}</th>)}</tr></thead>
                  <tbody>{MODULES.map((module) => <tr key={module}><th className="small text-muted">M{module}</th>{DAYS.map((day) => <td key={day} className={schedule[`${day}${module}`] ? 'table-primary fw-semibold small' : 'text-muted small'}>{schedule[`${day}${module}`] || '-'}</td>)}</tr>)}</tbody>
                </table>
              </div>
              <p className="text-muted small mt-2 mb-0">L: lunes · M: martes · W: miércoles · J: jueves · V: viernes.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnrollmentPage;