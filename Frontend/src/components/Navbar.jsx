import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { switchRole, isAdmin, isTeacher, isStudent } = useAuth();
  const location = useLocation();

  return (
    <nav className="navbar navbar-expand-lg navbar-dark siga-navbar sticky-top">
      <div className="container">
        <Link className="navbar-brand siga-brand d-flex align-items-center" to="/">
          <i className="bi bi-mortarboard-fill me-2 fs-4 text-warning"></i>
          <span>SIGA IPT</span>
        </Link>

        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarContent"
          aria-controls="navbarContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className="collapse navbar-collapse" id="navbarContent">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            {(isAdmin || isTeacher) && (
              <li className="nav-item">
                <Link
                  className={`nav-link ${location.pathname === '/students' ? 'active fw-bold' : ''}`}
                  to="/students"
                >
                  <i className="bi bi-people-fill me-1"></i>
                  {isAdmin ? 'Gestión de Estudiantes' : 'Consulta de Alumnos'}
                </Link>
              </li>
            )}

            {isAdmin && (
              <li className="nav-item">
                <Link
                  className={`nav-link ${location.pathname === '/students/new' ? 'active fw-bold' : ''}`}
                  to="/students/new"
                >
                  <i className="bi bi-person-plus-fill me-1"></i>
                  Registrar Estudiante
                </Link>
              </li>
            )}

            {isStudent && (
              <li className="nav-item">
                <Link
                  className={`nav-link ${location.pathname === '/profile' ? 'active fw-bold' : ''}`}
                  to="/profile"
                >
                  <i className="bi bi-person-badge-fill me-1"></i>
                  Mi Perfil Académico
                </Link>
              </li>
            )}
          </ul>

          {/* Dev Role Switcher */}
          <div className="d-flex align-items-center">
            <span className="text-light me-2 small d-none d-md-inline">
              <i className="bi bi-shield-lock-fill me-1"></i> Rol Activo:
            </span>
            <div className="btn-group btn-group-sm" role="group">
              <button
                type="button"
                className={`btn ${isAdmin ? 'btn-warning text-dark fw-bold' : 'btn-outline-light'}`}
                onClick={() => switchRole('ADMIN')}
              >
                ADMIN
              </button>
              <button
                type="button"
                className={`btn ${isTeacher ? 'btn-warning text-dark fw-bold' : 'btn-outline-light'}`}
                onClick={() => switchRole('TEACHER')}
              >
                DOCENTE
              </button>
              <button
                type="button"
                className={`btn ${isStudent ? 'btn-warning text-dark fw-bold' : 'btn-outline-light'}`}
                onClick={() => switchRole('STUDENT', '22222222-2')}
              >
                ALUMNO
              </button>
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
