import { useEffect, useState } from 'react';
import sectionRosterService from '../services/section-roster.service';

const TeacherSectionsPage = () => {
  const [sections, setSections] = useState([]);
  const [rosters, setRosters] = useState({});
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    sectionRosterService.getMySections().then((response) => setSections(response.data || []))
      .catch((err) => setError(err.response?.data?.message || 'No fue posible cargar tus secciones.'))
      .finally(() => setLoading(false));
  }, []);

  const toggleRoster = async (sectionId) => {
    if (expanded === sectionId) { setExpanded(null); return; }
    try {
      if (!rosters[sectionId]) {
        const response = await sectionRosterService.getEnrolledStudents(sectionId);
        setRosters((current) => ({ ...current, [sectionId]: response.data || [] }));
      }
      setExpanded(sectionId);
    } catch (err) { setError(err.response?.data?.message || 'No fue posible cargar la nómina.'); }
  };

  if (loading) return <div className="container py-5 text-center"><div className="spinner-border text-primary" role="status" /></div>;
  return <div className="container py-4"><div className="mb-4"><h1 className="h3 fw-bold mb-1">Mis Secciones</h1><p className="text-muted mb-0">Consulta tus cursos asignados y la nómina de estudiantes inscritos.</p></div>{error && <div className="alert alert-danger">{error}</div>}{sections.length === 0 ? <div className="siga-card p-4 text-muted">No tienes secciones asignadas.</div> : <div className="row g-4">{sections.map((section) => <div className="col-12" key={section.id}><div className="siga-card"><div className="card-body p-4"><div className="d-flex flex-wrap justify-content-between align-items-center gap-3"><div><span className="badge bg-primary me-2">{section.sectionCode}</span><strong>{section.courseCode} · {section.courseName}</strong><div className="small text-muted mt-2"><i className="bi bi-calendar-week me-1" />{section.academicPeriodId} · {section.timeBlocks?.map((block) => block.code || block).join(', ') || 'Sin horario'}</div></div><button className="btn btn-outline-primary btn-sm" onClick={() => toggleRoster(section.id)}><i className="bi bi-people me-1" />{expanded === section.id ? 'Ocultar nómina' : 'Ver nómina'}</button></div>{expanded === section.id && <div className="table-responsive mt-4"><table className="table table-sm align-middle mb-0"><thead><tr><th>RUN</th><th>Estudiante</th><th>Correo</th><th>Fecha inscripción</th></tr></thead><tbody>{(rosters[section.id] || []).map((student) => <tr key={student.run}><td className="font-monospace">{student.run}</td><td>{student.fullName}</td><td>{student.email}</td><td>{student.enrollmentDate ? new Date(student.enrollmentDate).toLocaleDateString('es-CL') : '-'}</td></tr>)}</tbody></table>{rosters[section.id]?.length === 0 && <p className="text-muted small mt-3 mb-0">No hay estudiantes inscritos.</p>}</div>}</div></div></div>)}</div>}</div>;
};

export default TeacherSectionsPage;