import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import studyPlanService from '../services/study-plan.service';
import studentService from '../services/student.service';
import { useAuth } from '../context/AuthContext';

/**
 * StudyPlanDetailPage provides dedicated viewing and management of an individual study plan.
 * - ADMIN: Can view and manage the study plan and curriculum.
 * - TEACHER: Can view the study plan in read-only mode.
 * - STUDENT: Can only access their assigned study plan (via "Mi malla"), in read-only mode.
 */
const StudyPlanDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAdmin, isTeacher, isStudent, currentUserRun } = useAuth();

  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [accessDenied, setAccessDenied] = useState(false);
  const [myPlanId, setMyPlanId] = useState(null);

  // Selected semester tab (1 to 4)
  const [activeSemester, setActiveSemester] = useState(1);
  const [actionSuccess, setActionSuccess] = useState(null);

  useEffect(() => {
    const loadPlanAndValidateAccess = async () => {
      setLoading(true);
      setError(null);
      setAccessDenied(false);

      try {
        // If current user is a STUDENT, verify they can only view their own assigned study plan
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

        // Fetch study plan details
        const response = await studyPlanService.getStudyPlanById(id);
        setPlan(response.data);
      } catch (err) {
        console.error('Error loading study plan:', err);
        setError('No fue posible cargar la información del plan de estudios solicitado.');
      } finally {
        setLoading(false);
      }
    };

    loadPlanAndValidateAccess();
  }, [id, isStudent, currentUserRun]);

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
      setActionSuccess('¡El plan ha sido activado como vigente exitosamente!');
      setTimeout(() => setActionSuccess(null), 4000);
    } catch (err) {
      console.error('Error activating plan:', err);
      alert('Error al activar el plan de estudios.');
    }
  };

  // Case 1: Loading
  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status"></div>
        <p className="mt-3 text-muted">Cargando malla curricular y detalles del plan...</p>
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
          {isStudent ? 'Vista del Estudiante (Solo Lectura)' : isTeacher ? 'Vista Docente (Consulta)' : 'Modo Administrador'}
        </span>
      </div>

      {actionSuccess && (
        <div className="alert alert-success alert-dismissible fade show rounded-3 shadow-sm border-0 mb-4" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {actionSuccess}
        </div>
      )}

      {/* Plan Header Card */}
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

            <h1 className="display-6 fw-bold mb-2">
              {isStudent ? 'Mi Malla Curricular' : 'Plan de Estudios'}
            </h1>
            <h1 className="display-6 fw-bold mb-2">
              {plan.careerName}
            </h1>
            <h5 className="text-white-50 mb-0">
              Versión {plan.code}
            </h5>
          </div>

          <div className="col-lg-4 text-lg-end">
            <div className="d-inline-flex flex-column align-items-lg-end gap-2 bg-white bg-opacity-10 p-3 rounded-3 backdrop-blur">
              <div className="small text-50">
                <i className="bi bi-calendar3 me-1"></i> Duración Oficial: <strong>4 Semestres</strong>
              </div>
              {(isAdmin || isTeacher) &&
                <div className="small text-50">
                  <i className="bi bi-people me-1"></i> Alumnos en este Plan: <strong>{plan.enrolledStudentsCount ?? 0}</strong>
                </div>
              }
              {isAdmin && !plan.isActive && (
                <button
                  onClick={handleActivatePlan}
                  className="btn btn-warning btn-sm fw-bold text-dark mt-2 w-100"
                >
                  <i className="bi bi-check2-circle me-1"></i>
                  Hacer Plan Vigente
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Curriculum Grid / Semester Tabs Container (Preparation for Epic 3) */}
      <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white mb-4">
        <div className="card-header bg-light border-0 py-3 px-4">
          <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
            <div>
              <h5 className="fw-bold mb-1 text-dark">
                <i className="bi bi-diagram-3-fill text-primary me-2"></i>
                Estructura Curricular por Semestres
              </h5>
              <p className="text-muted small mb-0">
                Distribución de asignaturas de 1° a 4° semestre, créditos SCT y requisitos formativos.
              </p>
            </div>

            {/* Semester selector pills */}
            <div className="btn-group" role="group">
              {[1, 2, 3, 4].map((sem) => (
                <button
                  key={sem}
                  type="button"
                  className={`btn btn-sm ${activeSemester === sem ? 'btn-primary fw-bold' : 'btn-outline-secondary'}`}
                  onClick={() => setActiveSemester(sem)}
                >
                  Semestre {sem}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="card-body p-4">
          {/* Epic 3 Placeholder & Foundation */}
          <div className="p-4 rounded-4 bg-light border border-dashed text-center mb-4">
            <div className="d-inline-flex p-3 rounded-circle bg-primary bg-opacity-10 text-primary mb-3">
              <i className="bi bi-journal-code fs-2"></i>
            </div>
            <h5 className="fw-bold text-dark mb-2">
              Asignaturas de Semestre {activeSemester} (Preparado para Épica 3)
            </h5>
            <p className="text-muted small mx-auto mb-3" style={{ maxWidth: '640px' }}>
              En la próxima <strong>Épica 3 (Gestión de Asignaturas)</strong> se registrarán y desplegarán aquí las asignaturas correspondientes al {activeSemester}° semestre, con su código único, créditos SCT (de 1 a 7), horas pedagógicas semanales TEL (&lt; 8 hrs) y hasta 3 prerrequisitos formativos.
            </p>

            {isAdmin ? (
              <button
                className="btn btn-outline-primary btn-sm fw-semibold"
                disabled
                title="Se habilitará en la Épica 3"
              >
                <i className="bi bi-plus-circle me-1"></i>
                + Agregar Asignatura a Semestre {activeSemester} (Épica 3)
              </button>
            ) : (
              <span className="badge bg-secondary bg-opacity-10 text-secondary">
                Malla curricular oficial IPT
              </span>
            )}
          </div>

          {/* Semesters Overview Cards */}
          <div className="row g-3">
            {[1, 2, 3, 4].map((sem) => (
              <div key={sem} className="col-12 col-md-6 col-lg-3">
                <div
                  className={`p-3 rounded-3 border h-100 cursor-pointer transition-hover ${activeSemester === sem ? 'border-primary bg-primary bg-opacity-10 shadow-sm' : 'bg-white'
                    }`}
                  onClick={() => setActiveSemester(sem)}
                  style={{ cursor: 'pointer' }}
                >
                  <div className="d-flex justify-content-between align-items-center mb-2">
                    <span className="fw-bold text-dark">Semestre {sem}</span>
                    <span className="badge bg-light text-muted border">Año {sem <= 2 ? 1 : 2}</span>
                  </div>
                  <div className="text-muted small">
                    {sem === 1 && 'Ciclo Inicial de Fundamentos'}
                    {sem === 2 && 'Profundización y Aplicación'}
                    {sem === 3 && 'Competencias Avanzadas'}
                    {sem === 4 && 'Ciclo de Egreso y Titulación'}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudyPlanDetailPage;
