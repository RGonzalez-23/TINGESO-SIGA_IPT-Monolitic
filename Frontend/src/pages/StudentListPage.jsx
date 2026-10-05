import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import StudentDataService from '../services/student.service';
import CareerDataService from '../services/career.service';
import { useAuth } from '../context/AuthContext';

const StudentListPage = () => {
  const { isAdmin, isTeacher } = useAuth();
  const [students, setStudents] = useState([]);
  const [careers, setCareers] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Edit Modal State
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [editFormData, setEditFormData] = useState({
    firstName: '',
    paternalLastName: '',
    maternalLastName: '',
    careerCode: '',
    academicStatus: 'REGULAR',
  });
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const loadStudents = useCallback(async () => {
    try {
      setLoading(true);
      const response = await StudentDataService.getAll();
      setStudents(response.data);
      setError(null);
    } catch {
      setError('Error al cargar la lista de estudiantes. Asegúrate de que el backend esté ejecutándose.');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadCareers = useCallback(async () => {
    try {
      const response = await CareerDataService.getActiveCareers();
      setCareers(response.data);
    } catch (err) {
      console.error('Error loading careers:', err);
    }
  }, []);

  useEffect(() => {
    loadStudents();
    loadCareers();
  }, [loadStudents, loadCareers]);

  const handleDelete = async (run) => {
    if (!window.confirm(`¿Estás seguro de eliminar físicamente al estudiante con RUN ${run}?`)) {
      return;
    }
    try {
      await StudentDataService.delete(run);
      setSuccessMessage(`Estudiante con RUN ${run} eliminado correctamente.`);
      loadStudents();
    } catch (err) {
      alert(err.response?.data || 'Error al eliminar el estudiante.');
    }
  };

  const openEditModal = (student) => {
    setSelectedStudent(student);
    setEditFormData({
      firstName: student.firstName || '',
      paternalLastName: student.paternalLastName || '',
      maternalLastName: student.maternalLastName || '',
      careerCode: student.careerCode || '',
      academicStatus: student.academicStatus || 'REGULAR',
    });
    setIsEditModalOpen(true);
  };

  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    try {
      await StudentDataService.update(selectedStudent.run, editFormData);
      setIsEditModalOpen(false);
      setSuccessMessage(`Estudiante ${selectedStudent.run} actualizado con éxito.`);
      loadStudents();
    } catch (err) {
      alert(err.response?.data || 'Error al actualizar el estudiante.');
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'REGULAR': return 'badge-status-regular';
      case 'POSTERGACION': return 'badge-status-postergacion';
      case 'RETIRO_TEMPORAL': return 'badge-status-retiro';
      case 'EGRESADO': return 'badge-status-egresado';
      case 'ELIMINADO': return 'badge-status-eliminado';
      default: return 'bg-secondary';
    }
  };

  // Filter students based on search term and role access rules
  const filteredStudents = students.filter((st) => {
    // If Teacher: only access students in REGULAR status
    if (isTeacher && st.academicStatus !== 'REGULAR') {
      return false;
    }

    const matchesSearch =
      st.run.toLowerCase().includes(searchTerm.toLowerCase()) ||
      st.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (st.careerName && st.careerName.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesStatus = statusFilter === 'ALL' || st.academicStatus === statusFilter;

    return matchesSearch && matchesStatus;
  });

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="fw-bold mb-1" style={{ color: 'var(--siga-primary)' }}>
            <i className="bi bi-people-fill me-2"></i>
            {isAdmin ? 'Gestión de Estudiantes' : 'Consulta de Alumnos (Docente)'}
          </h2>
          <p className="text-muted small mb-0">
            {isAdmin
              ? 'Administración completa de estudiantes registrados, estados académicos y carreras.'
              : 'Información de alumnos en calidad REGULAR con asignaturas asignadas.'}
          </p>
        </div>
        {isAdmin && (
          <Link to="/students/new" className="btn btn-siga-primary">
            <i className="bi bi-person-plus-fill me-2"></i>
            Nuevo Estudiante
          </Link>
        )}
      </div>

      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {successMessage}
          <button type="button" className="btn-close" onClick={() => setSuccessMessage(null)}></button>
        </div>
      )}

      {error && (
        <div className="alert alert-danger" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
        </div>
      )}

      {/* Filters Bar */}
      <div className="siga-card mb-4 p-3">
        <div className="row g-2 align-items-center">
          <div className="col-md-7">
            <div className="input-group">
              <span className="input-group-text bg-white">
                <i className="bi bi-search text-muted"></i>
              </span>
              <input
                type="text"
                className="form-control"
                placeholder="Buscar por RUN, nombre o carrera..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          </div>
          {isAdmin && (
            <div className="col-md-5">
              <div className="d-flex align-items-center">
                <label className="form-label text-nowrap me-2 mb-0 small text-muted">Estado:</label>
                <select
                  className="form-select"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                >
                  <option value="ALL">Todos los estados</option>
                  <option value="REGULAR">Regular</option>
                  <option value="POSTERGACION">Postergación</option>
                  <option value="RETIRO_TEMPORAL">Retiro Temporal</option>
                  <option value="EGRESADO">Egresado</option>
                  <option value="ELIMINADO">Eliminado</option>
                </select>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Students Table */}
      <div className="siga-card">
        <div className="siga-card-header d-flex justify-content-between align-items-center">
          <span>Estudiantes Encontrados ({filteredStudents.length})</span>
          <button className="btn btn-sm btn-outline-secondary" onClick={loadStudents}>
            <i className="bi bi-arrow-clockwise me-1"></i> Actualizar
          </button>
        </div>
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>RUN</th>
                <th>Nombre Completo</th>
                <th>Correo Institucional</th>
                <th>Carrera</th>
                <th>Plan</th>
                <th>Estado</th>
                <th className="text-end">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="text-center py-4 text-muted">
                    <div className="spinner-border spinner-border-sm me-2" role="status"></div>
                    Cargando estudiantes...
                  </td>
                </tr>
              ) : filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-4 text-muted">
                    No se encontraron estudiantes registrados.
                  </td>
                </tr>
              ) : (
                filteredStudents.map((st) => (
                  <tr key={st.run}>
                    <td className="fw-semibold font-monospace">{st.run}</td>
                    <td>{st.fullName}</td>
                    <td>
                      <small className="text-muted">{st.email}</small>
                    </td>
                    <td>{st.careerName || st.careerCode}</td>
                    <td>
                      <span className="badge bg-light text-dark border">{st.studyPlanCode}</span>
                    </td>
                    <td>
                      <span className={`badge px-2 py-1 rounded-pill ${getStatusBadgeClass(st.academicStatus)}`}>
                        {st.academicStatus}
                      </span>
                    </td>
                    <td className="text-end">
                      <div className="btn-group btn-group-sm">
                        <Link
                          to={`/students/${st.run}`}
                          className="btn btn-outline-primary"
                          title="Ver Ficha y Opciones"
                        >
                          <i className="bi bi-eye-fill"></i>
                        </Link>
                        {isAdmin && (
                          <>
                            <button
                              type="button"
                              className="btn btn-outline-secondary"
                              onClick={() => openEditModal(st)}
                              title="Modificar Estudiante"
                            >
                              <i className="bi bi-pencil-fill"></i>
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => handleDelete(st.run)}
                              title="Eliminar Físicamente"
                            >
                              <i className="bi bi-trash-fill"></i>
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Modal */}
      {isEditModalOpen && (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content siga-card">
              <form onSubmit={handleUpdateSubmit}>
                <div className="modal-header">
                  <h5 className="modal-title fw-bold" style={{ color: 'var(--siga-primary)' }}>
                    <i className="bi bi-pencil-square me-2"></i>
                    Modificar Estudiante ({selectedStudent?.run})
                  </h5>
                  <button
                    type="button"
                    className="btn-close"
                    onClick={() => setIsEditModalOpen(false)}
                  ></button>
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Nombres</label>
                    <input
                      type="text"
                      className="form-control"
                      value={editFormData.firstName}
                      onChange={(e) => setEditFormData({ ...editFormData, firstName: e.target.value })}
                      required
                    />
                  </div>
                  <div className="row mb-3">
                    <div className="col">
                      <label className="form-label small fw-semibold">Apellido Paterno</label>
                      <input
                        type="text"
                        className="form-control"
                        value={editFormData.paternalLastName}
                        onChange={(e) => setEditFormData({ ...editFormData, paternalLastName: e.target.value })}
                        required
                      />
                    </div>
                    <div className="col">
                      <label className="form-label small fw-semibold">Apellido Materno</label>
                      <input
                        type="text"
                        className="form-control"
                        value={editFormData.maternalLastName}
                        onChange={(e) => setEditFormData({ ...editFormData, maternalLastName: e.target.value })}
                        required
                      />
                    </div>
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Carrera</label>
                    <select
                      className="form-select"
                      value={editFormData.careerCode}
                      onChange={(e) => setEditFormData({ ...editFormData, careerCode: e.target.value })}
                    >
                      {careers.map((c) => (
                        <option key={c.code} value={c.code}>
                          {c.code} - {c.name}
                        </option>
                      ))}
                    </select>
                    <div className="form-text small">
                      * Solo se puede cambiar si el estudiante no tiene inscripciones ni historial.
                    </div>
                  </div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Estado Académico (Admin)</label>
                    <select
                      className="form-select"
                      value={editFormData.academicStatus}
                      onChange={(e) => setEditFormData({ ...editFormData, academicStatus: e.target.value })}
                    >
                      <option value="REGULAR">REGULAR</option>
                      <option value="POSTERGACION">POSTERGACION</option>
                      <option value="RETIRO_TEMPORAL">RETIRO_TEMPORAL</option>
                    </select>
                    <div className="form-text small text-danger">
                      * Estados EGRESADO y ELIMINADO están protegidos y solo se asignan en cierre de semestre.
                    </div>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setIsEditModalOpen(false)}
                  >
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-siga-primary">
                    Guardar Cambios
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

export default StudentListPage;
