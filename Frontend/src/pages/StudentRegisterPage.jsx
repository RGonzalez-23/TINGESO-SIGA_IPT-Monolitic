import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import StudentDataService from '../services/student.service';
import CareerDataService from '../services/career.service';

const StudentRegisterPage = () => {
  const navigate = useNavigate();
  const [careers, setCareers] = useState([]);
  const [formData, setFormData] = useState({
    run: '',
    firstName: '',
    paternalLastName: '',
    maternalLastName: '',
    careerCode: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const loadCareers = useCallback(async () => {
    try {
      const response = await CareerDataService.getActiveCareers();
      setCareers(response.data);
      if (response.data.length > 0) {
        setFormData((prev) => ({ ...prev, careerCode: response.data[0].code }));
      }
    } catch (err) {
      console.error('Error loading careers:', err);
    }
  }, []);

  useEffect(() => {
    loadCareers();
  }, [loadCareers]);

  // Chilean RUN Check-digit calculation
  const calculateRunDV = (runBody) => {
    if (!runBody || !/^[0-9]+$/.test(runBody)) return '';
    let sum = 0;
    let mul = 2;
    for (let i = runBody.length - 1; i >= 0; i--) {
      sum += parseInt(runBody[i]) * mul;
      mul = mul === 7 ? 2 : mul + 1;
    }
    const rem = 11 - (sum % 11);
    if (rem === 11) return '0';
    if (rem === 10) return 'K';
    return rem.toString();
  };

  const isRunValid = () => {
    const cleaned = formData.run.trim().toUpperCase().replace(/\./g, '');
    if (!/^[0-9]{7,8}-[0-9K]$/.test(cleaned)) return false;
    const [body, dv] = cleaned.split('-');
    return calculateRunDV(body) === dv;
  };

  const sanitizeForEmail = (text) => {
    if (!text) return '';
    return text
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]/g, '');
  };

  const emailPreview = () => {
    const first = sanitizeForEmail(formData.firstName.split(' ')[0]);
    const paternal = sanitizeForEmail(formData.paternalLastName);
    if (first && paternal) {
      return `${first}.${paternal}@sigaipt.cl`;
    }
    return 'nombre.apellido@sigaipt.cl';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isRunValid()) {
      setError('El RUN ingresado no tiene un dígito verificador válido (Módulo 11).');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const cleaned = formData.run.trim().toUpperCase().replace(/\./g, '');
      const payload = {
        ...formData,
        run: cleaned,
      };

      const response = await StudentDataService.create(payload);
      setSuccess(`¡Estudiante registrado con éxito con correo ${response.data.email}!`);
      setTimeout(() => {
        navigate(`/students/${response.data.run}`);
      }, 1500);
    } catch (err) {
      setError(err.response?.data || 'Error al registrar el estudiante en el sistema.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container py-4">
      <div className="row justify-content-center">
        <div className="col-lg-8">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <Link to="/students" className="btn btn-outline-secondary btn-sm">
              <i className="bi bi-arrow-left me-1"></i> Volver a la lista
            </Link>
            <span className="badge bg-primary text-uppercase px-3 py-2">Épica 1: Estudiantes</span>
          </div>

          <div className="siga-card">
            <div className="siga-card-header">
              <h4 className="mb-0 fw-bold" style={{ color: 'var(--siga-primary)' }}>
                <i className="bi bi-person-plus-fill me-2"></i>
                Registro de Nuevo Estudiante
              </h4>
            </div>

            <div className="card-body p-4">
              {error && (
                <div className="alert alert-danger" role="alert">
                  <i className="bi bi-exclamation-triangle-fill me-2"></i>
                  {error}
                </div>
              )}

              {success && (
                <div className="alert alert-success" role="alert">
                  <i className="bi bi-check-circle-fill me-2"></i>
                  {success}
                </div>
              )}

              <form onSubmit={handleSubmit}>
                {/* RUN Field */}
                <div className="mb-3">
                  <label className="form-label fw-semibold">
                    RUN / RUT Chileno <span className="text-danger">*</span>
                  </label>
                  <div className="input-group">
                    <span className="input-group-text bg-light">
                      <i className="bi bi-card-heading"></i>
                    </span>
                    <input
                      type="text"
                      className={`form-control font-monospace ${
                        formData.run.length > 7
                          ? isRunValid()
                            ? 'is-valid'
                            : 'is-invalid'
                          : ''
                      }`}
                      placeholder="Ej: 12345678-5 (con guion y sin puntos)"
                      value={formData.run}
                      onChange={(e) => setFormData({ ...formData, run: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-text small">
                    El sistema valida automáticamente el dígito verificador oficial (Módulo 11).
                  </div>
                </div>

                {/* Names */}
                <div className="mb-3">
                  <label className="form-label fw-semibold">
                    Nombres <span className="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Ej: Juan Carlos"
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    required
                  />
                </div>

                <div className="row mb-3">
                  <div className="col-md-6">
                    <label className="form-label fw-semibold">
                      Apellido Paterno <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Ej: Pérez"
                      value={formData.paternalLastName}
                      onChange={(e) => setFormData({ ...formData, paternalLastName: e.target.value })}
                      required
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-semibold">
                      Apellido Materno <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Ej: González"
                      value={formData.maternalLastName}
                      onChange={(e) => setFormData({ ...formData, maternalLastName: e.target.value })}
                      required
                    />
                  </div>
                </div>

                {/* Career Selector */}
                <div className="mb-3">
                  <label className="form-label fw-semibold">
                    Carrera Profesional <span className="text-danger">*</span>
                  </label>
                  <select
                    className="form-select"
                    value={formData.careerCode}
                    onChange={(e) => setFormData({ ...formData, careerCode: e.target.value })}
                    required
                  >
                    {careers.map((c) => (
                      <option key={c.code} value={c.code}>
                        {c.code} - {c.name}
                      </option>
                    ))}
                  </select>
                  <div className="form-text small text-muted">
                    * El estudiante quedará asociado automáticamente al plan de estudios vigente de dicha carrera.
                  </div>
                </div>

                {/* Autogenerated Details Preview Card */}
                <div className="card bg-light border-0 mb-4 p-3 rounded-3">
                  <h6 className="fw-bold mb-2" style={{ color: 'var(--siga-primary)' }}>
                    <i className="bi bi-info-circle-fill me-1"></i> Asignaciones Automáticas por Sistema
                  </h6>
                  <ul className="list-unstyled mb-0 small text-muted">
                    <li className="mb-1">
                      <strong>Correo Institucional: </strong>
                      <span className="font-monospace text-primary fw-semibold">{emailPreview()}</span>
                    </li>
                    <li className="mb-1">
                      <strong>Estado Académico Inicial: </strong>
                      <span className="badge badge-status-regular">REGULAR</span> (Asignado automáticamente)
                    </li>
                    <li>
                      <strong>Cuenta en Keycloak: </strong>
                      Aprovisionamiento automático de credenciales con rol <code>STUDENT</code>.
                    </li>
                  </ul>
                </div>

                <div className="d-flex justify-content-end gap-2">
                  <Link to="/students" className="btn btn-secondary">
                    Cancelar
                  </Link>
                  <button type="submit" className="btn btn-siga-primary px-4" disabled={loading}>
                    {loading ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                        Registrando...
                      </>
                    ) : (
                      <>
                        <i className="bi bi-check-lg me-1"></i> Confirmar y Registrar
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudentRegisterPage;
