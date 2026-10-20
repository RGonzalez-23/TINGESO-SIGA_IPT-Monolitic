# SIGA IPT - Frontend SPA (React + Vite)

[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-6.x-purple.svg)](https://vitejs.dev/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-purple.svg)](https://getbootstrap.com/)
[![Keycloak JS](https://img.shields.io/badge/Keycloak%20JS-Adapter-orange.svg)](https://www.keycloak.org/)
[![ESLint](https://img.shields.io/badge/ESLint-0%20errors-brightgreen.svg)]()

Interfaz de usuario web (Single Page Application - SPA) para el **Sistema Integrado de Gestión Académica (SIGA IPT)**, desarrollada con **React 19**, **Vite** y **Bootstrap 5**. Provee una experiencia moderna, fluida y reactiva para administradores, docentes y estudiantes, con autenticación segura Single Sign-On (SSO) mediante Keycloak.

---

## 1. Tecnologías y Librerías

* **Core:** React 19, Vite 6
* **Enrutamiento:** React Router DOM v7
* **Cliente HTTP:** Axios (con interceptores automáticos para Bearer Tokens JWT)
* **Autenticación e Identidad:** `keycloak-js` (Flujo PKCE S256 con renovación transparente de tokens)
* **Diseño y Estilos:** Bootstrap 5.3, Bootstrap Icons, CSS personalizado con variables de color accesibles
* **Calidad de Código:** ESLint con reglas para React Hooks y Refresh

---

## 2. Estructura del Código Fuente (`Frontend/src`)

```text
Frontend/src/
├── assets/             # Logotipos y recursos estáticos
├── components/         # Componentes transversales reutilizables (Navbar, Footer, ProtectedRoute, etc.)
├── context/            # AuthContext (Gestión global de sesión, roles de Keycloak e interceptor de estado)
├── pages/              # Vistas completas del sistema:
│   ├── HomeAdmin.jsx               # Panel de control principal para Administradores
│   ├── TeacherListPage.jsx         # Gestión de docentes (creación, edición, filtros y reset de clave)
│   ├── TeacherProfilePage.jsx      # Perfil del docente autenticado
│   ├── CourseListPage.jsx          # Catálogo de asignaturas y gestión de horas TEL
│   ├── PrerequisiteManagerPage.jsx # Administrador visual de prerrequisitos entre asignaturas
│   ├── AcademicPeriodPage.jsx      # Gestión y apertura de períodos semestrales
│   ├── SectionManagerPage.jsx      # Oferta académica, asignación docente y bloques TEL
│   ├── StudyPlanListPage.jsx       # Versionado y activación de planes de estudio
│   ├── StudyPlanDetailPage.jsx     # Asignación de ramos a niveles de un plan
│   ├── StudentListPage.jsx         # Padrón de estudiantes (filtro de matrícula activa/inactiva)
│   ├── StudentRegisterPage.jsx     # Registro de nuevos estudiantes
│   ├── StudentDetailPage.jsx       # Detalle de alumno, cambio de carrera y actualización de clave
│   ├── StudentEnrollmentPage.jsx   # Portal de inscripción de asignaturas en línea
│   ├── GradesManagementPage.jsx    # Módulo docente de ingreso y actas de calificaciones
│   ├── GradesClosurePage.jsx       # Cierre semestral de actas y consolidación académica
│   ├── AcademicHistoryPage.jsx     # Consulta de avance curricular y notas del alumno
│   ├── StudentProfilePage.jsx      # Perfil del estudiante autenticado
│   ├── WelcomePage.jsx             # Pantalla de bienvenida con cuentas de acceso rápido
│   └── UnauthorizedPage.jsx        # Pantalla de acceso denegado por rol insuficiente
└── services/           # Clientes API:
    ├── http-common.js              # Instancia de Axios con inyección automática de Bearer Token
    └── keycloak.js                 # Inicialización y configuración de la instancia Keycloak
```

---

## 3. Variables de Entorno

La aplicación lee su configuración de entorno durante el build de Vite (`import.meta.env`):

| Variable | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `VITE_API_URL` | `/api` | Prefijo base para las llamadas al API REST (enrutadas por Nginx). |
| `VITE_KEYCLOAK_URL` | `http://localhost:8080` | URL base del servidor Keycloak IAM. |
| `VITE_KEYCLOAK_REALM` | `siga-ipt-realm` | Realm de autenticación de SIGA IPT. |
| `VITE_KEYCLOAK_CLIENT_ID`| `siga-frontend` | ID del cliente público registrado en Keycloak. |

---

## 4. Ejecución Local para Desarrollo

### Requisitos:
* Node.js 20+ o 22+ (LTS).
* npm o yarn.
* Servicios de PostgreSQL y Keycloak levantados (`docker compose -f ../docker-compose.dev.yml up -d`).

### Comandos:
```bash
# Instalar dependencias
npm install

# Iniciar servidor de desarrollo con HMR
npm run dev

# Validar calidad de código y linter
npm run lint

# Construir bundle optimizado de producción
npm run build

# Previsualizar el bundle generado localmente
npm run preview
```

El servidor de desarrollo iniciará por defecto en `http://localhost:5173`.

---

## 5. Integración con Keycloak (SSO)

1. **Flujo de Autenticación:** La aplicación utiliza el flujo estándar de OpenID Connect con **PKCE (Proof Key for Code Exchange)** mediante el método `S256`.
2. **Control de Acceso Basado en Roles (RBAC):** El componente `ProtectedRoute` intercepta cada ruta y valida que el usuario posea el rol correspondiente (`ADMIN`, `TEACHER` o `STUDENT`) en los claims del token JWT (`realm_access.roles`).
3. **Renovación Transparente de Token:** Antes de cada llamada HTTP con Axios, el interceptor en `http-common.js` verifica si el token está próximo a expirar (ventana de 30 segundos) y lo renueva automáticamente antes de enviar la cabecera `Authorization: Bearer <token>`.

---

## 6. Dockerfile Multi-Stage y Configuración Nginx

El archivo [Frontend/Dockerfile](file:///c:/Users/Raul/Desktop/USACH/Tingeso 2nd Try\Monolítico\TINGESO-SIGA_IPT-Monolitic\Frontend\Dockerfile) compila y sirve la SPA de la siguiente forma:
* **Etapa 1 (Builder):** Utiliza `node:22-alpine` para instalar dependencias (`npm ci`) y compilar la aplicación (`npm run build`), recibiendo los argumentos de entorno de Vite como `ARG`.
* **Etapa 2 (Web Server):** Copia los artefactos estáticos generados en `dist/` a un contenedor liviano `nginx:alpine`.
* **Soporte para React Router:** El archivo [Frontend/nginx.conf](file:///c:/Users/Raul/Desktop/USACH/Tingeso 2nd Try\Monolítico\TINGESO-SIGA_IPT-Monolitic\Frontend\nginx.conf) incluye la directiva `try_files $uri $uri/ /index.html =404;`, evitando que al refrescar o acceder directamente a rutas internas del navegador (como `/students` o `/enrollments`) se generen errores 404.
