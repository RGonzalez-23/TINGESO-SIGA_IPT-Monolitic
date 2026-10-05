import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { authenticated, user, isAdmin, isTeacher, isStudent, login, logout } = useAuth();
  const location = useLocation();

  const getRoleBadge = () => {
    if (isAdmin) return <span className="badge bg-warning text-dark fw-bold">ADMIN</span>;
    if (isTeacher) return <span className="badge bg-info text-dark fw-bold">DOCENTE</span>;
    if (isStudent) return <span className="badge bg-success fw-bold">ESTUDIANTE</span>;
    return null;
  };

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
            {authenticated && (isAdmin || isTeacher) && (
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

            {authenticated && isAdmin && (
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

            {authenticated && isStudent && (
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

          {/* User Auth Info & Actions */}
          <div className="d-flex align-items-center gap-3">
            {authenticated ? (
              <>
                <div className="d-flex flex-column text-end d-none d-sm-block text-white">
                  <div className="d-flex align-items-center justify-content-end gap-2">
                    <span className="fw-semibold small">{user?.name}</span>
                    {getRoleBadge()}
                  </div>
                  <span className="text-light-50 small" style={{ fontSize: '0.75rem' }}>
                    RUN: {user?.run}
                  </span>
                </div>

                <button
                  type="button"
                  className="btn btn-outline-light btn-sm d-flex align-items-center gap-1"
                  onClick={logout}
                  title="Cerrar sesión en Keycloak"
                >
                  <i className="bi bi-box-arrow-right"></i>
                  <span className="d-none d-md-inline">Cerrar Sesión</span>
                </button>
              </>
            ) : (
              <button
                type="button"
                className="btn btn-warning btn-sm fw-bold d-flex align-items-center gap-1 text-dark"
                onClick={login}
              >
                <i className="bi bi-box-arrow-in-right"></i>
                <span>Iniciar Sesión</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
