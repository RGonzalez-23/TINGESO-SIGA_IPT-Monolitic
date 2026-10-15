import { useState, useEffect, useCallback, useMemo } from 'react';
import academicPeriodService from '../services/academic-period.service';
import sectionService from '../services/section.service';
import courseService from '../services/course.service';
import teacherService from '../services/teacher.service';
import careerService from '../services/career.service';
import { useAuth } from '../context/AuthContext';

const DAYS = [
  { key: 'L', label: 'Lunes' },
  { key: 'M', label: 'Martes' },
  { key: 'W', label: 'Miércoles' },
  { key: 'J', label: 'Jueves' },
  { key: 'V', label: 'Viernes' },
];

const MODULES = [
  { num: 1, range: '08:15 - 09:35' },
  { num: 2, range: '09:50 - 11:10' },
  { num: 3, range: '11:25 - 12:45' },
  { num: 4, range: '13:45 - 15:05' },
  { num: 5, range: '15:20 - 16:40' },
  { num: 6, range: '16:55 - 18:15' },
];

/**
 * AcademicOfferPage provides comprehensive management of Academic Periods and Sections.
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 */
const AcademicOfferPage = () => {
  const { isAdmin } = useAuth();

  // Periods state
  const [periods, setPeriods] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [loadingPeriods, setLoadingPeriods] = useState(true);

  // Sections state
  const [sections, setSections] = useState([]);
  const [loadingSections, setLoadingSections] = useState(false);

  // Auxiliary data
  const [courses, setCourses] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [careers, setCareers] = useState([]);

  // UI state
  const [alert, setAlert] = useState(null); // { type: 'success' | 'danger' | 'warning', message: '' }
  const [activeTab, setActiveTab] = useState('list'); // 'list' | 'schedule'
  const [searchTerm, setSearchTerm] = useState('');
  const [filterCareer, setFilterCareer] = useState('ALL');
  const [filterTeacher, setFilterTeacher] = useState('ALL');

  // Modals
  const [showCreatePeriodModal, setShowCreatePeriodModal] = useState(false);
  const [showCreateSectionModal, setShowCreateSectionModal] = useState(false);
  const [showEditSectionModal, setShowEditSectionModal] = useState(false);
  const [showDeleteSectionModal, setShowDeleteSectionModal] = useState(false);
  const [sectionToEdit, setSectionToEdit] = useState(null);
  const [sectionToDelete, setSectionToDelete] = useState(null);

  // Period Form
  const [periodForm, setPeriodForm] = useState({
    year: new Date().getFullYear() + 1,
    semester: 1,
  });

  // Section Form (Create)
  const [createSectionForm, setCreateSectionForm] = useState({
    courseId: '',
    teacherRun: '',
    capacity: 45,
    selectedBlockCodes: new Set(),
  });

  // Modal course filters (Create Section)
  const [modalCourseCareerFilter, setModalCourseCareerFilter] = useState('ALL');
  const [modalCourseSemesterFilter, setModalCourseSemesterFilter] = useState('ALL');

  // Section Form (Edit)
  const [editSectionForm, setEditSectionForm] = useState({
    teacherRun: '',
    capacity: 45,
    selectedBlockCodes: new Set(),
  });

  const [submitting, setSubmitting] = useState(false);

  // Load Initial Academic Periods and Base Catalogs
  useEffect(() => {
    const initCatalogs = async () => {
      try {
        setLoadingPeriods(true);
        const [pRes, cRes, tRes, carRes] = await Promise.all([
          academicPeriodService.getAll(),
          courseService.getAllCourses(),
          teacherService.getAll({ active: true }).catch((err) => {
            console.warn('Could not load teachers catalog for current role:', err);
            return { data: [] };
          }),
          careerService.getAllCareers(),
        ]);

        setPeriods(pRes.data);
        if (pRes.data.length > 0) {
          // Default to the first open period or the first period available
          const openP = pRes.data.find((p) => p.isOpen);
          setSelectedPeriodId(openP ? openP.id : pRes.data[0].id);
        }
        setCourses(cRes.data || []);
        setTeachers(tRes.data || []);
        setCareers(carRes.data || []);
      } catch (err) {
        console.error('Error loading catalogs:', err);
        setAlert({
          type: 'danger',
          message: 'Error al cargar los catálogos y períodos iniciales.',
        });
      } finally {
        setLoadingPeriods(false);
      }
    };
    initCatalogs();
  }, []);

  // Fetch sections whenever selected period changes
  const fetchSections = useCallback(async () => {
    if (!selectedPeriodId) return;
    try {
      setLoadingSections(true);
      const res = await sectionService.getAll({ periodId: selectedPeriodId });
      setSections(res.data || []);
    } catch (err) {
      console.error('Error fetching sections:', err);
      setAlert({
        type: 'danger',
        message: 'No fue posible cargar las secciones del período seleccionado.',
      });
    } finally {
      setLoadingSections(false);
    }
  }, [selectedPeriodId]);

  useEffect(() => {
    fetchSections();
  }, [fetchSections]);

  const currentPeriod = useMemo(
    () => periods.find((p) => p.id === selectedPeriodId),
    [periods, selectedPeriodId]
  );

  // Toggle Academic Period status (Open / Closed)
  const handleTogglePeriodStatus = async () => {
    if (!currentPeriod || !isAdmin) return;
    const willOpen = !currentPeriod.isOpen;
    const otherOpen = periods.find((p) => p.isOpen && p.id !== currentPeriod.id);

    let confirmMsg = `¿Confirmas que deseas ${willOpen ? 'ABRIR' : 'CERRAR'} el período académico ${currentPeriod.id}?`;
    if (willOpen && otherOpen) {
      confirmMsg = `¿Confirmas que deseas ABRIR el período académico ${currentPeriod.id}?\n\nImportante: Solo un período académico puede estar abierto a la vez. Al abrir este período, el período ${otherOpen.id} (actualmente abierto) se cerrará automáticamente.`;
    }

    if (!window.confirm(confirmMsg)) {
      return;
    }

    try {
      setSubmitting(true);
      await academicPeriodService.toggleStatus(currentPeriod.id);
      const updatedPeriods = await academicPeriodService.getAll();
      setPeriods(updatedPeriods.data);
      setAlert({
        type: 'success',
        message: willOpen
          ? `El período ${currentPeriod.id} ahora está ABIERTO${otherOpen ? ` (se cerró automáticamente el período ${otherOpen.id})` : ''}.`
          : `El período ${currentPeriod.id} ahora está CERRADO.`,
      });
    } catch (err) {
      console.error('Error toggling period status:', err);
      setAlert({
        type: 'danger',
        message:
          err.response?.data?.message || 'Error al cambiar el estado del período.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Create Academic Period
  const handleCreatePeriod = async (e) => {
    e.preventDefault();
    const otherOpen = periods.find((p) => p.isOpen);
    try {
      setSubmitting(true);
      const res = await academicPeriodService.create({
        year: parseInt(periodForm.year, 10),
        semester: parseInt(periodForm.semester, 10),
      });
      const updatedPeriods = await academicPeriodService.getAll();
      setPeriods(updatedPeriods.data);
      setSelectedPeriodId(res.data.id);
      setShowCreatePeriodModal(false);
      setAlert({
        type: 'success',
        message: `Período académico ${res.data.id} creado con éxito y abierto para oferta académica${otherOpen ? ` (se cerró automáticamente el período ${otherOpen.id})` : ''}.`,
      });
    } catch (err) {
      console.error('Error creating academic period:', err);
      setAlert({
        type: 'danger',
        message:
          err.response?.data?.message || 'Error al crear el período académico.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Selected Course for Create Section Form
  const selectedCourseForCreate = useMemo(() => {
    if (!createSectionForm.courseId) return null;
    return courses.find((c) => String(c.id) === String(createSectionForm.courseId));
  }, [courses, createSectionForm.courseId]);

  // Filter courses for Create Section modal by Career and Semester
  const modalFilteredCourses = useMemo(() => {
    return courses.filter((c) => {
      const matchCareer =
        modalCourseCareerFilter === 'ALL' ||
        String(c.careerCode) === String(modalCourseCareerFilter);

      const matchSemester =
        modalCourseSemesterFilter === 'ALL' ||
        String(c.semester) === String(modalCourseSemesterFilter);

      return matchCareer && matchSemester;
    });
  }, [courses, modalCourseCareerFilter, modalCourseSemesterFilter]);

  // Total TEL and Required Blocks for Create Form
  const requiredBlocksCount = useMemo(() => {
    if (!selectedCourseForCreate) return 0;
    const totalTEL =
      (selectedCourseForCreate.theoryHours || 0) +
      (selectedCourseForCreate.exerciseHours || 0) +
      (selectedCourseForCreate.laboratoryHours || 0);
    return Math.max(1, Math.floor(totalTEL / 2));
  }, [selectedCourseForCreate]);

  // Expected Section Code preview: Strictly "${course.code}-A-1"
  const previewSectionCode = useMemo(() => {
    if (!selectedCourseForCreate) return '';
    return `${selectedCourseForCreate.code}-A-1`;
  }, [selectedCourseForCreate]);

  // Check if course already has a section in the current period
  const courseAlreadyHasSection = useMemo(() => {
    if (!selectedCourseForCreate) return false;
    return sections.some((s) => s.courseId === selectedCourseForCreate.id);
  }, [sections, selectedCourseForCreate]);

  // Find busy time blocks for a teacher in the current period
  const getTeacherBusyBlocks = useCallback(
    (teacherRun, excludeSectionId = null) => {
      if (!teacherRun) return new Set();
      const busy = new Set();
      sections.forEach((s) => {
        if (s.teacherRun === teacherRun && (!excludeSectionId || s.id !== excludeSectionId)) {
          s.timeBlocks.forEach((tb) => busy.add(tb.code));
        }
      });
      return busy;
    },
    [sections]
  );

  const teacherBusyBlocksInCreate = useMemo(() => {
    return getTeacherBusyBlocks(createSectionForm.teacherRun);
  }, [getTeacherBusyBlocks, createSectionForm.teacherRun]);

  const teacherBusyBlocksInEdit = useMemo(() => {
    if (!sectionToEdit) return new Set();
    return getTeacherBusyBlocks(editSectionForm.teacherRun, sectionToEdit.id);
  }, [getTeacherBusyBlocks, editSectionForm.teacherRun, sectionToEdit]);

  // Toggle Time Block in Create Form
  const handleToggleBlockCreate = (blockCode) => {
    const nextSet = new Set(createSectionForm.selectedBlockCodes);
    if (nextSet.has(blockCode)) {
      nextSet.delete(blockCode);
    } else {
      if (nextSet.size >= requiredBlocksCount) {
        setAlert({
          type: 'warning',
          message: `Esta asignatura requiere exactamente ${requiredBlocksCount} bloques. Deselecciona uno antes de agregar otro.`,
        });
        return;
      }
      nextSet.add(blockCode);
    }
    setCreateSectionForm((prev) => ({ ...prev, selectedBlockCodes: nextSet }));
  };

  // Toggle Time Block in Edit Form
  const handleToggleBlockEdit = (blockCode) => {
    if (!sectionToEdit) return;
    const required = sectionToEdit.requiredBlocksCount || 1;
    const nextSet = new Set(editSectionForm.selectedBlockCodes);
    if (nextSet.has(blockCode)) {
      nextSet.delete(blockCode);
    } else {
      if (nextSet.size >= required) {
        setAlert({
          type: 'warning',
          message: `Esta asignatura requiere exactamente ${required} bloques. Deselecciona uno antes de agregar otro.`,
        });
        return;
      }
      nextSet.add(blockCode);
    }
    setEditSectionForm((prev) => ({ ...prev, selectedBlockCodes: nextSet }));
  };

  // Submit Create Section
  const handleCreateSectionSubmit = async (e) => {
    e.preventDefault();
    if (!selectedCourseForCreate) {
      setAlert({ type: 'warning', message: 'Debes seleccionar una asignatura.' });
      return;
    }
    if (!createSectionForm.teacherRun) {
      setAlert({ type: 'warning', message: 'Debes asignar un docente activo.' });
      return;
    }
    if (createSectionForm.selectedBlockCodes.size !== requiredBlocksCount) {
      setAlert({
        type: 'danger',
        message: `Debes seleccionar exactamente ${requiredBlocksCount} bloques horarios (tienes ${createSectionForm.selectedBlockCodes.size}).`,
      });
      return;
    }

    try {
      setSubmitting(true);
      await sectionService.create({
        courseId: selectedCourseForCreate.id,
        academicPeriodId: selectedPeriodId,
        teacherRun: createSectionForm.teacherRun,
        capacity: parseInt(createSectionForm.capacity, 10),
        timeBlockCodes: Array.from(createSectionForm.selectedBlockCodes),
      });

      setShowCreateSectionModal(false);
      setCreateSectionForm({
        courseId: '',
        teacherRun: '',
        capacity: 45,
        selectedBlockCodes: new Set(),
      });
      setModalCourseCareerFilter('ALL');
      setModalCourseSemesterFilter('ALL');
      setAlert({
        type: 'success',
        message: `Sección ${previewSectionCode} creada exitosamente para ${selectedCourseForCreate.name}.`,
      });
      fetchSections();
    } catch (err) {
      console.error('Error creating section:', err);
      setAlert({
        type: 'danger',
        message: err.response?.data?.message || 'Error al crear la sección.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Open Edit Section Modal
  const handleOpenEditSection = (section) => {
    setSectionToEdit(section);
    setEditSectionForm({
      teacherRun: section.teacherRun,
      capacity: section.capacity,
      selectedBlockCodes: new Set(section.timeBlocks.map((b) => b.code)),
    });
    setShowEditSectionModal(true);
  };

  // Submit Edit Section
  const handleEditSectionSubmit = async (e) => {
    e.preventDefault();
    if (!sectionToEdit) return;
    const required = sectionToEdit.requiredBlocksCount;
    if (editSectionForm.selectedBlockCodes.size !== required) {
      setAlert({
        type: 'danger',
        message: `Debes seleccionar exactamente ${required} bloques horarios (tienes ${editSectionForm.selectedBlockCodes.size}).`,
      });
      return;
    }

    try {
      setSubmitting(true);
      await sectionService.update(sectionToEdit.id, {
        teacherRun: editSectionForm.teacherRun,
        capacity: parseInt(editSectionForm.capacity, 10),
        timeBlockCodes: Array.from(editSectionForm.selectedBlockCodes),
      });

      setShowEditSectionModal(false);
      setSectionToEdit(null);
      setAlert({
        type: 'success',
        message: `Sección ${sectionToEdit.sectionCode} actualizada exitosamente.`,
      });
      fetchSections();
    } catch (err) {
      console.error('Error updating section:', err);
      setAlert({
        type: 'danger',
        message: err.response?.data?.message || 'Error al actualizar la sección.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Delete Section Confirmation & Action
  const handleConfirmDeleteSection = async () => {
    if (!sectionToDelete) return;
    try {
      setSubmitting(true);
      await sectionService.delete(sectionToDelete.id);
      setShowDeleteSectionModal(false);
      setSectionToDelete(null);
      setAlert({
        type: 'success',
        message: `Sección ${sectionToDelete.sectionCode} eliminada exitosamente.`,
      });
      fetchSections();
    } catch (err) {
      console.error('Error deleting section:', err);
      setAlert({
        type: 'danger',
        message: err.response?.data?.message || 'Error al eliminar la sección.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // Filtered Sections
  const filteredSections = useMemo(() => {
    return sections.filter((s) => {
      const matchSearch =
        !searchTerm.trim() ||
        s.sectionCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.courseCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.courseName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.teacherName?.toLowerCase().includes(searchTerm.toLowerCase());

      const matchCareer =
        filterCareer === 'ALL' || s.careerCode === filterCareer;

      const matchTeacher =
        filterTeacher === 'ALL' || s.teacherRun === filterTeacher;

      return matchSearch && matchCareer && matchTeacher;
    });
  }, [sections, searchTerm, filterCareer, filterTeacher]);

  // Metrics
  const metrics = useMemo(() => {
    const totalSections = filteredSections.length;
    const totalCapacity = filteredSections.reduce((acc, s) => acc + s.capacity, 0);
    const totalEnrolled = filteredSections.reduce((acc, s) => acc + s.enrolledCount, 0);
    const distinctTeachers = new Set(filteredSections.map((s) => s.teacherRun)).size;
    return { totalSections, totalCapacity, totalEnrolled, distinctTeachers };
  }, [filteredSections]);

  return (
    <div className="container py-4">
      {/* Top Banner */}
      <div
        className="p-4 p-md-5 mb-4 rounded-4 text-white shadow-sm position-relative overflow-hidden"
        style={{
          background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 40%, #4338ca 100%)',
        }}
      >
        <div className="position-relative z-1">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
            <div>
              <div className="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white bg-opacity-20 backdrop-blur mb-3 text-black small fw-semibold">
                <i className="bi bi-calendar3-range"></i>
                {isAdmin ? <span>Gestión de Oferta Académica y Secciones</span> : <span>Oferta Académica</span>}
              </div>
              <h1 className="display-6 fw-bold mb-2">Oferta Académica IPT</h1>
              <p className="lead mb-0 text-white-50 fs-6" style={{ maxWidth: '700px' }}>
                Planificación semestral de asignaturas, distribución de módulos horarios
                estandarizados de 80 minutos y asignación de docentes. Formato de código de sección:{' '}
                <span className="font-monospace fw-bold text-white bg-black bg-opacity-25 px-2 py-0.5 rounded">
                  [CÓDIGO]-A-1
                </span>
                .
              </p>
            </div>

            {/* Academic Period Selector Badge */}
            <div className="bg-white bg-opacity-10 p-3 rounded-4 backdrop-blur border border-white border-opacity-20 d-flex flex-column gap-2 text-end">
              <span className="small text-white-50 fw-semibold text-uppercase">
                Período Académico Activo
              </span>
              <div className="d-flex align-items-center gap-2">
                <select
                  className="form-select form-select-lg fw-bold border-0 bg-white text-dark shadow-sm"
                  style={{ minWidth: '160px' }}
                  value={selectedPeriodId}
                  onChange={(e) => setSelectedPeriodId(e.target.value)}
                  disabled={loadingPeriods}
                >
                  {periods.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.id} {p.isOpen ? '• ABIERTO' : '• CERRADO'}
                    </option>
                  ))}
                </select>

                {currentPeriod && (
                  <span
                    className={`badge p-2 px-3 rounded-pill fs-6 ${currentPeriod.isOpen ? 'bg-success' : 'bg-secondary'
                      }`}
                  >
                    <i
                      className={`bi ${currentPeriod.isOpen ? 'bi-unlock-fill' : 'bi-lock-fill'
                        } me-1`}
                    ></i>
                    {currentPeriod.isOpen ? 'ABIERTO' : 'CERRADO'}
                  </span>
                )}
              </div>

              {/* Admin Period Actions */}
              {isAdmin && currentPeriod && (
                <div className="d-flex align-items-center justify-content-end gap-2 mt-1">
                  <button
                    onClick={handleTogglePeriodStatus}
                    disabled={submitting}
                    className={`btn btn-sm ${currentPeriod.isOpen ? 'btn-outline-warning text-white' : 'btn-outline-success text-white'
                      } fw-semibold`}
                  >
                    <i
                      className={`bi ${currentPeriod.isOpen ? 'bi-lock' : 'bi-unlock'
                        } me-1`}
                    ></i>
                    {currentPeriod.isOpen ? 'Cerrar Período' : 'Abrir Período'}
                  </button>
                  <button
                    onClick={() => setShowCreatePeriodModal(true)}
                    className="btn btn-sm btn-light fw-semibold text-primary"
                  >
                    <i className="bi bi-plus-circle me-1"></i>
                    Nuevo Período
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Alert banner if period is closed */}
      {currentPeriod && !currentPeriod.isOpen && (
        <div className="alert alert-warning border-0 shadow-sm rounded-4 d-flex align-items-center gap-3 p-3 mb-4">
          <i className="bi bi-shield-lock-fill fs-3 text-warning"></i>
          <div>
            <h6 className="fw-bold mb-1">Período Académico CERRADO</h6>
            <p className="small mb-0 text-muted">
              Este período académico se encuentra en modo sólo lectura. No es posible crear, editar ni
              eliminar secciones hasta que un administrador lo vuelva a abrir.
            </p>
          </div>
        </div>
      )}

      {/* Global Alert */}
      {alert && (
        <div
          className={`alert alert-${alert.type} alert-dismissible fade show rounded-4 shadow-sm mb-4`}
          role="alert"
        >
          <i
            className={`bi ${alert.type === 'success'
              ? 'bi-check-circle-fill'
              : alert.type === 'danger'
                ? 'bi-exclamation-triangle-fill'
                : 'bi-info-circle-fill'
              } me-2`}
          ></i>
          {alert.message}
          <button
            type="button"
            className="btn-close"
            onClick={() => setAlert(null)}
          ></button>
        </div>
      )}

      {/* Statistics Cards */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white h-100">
            <span className="text-muted small fw-semibold">Total Secciones</span>
            <div className="d-flex align-items-baseline gap-2 mt-1">
              <span className="fs-3 fw-bold text-primary">{metrics.totalSections}</span>
              <span className="text-muted small">asignaturas</span>
            </div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white h-100">
            <span className="text-muted small fw-semibold">Capacidad Total</span>
            <div className="d-flex align-items-baseline gap-2 mt-1">
              <span className="fs-3 fw-bold text-success">{metrics.totalCapacity}</span>
              <span className="text-muted small">cupos ofertados</span>
            </div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white h-100">
            <span className="text-muted small fw-semibold">Estudiantes Inscritos</span>
            <div className="d-flex align-items-baseline gap-2 mt-1">
              <span className="fs-3 fw-bold text-info">{metrics.totalEnrolled}</span>
              <span className="text-muted small">matrículas</span>
            </div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card border-0 shadow-sm rounded-4 p-3 bg-white h-100">
            <span className="text-muted small fw-semibold">Docentes Asignados</span>
            <div className="d-flex align-items-baseline gap-2 mt-1">
              <span className="fs-3 fw-bold text-indigo" style={{ color: '#4338ca' }}>
                {metrics.distinctTeachers}
              </span>
              <span className="text-muted small">profesores</span>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs and Controls */}
      <div className="card border-0 shadow-sm rounded-4 mb-4">
        <div className="card-body p-3 p-md-4">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            {/* View Switcher Tabs */}
            <ul className="nav nav-pills bg-light p-1 rounded-3">
              <li className="nav-item">
                <button
                  className={`nav-link fw-semibold px-3 py-2 rounded-3 ${activeTab === 'list' ? 'active bg-primary text-white shadow-sm' : 'text-muted'
                    }`}
                  onClick={() => setActiveTab('list')}
                >
                  <i className="bi bi-list-columns-reverse me-2"></i>
                  Listado de Secciones
                </button>
              </li>
              <li className="nav-item">
                <button
                  className={`nav-link fw-semibold px-3 py-2 rounded-3 ${activeTab === 'schedule' ? 'active bg-primary text-white shadow-sm' : 'text-muted'
                    }`}
                  onClick={() => setActiveTab('schedule')}
                >
                  <i className="bi bi-grid-3x3-gap-fill me-2"></i>
                  Malla Horaria Semanal
                </button>
              </li>
            </ul>

            {/* Create Section CTA Button */}
            {isAdmin && (
              <button
                className="btn btn-primary fw-semibold px-4 py-2 rounded-3 d-flex align-items-center gap-2 shadow-sm"
                style={{ backgroundColor: '#312e81', borderColor: '#312e81' }}
                onClick={() => {
                  setCreateSectionForm({
                    courseId: '',
                    teacherRun: '',
                    capacity: 45,
                    selectedBlockCodes: new Set(),
                  });
                  setModalCourseCareerFilter('ALL');
                  setModalCourseSemesterFilter('ALL');
                  setShowCreateSectionModal(true);
                }}
                disabled={!currentPeriod?.isOpen}
              >
                <i className="bi bi-plus-lg fs-5"></i>
                <span>Crear Nueva Sección</span>
              </button>
            )}
          </div>

          {/* Search and Filters */}
          <div className="row g-3">
            <div className="col-12 col-md-5">
              <div className="input-group">
                <span className="input-group-text bg-white border-end-0">
                  <i className="bi bi-search text-muted"></i>
                </span>
                <input
                  type="text"
                  className="form-control border-start-0 ps-0"
                  placeholder="Buscar por código de sección (TAP101-A-1), asignatura o docente..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="col-6 col-md-3">
              <select
                className="form-select"
                value={filterCareer}
                onChange={(e) => setFilterCareer(e.target.value)}
              >
                <option value="ALL">Todas las Carreras</option>
                {careers.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-6 col-md-4">
              <select
                className="form-select"
                value={filterTeacher}
                onChange={(e) => setFilterTeacher(e.target.value)}
              >
                <option value="ALL">Todos los Docentes</option>
                {teachers.map((t) => (
                  <option key={t.run} value={t.run}>
                    {t.fullName} ({t.run})
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content: List Tab vs Schedule Tab */}
      {loadingSections ? (
        <div className="text-center py-5 text-muted">
          <div className="spinner-border text-primary mb-3" role="status"></div>
          <p className="fw-semibold">Cargando secciones del período...</p>
        </div>
      ) : activeTab === 'list' ? (
        /* ================= LIST TAB ================= */
        <div className="card border-0 shadow-sm rounded-4 overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light small text-uppercase">
                <tr>
                  <th className="ps-4">Código Sección</th>
                  <th>Asignatura</th>
                  <th>Carrera / Plan</th>
                  <th>Docente</th>
                  <th>Horario (Bloques)</th>
                  <th className="text-center">Cupos</th>
                  {isAdmin && <th className="text-end pe-4">Acciones</th>}
                </tr>
              </thead>
              <tbody className="divide-y">
                {filteredSections.length === 0 ? (
                  <tr>
                    <td
                      colSpan={isAdmin ? 7 : 6}
                      className="text-center py-5 text-muted"
                    >
                      <i className="bi bi-inbox fs-1 d-block mb-2 text-black-50"></i>
                      No se encontraron secciones en la oferta de este período con los filtros aplicados.
                    </td>
                  </tr>
                ) : (
                  filteredSections.map((sec) => (
                    <tr key={sec.id}>
                      <td className="ps-4">
                        <span className="badge bg-dark bg-opacity-75 font-monospace fs-6 px-3 py-2 rounded-3 border border-dark">
                          {sec.sectionCode}
                        </span>
                        <div className="small text-muted mt-1">
                          Semestre {sec.semester} &bull; {sec.sctCredits} SCT
                        </div>
                      </td>
                      <td>
                        <div className="fw-bold text-dark">{sec.courseName}</div>
                        <div className="small text-muted font-monospace">
                          {sec.courseCode} &bull; TEL {sec.theoryHours}-{sec.exerciseHours}-{sec.laboratoryHours} ({sec.totalTELHours} hrs)
                        </div>
                      </td>
                      <td>
                        <div className="small fw-semibold text-dark">
                          {sec.careerName || 'Técnico Universitario'}
                        </div>
                        <span className="badge bg-light text-secondary border">
                          Plan {sec.studyPlanCode || 'Vigente'}
                        </span>
                      </td>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <div
                            className="rounded-circle bg-primary bg-opacity-10 text-primary d-flex align-items-center justify-content-center fw-bold"
                            style={{ width: '32px', height: '32px', fontSize: '13px' }}
                          >
                            {sec.teacherName?.charAt(0) || 'D'}
                          </div>
                          <div>
                            <div className="small fw-semibold text-dark">
                              {sec.teacherName}
                            </div>
                            <div className="small text-muted font-monospace">
                              {sec.teacherRun}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="d-flex flex-wrap gap-1" style={{ maxWidth: '280px' }}>
                          {sec.timeBlocks.map((b) => (
                            <span
                              key={b.code}
                              className="badge bg-indigo-subtle text-indigo border px-2 py-1 rounded"
                              style={{
                                backgroundColor: '#e0e7ff',
                                color: '#3730a3',
                                borderColor: '#c7d2fe',
                              }}
                              title={`${b.dayName} ${b.moduleNumber} (${b.startTime?.slice(0, 5)} - ${b.endTime?.slice(0, 5)})`}
                            >
                              <strong>{b.code}</strong> ({b.startTime?.slice(0, 5)}-{b.endTime?.slice(0, 5)})
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="text-center">
                        <div className="fw-bold">
                          <span className="text-success">{sec.availableQuota}</span>
                          <span className="text-muted fw-normal"> / {sec.capacity}</span>
                        </div>
                        <div className="progress mt-1" style={{ height: '5px' }}>
                          <div
                            className="progress-bar bg-primary"
                            role="progressbar"
                            style={{
                              width: `${(sec.enrolledCount / sec.capacity) * 100}%`,
                            }}
                          ></div>
                        </div>
                        <span className="small text-muted mt-1 d-block">
                          {sec.enrolledCount} inscritos
                        </span>
                      </td>
                      {isAdmin && (
                        <td className="text-end pe-4">
                          <div className="btn-group">
                            <button
                              className="btn btn-sm btn-outline-secondary"
                              title="Editar Sección"
                              onClick={() => handleOpenEditSection(sec)}
                              disabled={!currentPeriod?.isOpen}
                            >
                              <i className="bi bi-pencil-fill"></i>
                            </button>
                            <button
                              className="btn btn-sm btn-outline-danger"
                              title={
                                sec.enrolledCount > 0
                                  ? 'No se puede eliminar: tiene alumnos inscritos'
                                  : 'Eliminar Sección'
                              }
                              onClick={() => {
                                setSectionToDelete(sec);
                                setShowDeleteSectionModal(true);
                              }}
                              disabled={!currentPeriod?.isOpen || sec.enrolledCount > 0}
                            >
                              <i className="bi bi-trash-fill"></i>
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* ================= SCHEDULE MATRIX TAB ================= */
        <div className="card border-0 shadow-sm rounded-4 overflow-hidden">
          <div className="card-header bg-white p-3 border-bottom d-flex align-items-center justify-content-between">
            <h6 className="fw-bold mb-0">
              <i className="bi bi-calendar-week me-2 text-primary"></i>
              Horario Semanal de la Oferta ({currentPeriod?.id})
            </h6>
            <span className="badge bg-light text-muted border">
              6 módulos diarios de 80 min &bull; Recreos 15 min &bull; Almuerzo 1 hr
            </span>
          </div>
          <div className="table-responsive">
            <table className="table table-bordered mb-0 text-center align-middle" style={{ minWidth: '800px' }}>
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
                  <tr key={m.num}>
                    <td className="bg-light p-2">
                      <div className="fw-bold text-dark">Módulo {m.num}</div>
                      <div className="small text-muted font-monospace">{m.range}</div>
                    </td>
                    {DAYS.map((d) => {
                      const code = `${d.key}${m.num}`;
                      const matchedSections = filteredSections.filter((s) =>
                        s.timeBlocks.some((b) => b.code === code)
                      );

                      return (
                        <td
                          key={code}
                          className="p-2"
                          style={{
                            verticalAlign: 'top',
                            backgroundColor: matchedSections.length > 0 ? '#f8fafc' : 'white',
                            height: '100px',
                          }}
                        >
                          {matchedSections.length === 0 ? (
                            <span className="text-muted small opacity-25 font-monospace">
                              {code}
                            </span>
                          ) : (
                            <div className="d-flex flex-column gap-1">
                              {matchedSections.map((s) => (
                                <div
                                  key={s.id}
                                  className="p-2 rounded-3 text-start shadow-sm border"
                                  style={{
                                    backgroundColor: '#eef2ff',
                                    borderColor: '#c7d2fe',
                                  }}
                                >
                                  <div className="d-flex align-items-center justify-content-between">
                                    <span className="badge bg-dark font-monospace" style={{ fontSize: '11px' }}>
                                      {s.sectionCode}
                                    </span>
                                    <span className="small text-muted" style={{ fontSize: '11px' }}>
                                      {s.availableQuota} cupos
                                    </span>
                                  </div>
                                  <div className="fw-bold text-dark mt-1 text-truncate" style={{ fontSize: '12px' }}>
                                    {s.courseName}
                                  </div>
                                  <div className="text-muted text-truncate" style={{ fontSize: '11px' }}>
                                    <i className="bi bi-person me-1"></i>
                                    {s.teacherName}
                                  </div>
                                </div>
                              ))}
                            </div>
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

      {/* ================= MODAL: CREATE ACADEMIC PERIOD ================= */}
      {showCreatePeriodModal && (
        <div
          className="modal show d-block"
          tabIndex="-1"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <form onSubmit={handleCreatePeriod}>
                <div className="modal-header border-0 pb-0">
                  <h5 className="modal-title fw-bold">
                    <i className="bi bi-calendar-plus text-primary me-2"></i>
                    Crear Período Académico
                  </h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setShowCreatePeriodModal(false)}
                  ></button>
                </div>
                <div className="modal-body p-4">
                  <div className="alert alert-info border-0 small mb-3 p-3 rounded-3">
                    <i className="bi bi-info-circle-fill me-2 text-primary"></i>
                    <strong>Regla:</strong> Solo un período académico puede estar abierto a la vez. Al crear este período (que inicia <strong>ABIERTO</strong>), cualquier otro período actualmente abierto se cerrará automáticamente.
                  </div>
                  <div className="mb-3">
                    <label className="form-label fw-semibold">Año Académico</label>
                    <input
                      type="number"
                      className="form-control"
                      value={periodForm.year}
                      onChange={(e) =>
                        setPeriodForm((prev) => ({ ...prev, year: e.target.value }))
                      }
                      min="2020"
                      max="2035"
                      required
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label fw-semibold">Semestre</label>
                    <select
                      className="form-select"
                      value={periodForm.semester}
                      onChange={(e) =>
                        setPeriodForm((prev) => ({ ...prev, semester: e.target.value }))
                      }
                      required
                    >
                      <option value="1">1 (Primer Semestre)</option>
                      <option value="2">2 (Segundo Semestre)</option>
                    </select>
                  </div>
                  <div className="p-3 bg-light rounded-3 border text-center">
                    <span className="small text-muted">Identificador resultante: </span>
                    <strong className="font-monospace fs-5 text-primary">
                      {periodForm.year}-{periodForm.semester}
                    </strong>
                  </div>
                </div>
                <div className="modal-footer border-0 pt-0">
                  <button
                    type="button"
                    className="btn btn-light rounded-3"
                    onClick={() => setShowCreatePeriodModal(false)}
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary rounded-3 fw-semibold px-4"
                    disabled={submitting}
                  >
                    {submitting ? 'Creando...' : 'Crear Período'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: CREATE SECTION ================= */}
      {showCreateSectionModal && (
        <div
          className="modal show d-block"
          tabIndex="-1"
          style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}
        >
          <div className="modal-dialog modal-dialog-centered modal-xl">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <form onSubmit={handleCreateSectionSubmit}>
                <div className="modal-header border-bottom p-4">
                  <div>
                    <h5 className="modal-title fw-bold">
                      <i className="bi bi-plus-circle-fill text-primary me-2"></i>
                      Nueva Sección para Oferta Académica ({selectedPeriodId})
                    </h5>
                    <p className="text-muted small mb-0">
                      Asigna la asignatura, el docente y la carga horaria en los bloques correspondientes.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setShowCreateSectionModal(false)}
                  ></button>
                </div>

                <div className="modal-body p-4">
                  <div className="row g-4 mb-4">
                    {/* Course Selection */}
                    <div className="col-12 col-md-6">
                      <label className="form-label fw-bold mb-2">
                        1. Seleccionar Asignatura <span className="text-danger">*</span>
                      </label>

                      {/* Filtros por Carrera y Semestre dentro del formulario */}
                      <div className="card border-0 bg-light rounded-3 p-2 mb-3 shadow-none">
                        <div className="row g-2">
                          <div className="col-12 col-sm-6">
                            <label className="form-label small text-muted fw-semibold mb-1 d-flex align-items-center">
                              <i className="bi bi-mortarboard-fill me-1 text-primary"></i> Filtrar Carrera
                            </label>
                            <select
                              className="form-select form-select-sm"
                              value={modalCourseCareerFilter}
                              onChange={(e) => setModalCourseCareerFilter(e.target.value)}
                            >
                              <option value="ALL">Todas las Carreras</option>
                              {careers.map((c) => (
                                <option key={c.code} value={c.code}>
                                  {c.name} ({c.code})
                                </option>
                              ))}
                            </select>
                          </div>
                          <div className="col-12 col-sm-6">
                            <label className="form-label small text-muted fw-semibold mb-1 d-flex align-items-center">
                              <i className="bi bi-calendar-week-fill me-1" style={{ color: '#4338ca' }}></i> Filtrar Semestre
                            </label>
                            <select
                              className="form-select form-select-sm"
                              value={modalCourseSemesterFilter}
                              onChange={(e) => setModalCourseSemesterFilter(e.target.value)}
                            >
                              <option value="ALL">Todos los Semestres</option>
                              <option value="1">1° Semestre</option>
                              <option value="2">2° Semestre</option>
                              <option value="3">3° Semestre</option>
                              <option value="4">4° Semestre</option>
                            </select>
                          </div>
                        </div>
                      </div>

                      <select
                        className="form-select"
                        value={createSectionForm.courseId}
                        onChange={(e) => {
                          setCreateSectionForm((prev) => ({
                            ...prev,
                            courseId: e.target.value,
                            selectedBlockCodes: new Set(),
                          }));
                        }}
                        required
                      >
                        <option value="">
                          -- Selecciona una asignatura ({modalFilteredCourses.length} disponibles) --
                        </option>
                        {/* Mantener la asignatura seleccionada visible en el select si fue filtrada temporalmente */}
                        {selectedCourseForCreate &&
                          !modalFilteredCourses.some((c) => String(c.id) === String(selectedCourseForCreate.id)) && (
                            <option value={selectedCourseForCreate.id}>
                              [{selectedCourseForCreate.code}] {selectedCourseForCreate.name} (Sem {selectedCourseForCreate.semester} - Seleccionada actualmente)
                            </option>
                          )}
                        {modalFilteredCourses.map((c) => {
                          const hasSec = sections.some((s) => s.courseId === c.id);
                          return (
                            <option
                              key={c.id}
                              value={c.id}
                              disabled={hasSec}
                            >
                              [{c.code}] {c.name} (Sem {c.semester}) {hasSec ? '— YA TIENE SECCIÓN' : ''}
                            </option>
                          );
                        })}
                      </select>

                      {modalFilteredCourses.length === 0 && !selectedCourseForCreate && (
                        <div className="alert alert-warning mt-2 small p-2 mb-0">
                          <i className="bi bi-info-circle me-1"></i>
                          No se encontraron asignaturas para la carrera y semestre seleccionados.
                        </div>
                      )}

                      {selectedCourseForCreate && (
                        <div className="mt-3 p-3 bg-light rounded-3 border">
                          <div className="d-flex align-items-center justify-content-between mb-2">
                            <span className="small text-muted">Código de Sección Formato Canónico:</span>
                            <span className="badge bg-primary font-monospace fs-6 px-3 py-1">
                              {previewSectionCode}
                            </span>
                          </div>
                          <div className="small text-muted">
                            <div><strong>Carrera:</strong> {selectedCourseForCreate.careerName || 'IPT USACH'}</div>
                            <div>
                              <strong>Horas TEL:</strong> {selectedCourseForCreate.theoryHours} Teoría, {selectedCourseForCreate.exerciseHours} Ejercicios, {selectedCourseForCreate.laboratoryHours} Laboratorio (Total {selectedCourseForCreate.theoryHours + selectedCourseForCreate.exerciseHours + selectedCourseForCreate.laboratoryHours} hrs)
                            </div>
                            <div className="mt-1 text-primary fw-semibold">
                              <i className="bi bi-info-circle me-1"></i>
                              Requiere exactamente <strong>{requiredBlocksCount} bloques horarios</strong> (80 min c/u)
                            </div>
                          </div>
                        </div>
                      )}

                      {courseAlreadyHasSection && (
                        <div className="alert alert-danger mt-2 small p-2">
                          Esta asignatura ya tiene una sección creada en este período. Cada asignatura solo puede tener una única sección.
                        </div>
                      )}
                    </div>

                    {/* Teacher & Capacity Selection */}
                    <div className="col-12 col-md-6">
                      <div className="mb-3">
                        <label className="form-label fw-bold">
                          2. Asignar Docente <span className="text-danger">*</span>
                        </label>
                        <select
                          className="form-select"
                          value={createSectionForm.teacherRun}
                          onChange={(e) =>
                            setCreateSectionForm((prev) => ({
                              ...prev,
                              teacherRun: e.target.value,
                            }))
                          }
                          required
                        >
                          <option value="">-- Selecciona un docente activo --</option>
                          {teachers.map((t) => (
                            <option key={t.run} value={t.run}>
                              {t.fullName} ({t.run}) — {t.professionalTitle || t.academicDegree}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="mb-3">
                        <label className="form-label fw-bold">
                          3. Capacidad de Cupos (1 a 60) <span className="text-danger">*</span>
                        </label>
                        <div className="input-group">
                          <input
                            type="number"
                            className="form-control"
                            value={createSectionForm.capacity}
                            onChange={(e) =>
                              setCreateSectionForm((prev) => ({
                                ...prev,
                                capacity: e.target.value,
                              }))
                            }
                            min="1"
                            max="60"
                            required
                          />
                          <span className="input-group-text">estudiantes</span>
                        </div>
                        <div className="form-text small">
                          Cupo estándar sugerido: 45 estudiantes.
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 4. Interactive Schedule Grid */}
                  <div className="border-top pt-3">
                    <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                      <div>
                        <h6 className="fw-bold mb-0">
                          4. Selección de Bloques Horarios
                        </h6>
                        <span className="text-muted small">
                          Haz clic en los bloques para asignarlos a esta sección.
                        </span>
                      </div>
                      <div className="d-flex align-items-center gap-2">
                        <span
                          className={`badge fs-6 px-3 py-2 rounded-pill ${createSectionForm.selectedBlockCodes.size === requiredBlocksCount &&
                            requiredBlocksCount > 0
                            ? 'bg-success'
                            : 'bg-warning text-dark'
                            }`}
                        >
                          Seleccionados: {createSectionForm.selectedBlockCodes.size} de{' '}
                          {requiredBlocksCount} requeridos
                        </span>
                      </div>
                    </div>

                    <div className="table-responsive border rounded-3 overflow-hidden">
                      <table className="table table-bordered mb-0 text-center align-middle">
                        <thead className="table-light">
                          <tr>
                            <th style={{ width: '130px' }}>Módulo</th>
                            {DAYS.map((d) => (
                              <th key={d.key}>{d.label}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {MODULES.map((m) => (
                            <tr key={m.num}>
                              <td className="bg-light p-2">
                                <div className="fw-bold" style={{ fontSize: '13px' }}>
                                  Módulo {m.num}
                                </div>
                                <div className="text-muted font-monospace" style={{ fontSize: '11px' }}>
                                  {m.range}
                                </div>
                              </td>
                              {DAYS.map((d) => {
                                const code = `${d.key}${m.num}`;
                                const isSelected = createSectionForm.selectedBlockCodes.has(code);
                                const isBusy = teacherBusyBlocksInCreate.has(code);

                                return (
                                  <td
                                    key={code}
                                    className="p-1"
                                    style={{
                                      backgroundColor: isSelected
                                        ? '#dbeafe'
                                        : isBusy
                                          ? '#fee2e2'
                                          : 'white',
                                      cursor: isBusy ? 'not-allowed' : 'pointer',
                                      transition: 'background-color 0.15s ease',
                                    }}
                                    onClick={() => {
                                      if (isBusy) {
                                        setAlert({
                                          type: 'danger',
                                          message: `Conflicto de horario: El docente seleccionado ya tiene otra sección asignada en el bloque ${code}.`,
                                        });
                                        return;
                                      }
                                      handleToggleBlockCreate(code);
                                    }}
                                  >
                                    <div className="p-2 rounded-2 d-flex flex-column align-items-center justify-content-center h-100">
                                      <span
                                        className={`font-monospace fw-bold ${isSelected
                                          ? 'text-primary'
                                          : isBusy
                                            ? 'text-danger'
                                            : 'text-dark'
                                          }`}
                                      >
                                        {code}
                                      </span>
                                      {isSelected && (
                                        <span className="badge bg-primary mt-1" style={{ fontSize: '10px' }}>
                                          <i className="bi bi-check2 me-1"></i> Asignado
                                        </span>
                                      )}
                                      {isBusy && (
                                        <span className="badge bg-danger mt-1" style={{ fontSize: '10px' }}>
                                          <i className="bi bi-x-circle me-1"></i> Ocupado
                                        </span>
                                      )}
                                    </div>
                                  </td>
                                );
                              })}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-top p-3">
                  <button
                    type="button"
                    className="btn btn-light rounded-3"
                    onClick={() => setShowCreateSectionModal(false)}
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary rounded-3 fw-semibold px-4"
                    disabled={
                      submitting ||
                      !selectedCourseForCreate ||
                      courseAlreadyHasSection ||
                      createSectionForm.selectedBlockCodes.size !== requiredBlocksCount
                    }
                  >
                    {submitting ? 'Guardando...' : 'Guardar y Publicar Sección'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: EDIT SECTION ================= */}
      {showEditSectionModal && sectionToEdit && (
        <div
          className="modal show d-block"
          tabIndex="-1"
          style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}
        >
          <div className="modal-dialog modal-dialog-centered modal-xl">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <form onSubmit={handleEditSectionSubmit}>
                <div className="modal-header border-bottom p-4">
                  <div>
                    <h5 className="modal-title fw-bold">
                      <i className="bi bi-pencil-square text-primary me-2"></i>
                      Editar Sección: {sectionToEdit.sectionCode}
                    </h5>
                    <p className="text-muted small mb-0">
                      Asignatura: {sectionToEdit.courseName} &bull; Requiere{' '}
                      {sectionToEdit.requiredBlocksCount} bloques horarios.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setShowEditSectionModal(false)}
                  ></button>
                </div>

                <div className="modal-body p-4">
                  <div className="row g-4 mb-4">
                    <div className="col-12 col-md-6">
                      <label className="form-label fw-bold">Docente Asignado</label>
                      <select
                        className="form-select"
                        value={editSectionForm.teacherRun}
                        onChange={(e) =>
                          setEditSectionForm((prev) => ({
                            ...prev,
                            teacherRun: e.target.value,
                          }))
                        }
                        required
                      >
                        {teachers.map((t) => (
                          <option key={t.run} value={t.run}>
                            {t.fullName} ({t.run}) — {t.professionalTitle || t.academicDegree}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="col-12 col-md-6">
                      <label className="form-label fw-bold">
                        Capacidad de Cupos (Mínimo: {sectionToEdit.enrolledCount})
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        value={editSectionForm.capacity}
                        onChange={(e) =>
                          setEditSectionForm((prev) => ({
                            ...prev,
                            capacity: e.target.value,
                          }))
                        }
                        min={Math.max(1, sectionToEdit.enrolledCount)}
                        max="60"
                        required
                      />
                      <div className="form-text small">
                        Actualmente hay {sectionToEdit.enrolledCount} alumnos matriculados.
                      </div>
                    </div>
                  </div>

                  {/* Interactive Schedule Grid for Edit */}
                  <div className="border-top pt-3">
                    <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                      <div>
                        <h6 className="fw-bold mb-0">Distribución Horaria</h6>
                        <span className="text-muted small">
                          Selecciona los {sectionToEdit.requiredBlocksCount} bloques de 80 minutos.
                        </span>
                      </div>
                      <span
                        className={`badge fs-6 px-3 py-2 rounded-pill ${editSectionForm.selectedBlockCodes.size === sectionToEdit.requiredBlocksCount
                          ? 'bg-success'
                          : 'bg-warning text-dark'
                          }`}
                      >
                        Seleccionados: {editSectionForm.selectedBlockCodes.size} de{' '}
                        {sectionToEdit.requiredBlocksCount} requeridos
                      </span>
                    </div>

                    <div className="table-responsive border rounded-3 overflow-hidden">
                      <table className="table table-bordered mb-0 text-center align-middle">
                        <thead className="table-light">
                          <tr>
                            <th style={{ width: '130px' }}>Módulo</th>
                            {DAYS.map((d) => (
                              <th key={d.key}>{d.label}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {MODULES.map((m) => (
                            <tr key={m.num}>
                              <td className="bg-light p-2">
                                <div className="fw-bold" style={{ fontSize: '13px' }}>
                                  Módulo {m.num}
                                </div>
                                <div className="text-muted font-monospace" style={{ fontSize: '11px' }}>
                                  {m.range}
                                </div>
                              </td>
                              {DAYS.map((d) => {
                                const code = `${d.key}${m.num}`;
                                const isSelected = editSectionForm.selectedBlockCodes.has(code);
                                const isBusy = teacherBusyBlocksInEdit.has(code);

                                return (
                                  <td
                                    key={code}
                                    className="p-1"
                                    style={{
                                      backgroundColor: isSelected
                                        ? '#dbeafe'
                                        : isBusy
                                          ? '#fee2e2'
                                          : 'white',
                                      cursor: isBusy ? 'not-allowed' : 'pointer',
                                    }}
                                    onClick={() => {
                                      if (isBusy) {
                                        setAlert({
                                          type: 'danger',
                                          message: `Conflicto de horario: El docente seleccionado ya tiene otra sección asignada en el bloque ${code}.`,
                                        });
                                        return;
                                      }
                                      handleToggleBlockEdit(code);
                                    }}
                                  >
                                    <div className="p-2 rounded-2 d-flex flex-column align-items-center justify-content-center h-100">
                                      <span
                                        className={`font-monospace fw-bold ${isSelected
                                          ? 'text-primary'
                                          : isBusy
                                            ? 'text-danger'
                                            : 'text-dark'
                                          }`}
                                      >
                                        {code}
                                      </span>
                                      {isSelected && (
                                        <span className="badge bg-primary mt-1" style={{ fontSize: '10px' }}>
                                          Asignado
                                        </span>
                                      )}
                                      {isBusy && (
                                        <span className="badge bg-danger mt-1" style={{ fontSize: '10px' }}>
                                          Ocupado
                                        </span>
                                      )}
                                    </div>
                                  </td>
                                );
                              })}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>

                <div className="modal-footer border-top p-3">
                  <button
                    type="button"
                    className="btn btn-light rounded-3"
                    onClick={() => setShowEditSectionModal(false)}
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary rounded-3 fw-semibold px-4"
                    disabled={
                      submitting ||
                      editSectionForm.selectedBlockCodes.size !== sectionToEdit.requiredBlocksCount
                    }
                  >
                    {submitting ? 'Guardando...' : 'Guardar Cambios'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* ================= MODAL: DELETE SECTION CONFIRMATION ================= */}
      {showDeleteSectionModal && sectionToDelete && (
        <div
          className="modal show d-block"
          tabIndex="-1"
          style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
        >
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border-0 shadow-lg rounded-4">
              <div className="modal-header border-0 pb-0">
                <h5 className="modal-title fw-bold text-danger">
                  <i className="bi bi-trash-fill me-2"></i>
                  Eliminar Sección
                </h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowDeleteSectionModal(false)}
                ></button>
              </div>
              <div className="modal-body p-4">
                <p className="mb-3">
                  ¿Confirmas la eliminación permanente de la sección{' '}
                  <strong className="font-monospace text-dark">
                    {sectionToDelete.sectionCode}
                  </strong>{' '}
                  ({sectionToDelete.courseName})?
                </p>
                {sectionToDelete.enrolledCount > 0 ? (
                  <div className="alert alert-danger mb-0 small">
                    <i className="bi bi-exclamation-triangle-fill me-2"></i>
                    Esta sección cuenta con {sectionToDelete.enrolledCount} estudiantes inscritos y
                    <strong> no puede ser eliminada</strong>.
                  </div>
                ) : (
                  <div className="alert alert-warning mb-0 small">
                    Esta acción liberará los bloques horarios del docente asignado y eliminará la oferta
                    académica de la asignatura en este período.
                  </div>
                )}
              </div>
              <div className="modal-footer border-0 pt-0">
                <button
                  type="button"
                  className="btn btn-light rounded-3"
                  onClick={() => setShowDeleteSectionModal(false)}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="btn btn-danger rounded-3 fw-semibold px-4"
                  onClick={handleConfirmDeleteSection}
                  disabled={submitting || sectionToDelete.enrolledCount > 0}
                >
                  {submitting ? 'Eliminando...' : 'Eliminar Sección'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AcademicOfferPage;
