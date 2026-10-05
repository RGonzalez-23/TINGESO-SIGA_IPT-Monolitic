# Contexto del Proyecto: SIGA IPT (Monolítico)
**Asignatura:** Técnicas de Ingeniería de Software (TINGESO) - USACH  
**Evaluación:** Evaluación 1 (2026-2)  
**Repositorio:** Monorepo con Backend (Spring Boot) y Frontend (React + Vite)

---

## 1. Visión General del Proyecto
SIGA IPT (Sistema Integrado de Gestión Académica) es una aplicación web monolítica por capas para el Instituto Profesional de Tecnología (IPT). Centraliza la gestión académica para 3 carreras técnicas de 2 años (4 semestres):
- **10450:** Técnico Analista Programador (Plan 2021.3)
- **10451:** Técnico en Análisis de Datos (Plan 2022.3)
- **10452:** Técnico en Ciberseguridad (Plan 2021.3)

---

## 2. Decisiones Arquitectónicas y Tecnológicas

### 2.1 Backend
- **Lenguaje y Versión:** Java 21.
- **Framework:** Spring Boot con Maven Wrapper (`./mvnw`).
- **Paquete Base:** `cl.usach.tingeso`.
- **Arquitectura de 4 capas:** `entity`, `repository`, `service`, `controller`, complementada con `dto`.
- **Regla de Negocio:** Lógica exclusivamente en la capa `service`.
- **Transferencia de Datos:** Uso estricto de **DTOs** para desacoplar entidades JPA de las respuestas/peticiones REST.
- **Manejo de Errores en Endpoints:** Controladores con bloques `try / catch` retornando `ResponseEntity<?>` y códigos de estado HTTP semánticos (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict`, etc.).
- **Persistencia:** Spring Data JPA con driver de **PostgreSQL** para ejecución de la app.
- **Documentación de API:** OpenAPI v3 con Swagger UI mediante `springdoc-openapi-starter-webmvc-ui`.
- **Convención de Idioma:** Código fuente (variables, clases, métodos, comentarios técnicos, DTOs, entidades) **100% en inglés**.

### 2.2 Frontend
- **Framework & Tooling:** React 19 con Vite.
- **Componentes:** Componentes funcionales y Hooks.
- **Arquitectura:** **Un único frontend (SPA)** que da soporte a los 3 roles de usuario.
- **Estructura de Directorios:** Organizada por tipo técnico (`pages/`, `components/`, `services/`, `context/`, `hooks/`).
- **Navegación y Rutas:** `react-router-dom` con componentes de rutas protegidas (`ProtectedRoute`) según el rol autenticado.
- **Cliente HTTP:** Axios centralizado en `Frontend/src/services/http-common.js`, con interceptores para inyectar el header `Authorization: Bearer <token>` y capturar errores 401/403.
- **Librería de UI:** Componentes prediseñados listos para usar (MUI o Bootstrap, evaluando compatibilidad con React 19).
- **Paleta de Colores Institucional:**
  - Primario / Barras de navegación: `#2947c0` (Azul)
  - Fondo de vistas y tarjetas: `#fffefe` (Blanco)
  - Acentos, alertas y estados: `#fe0103` (Rojo) y `#f5f523` (Amarillo)
- **Convención de Idioma:** Código fuente en inglés.

### 2.3 Autenticación y Autorización (IAM)
- **Herramienta:** Keycloak desplegado en contenedor Docker.
- **Roles del Sistema:**
  - `ADMIN`: Administrador Académico (gestión curricular, carreras, asignaturas, docentes, estudiantes, secciones y cierre de período).
  - `TEACHER`: Docente (consulta de secciones asignadas e ingreso de calificaciones finales).
  - `STUDENT`: Estudiante (consulta de historial, plan de estudios e inscripción académica).
- **Flujo de Seguridad:**
  - Frontend autentica mediante librería **`keycloak-js`**.
  - Backend protege endpoints REST con **Spring Security + OAuth2 Resource Server**, validando el token JWT emitido por Keycloak.
  - Prohibido almacenar contraseñas en PostgreSQL o implementar mecanismos de autenticación propios.
- **Modelado de Identidades:**
  - **Enlace Token $\leftrightarrow$ Base de Datos:** Se utiliza el **RUN** del usuario (presente en el claim `preferred_username` del JWT).
  - **Entidades de Dominio:** `Student` y `Teacher` se modelan como entidades JPA separadas en PostgreSQL (sin contraseñas).
  - **Administrador Académico:** Reside exclusivamente en Keycloak con sus credenciales y perfil (nombre, RUN, correo, rol `ADMIN`), sin necesidad de una tabla propia en PostgreSQL.
- **Estrategia de Aprovisionamiento en Keycloak:**
  - **Usuarios de prueba iniciales:** Preconfigurados e importados en el arranque de Docker mediante `realm-export.json` (`admin`, docente y estudiante de prueba).
  - **Nuevos estudiantes registrados por Admin:** Aprovisionamiento automático desde el backend (`StudentService`) mediante `keycloak-admin-client`, creando la cuenta con `username` = RUN, correo institucional autogenerado, contraseña inicial y rol `STUDENT`.

### 2.4 Testing y Calidad de Código
- **Herramienta de Cobertura:** **JaCoCo** configurado en Maven para auditar una cobertura de líneas de código $\ge 90\%$ en la capa `service`.
- **Base de Datos para Tests:** **H2 Database en memoria** (con modo de compatibilidad PostgreSQL) para pruebas unitarias e integración rápidas e independientes de Docker.
- **Pruebas de Integración:** Al menos 18 casos de prueba (3 procesos relevantes, con 3 escenarios exitosos y 3 escenarios de rechazo cada uno):
  1. *Inscripción académica (Épica 6)*.
  2. *Cierre de período académico (Épica 7)*.
  3. *Oferta académica y creación de secciones (Épica 5)*.

### 2.5 Estrategia de Entorno, Despliegue y CI/CD

#### 2.5.1 Entorno de Desarrollo (Estrategia Híbrida - Recomendada)
Para maximizar la velocidad de desarrollo (*Developer Experience*), evitar problemas de latencia/rendimiento con volúmenes montados en Windows y conservar configuraciones persistentes, se utiliza un enfoque híbrido durante la construcción de las épicas:
- **Infraestructura en Docker (`docker-compose.dev.yml`):**
  - **PostgreSQL (puerto 5432):** Contenedor oficial con volumen persistente (`pgdata`), evitando instalaciones locales y garantizando persistencia de datos entre reinicios.
  - **Keycloak (puerto 8080):** Contenedor oficial con persistencia en BD o volumen para la configuración del Realm (`siga-ipt-realm`), clientes (`frontend-client`, `backend-api`) y usuarios de prueba con sus roles (`ADMIN`, `TEACHER`, `STUDENT`). Se configura una única vez al inicio y queda disponible de forma permanente.
- **Código de Aplicación en Local (Fast Feedback Loop):**
  - **Frontend:** Ejecución directa mediante `npm run dev` (Vite) en `localhost:5173`. Permite Hot Module Replacement (HMR) instantáneo en milisegundos sin latencia ni bloqueos de sincronización de archivos de Windows/Docker.
  - **Backend:** Ejecución directa mediante Spring Boot (`./mvnw spring-boot:run` o vía IDE en `localhost:8081`) apuntando a la base de datos y Keycloak levantados en Docker (`localhost:5432` y `localhost:8080`).
  - **Tests y Coberura JaCoCo:** Ejecución local inmediata (`./mvnw test`) utilizando base de datos en memoria H2 para feedback ultra rápido en cada iteración de desarrollo.

#### 2.5.2 Fase Final: Empaquetado, Producción y CI/CD
Una vez implementadas y validadas las 7 épicas y alcanzado el umbral de cobertura ($\ge 90\%$):
- **Contenedores y Docker Compose de Producción (`docker-compose.yml`):**
  - Base de datos: PostgreSQL con volumen persistente.
  - Backend: 3 réplicas explícitas (`backend-1`, `backend-2`, `backend-3`).
  - Balanceador de Carga: Nginx en puerto 80 como punto de entrada único (sirviendo Frontend en `/` y balanceando peticiones `/api/` en round-robin hacia el upstream de réplicas de backend).
  - Frontend: 1 instancia empaquetada lista para producción.
  - Keycloak en la misma red interna de Docker.
  - Configuración vía variables de entorno (`.env`).
- **Pipeline de Entrega Continua (CD):** GitHub Actions automatizado:
  1. Checkout de código.
  2. Ejecución de pruebas unitarias y de integración del backend.
  3. Build de imágenes Docker de frontend y backend.
  4. Publicación de imágenes en Docker Hub mediante **GitHub Secrets** (`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`).
- **Despliegue en Producción:** Manual vía Docker Compose en instancia EC2 de AWS conectada a Docker Hub.

---

## 3. Resumen de Reglas de Negocio Clave (Épicas 1 a 7)

1. **Gestión de Estudiantes (Épica 1):**
   - **Atributos de `Student`:**
     - `run`: Único, validado con dígito verificador (Módulo 11), almacenado en formato limpio con guion (ej. `12345678-9`).
     - Nombres separados: `firstName`, `paternalLastName`, `maternalLastName` (con helper `getFullName()` para vistas y reportes).
     - `email`: Único, autogenerado por el sistema con formato `nombre.apellidoPaterno@sigaipt.cl` y validado por expresión regular.
     - `career` y `studyPlan`: Relación `@ManyToOne` (se inicializa con carga básica de las 3 carreras y planes del Apéndice A).
     - `academicStatus`: ENUM (`REGULAR`, `POSTERGACION`, `RETIRO_TEMPORAL`, `EGRESADO`, `ELIMINADO`). Inicializado automáticamente en `REGULAR`.
   - **Reglas de Modificación y Estados:**
     - El Administrador Académico solo puede cambiar estados entre `REGULAR`, `POSTERGACION` y `RETIRO_TEMPORAL`.
     - `EGRESADO` y `ELIMINADO` no pueden asignarse manualmente; se asignan de forma automática durante el cierre de semestre.
     - La carrera y el plan de estudios solo se pueden modificar si el estudiante NO posee inscripciones ni historial académico.
   - **Reglas de Eliminación:**
     - Prohibida la eliminación física si el alumno tiene ramos inscritos o historial académico.
     - El borrado físico (`DELETE /api/students/{run}`) solo se permite si el alumno no registra inscripciones ni historial previo.
   - **Matriz de Acceso y Visibilidad:**
     - `STUDENT`: Solo puede consultar su **propia información e historial académico** (filtrado por su RUN extraído del JWT).
     - `TEACHER`: Puede acceder a la información de contacto/datos de estudiantes en estado `REGULAR` inscritos en sus secciones asignadas (vía botón en la lista del curso). No tiene acceso al historial académico de los alumnos.
     - `ADMIN`: Vista global con buscador de todos los estudiantes, edición de datos permitidos y cambio de estados habilitados. Acceso al historial académico.
   - **Vistas del Frontend:**
     - Vista de detalle de estudiante con botones hacia "Asignaturas Cursadas" y "Calificaciones" (preparadas como enlaces/placeholders para futuras épicas).
2. **Carreras y Planes de Estudio (Épica 2):**
   - Carreras: código único, nombre, descripción, duración 4 semestres, estado `ACTIVA`/`INACTIVA`.
   - Planes: solo uno vigente por carrera a la vez. Al activar un plan nuevo, el anterior pasa automáticamente a `NO VIGENTE`.
3. **Asignaturas y Prerrequisitos (Épica 3):**
   - Asignaturas asociadas a un semestre (1 al 4), créditos SCT ($0 < SCT \le 7$), horas TEL (Teoría, Ejercicios, Laboratorio; suma $< 8$ hrs/semana).
   - Entre 0 y 3 prerrequisitos por asignatura (deben ser de semestres anteriores del mismo plan).
4. **Gestión de Docentes (Épica 4):**
   - RUN único, nombre, correo único, título, grado, estado `ACTIVO`/`INACTIVO`.
   - Solo docentes `ACTIVO` pueden asignarse a secciones. No pueden pasar a inactivos si tienen secciones en períodos `ABIERTO`.
5. **Oferta Académica y Secciones (Épica 5):**
   - Períodos tipo `YYYY-S` (ej. `2027-1`), estados `ABIERTO` o `CERRADO`.
   - Máximo 1 sección por asignatura por período. Cupo máx: 60 estudiantes.
   - Bloques horarios: días (L, M, W, J, V), módulos (M1 a M6 de 80 min). La cantidad de bloques semanales debe coincidir con las horas TEL (1 bloque = 2 hrs pedagógicas).
   - Control de choque de horario para docentes.
6. **Inscripción Académica (Épica 6):**
   - Solo estudiantes `REGULAR` y matriculados.
   - Mínimo 3 y máximo 6 asignaturas (salvo que falten menos de 3 para egresar).
   - Obligatorio inscribir asignaturas pendientes de semestres inferiores antes de inscribir superiores.
   - Verificación de cupos disponibles, prerrequisitos aprobados y sin choques de horario.
7. **Calificaciones y Cierre de Semestre (Épica 7):**
   - Calificación final entre 1,0 y 7,0. Aprobado con nota $\ge 4,0$; reprobado con $< 4,0$.
   - Para cerrar período: todas las inscripciones deben tener nota final registrada.
   - El cierre consolida el historial académico, pasa estudiantes con segunda reprobación a `ELIMINADO`, y a quienes completaron su plan a `EGRESADO`.
   - Tras el cierre, el período pasa a `CERRADO` y se bloquean modificaciones.

---

## 4. Estado de Avance y Entregables

| Módulo / Épica | Backend | Cobertura JaCoCo | Frontend | Estado General |
| :--- | :---: | :---: | :---: | :---: |
| **Infraestructura Dev** | PostgreSQL 16 + Keycloak 24 en Docker | N/A | Vite + Bootstrap | **COMPLETADO** |
| **Épica 1: Gestión de Estudiantes** | Endpoints REST, Validación RUN, Email auto | **97.98%** (97/99 líneas) | Listado, Registro, Ficha, Perfil | **COMPLETADO Y VALIDADO** |
| **Seguridad & IAM: Integración Keycloak** | Spring Security + OAuth2 Resource Server | Pendiente | `keycloak-js` + AuthContext real | **SIGUIENTE PASO INMEDIATO** |
| **Épica 2: Carreras y Planes** | Entidades base creadas | Pendiente | Pendiente | **EN COLA** |
| **Épica 3: Asignaturas y Prerrequisitos** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 4: Gestión de Docentes** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 5: Oferta Académica y Secciones** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 6: Inscripción Académica** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 7: Calificaciones y Cierre** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |

### Resumen de lo Implementado en Épica 1:
- **Backend:**
  - Entidades: `StudentEntity`, `CareerEntity`, `StudyPlanEntity`, `AcademicStatus`.
  - Repositorios: `StudentRepository`, `CareerRepository`, `StudyPlanRepository`.
  - Precarga de datos: `DataInitializerConfig` (3 carreras y planes vigentes del Apéndice A).
  - DTOs: `StudentRegistrationDTO`, `StudentUpdateDTO`, `StudentResponseDTO`, `CareerResponseDTO`.
  - Servicios: `StudentService`, `CareerService`.
  - Controladores REST: `StudentController` (`/api/students`), `CareerController` (`/api/careers`).
  - Pruebas unitarias: `StudentServiceTest` (19 tests) y `CareerServiceTest` (1 test) con Mockito/JUnit 5. Cobertura: **97.98%**.
- **Frontend:**
  - Cliente HTTP: `http-common.js` con Axios e interceptores base.
  - Contexto de Autenticación: `AuthContext.jsx` con conmutador de roles simulado para pruebas de desarrollo (`ADMIN`, `TEACHER`, `STUDENT`).
  - Navegación: `Navbar.jsx` y `App.jsx` con rutas protegidas (`ProtectedRoute`).
  - Vistas: `StudentListPage.jsx` (búsqueda y filtros), `StudentRegisterPage.jsx` (validación RUN Módulo 11 en tiempo real y previsualización de correo), `StudentDetailPage.jsx` (ficha con botones de asignaturas cursadas y calificaciones), `StudentProfilePage.jsx` (vista personal del alumno).

---

## 5. Hoja de Ruta Próxima Sesión: Integración de Seguridad y Autenticación con Keycloak

Antes de continuar con la **Épica 2 (Carreras y Planes)**, conectaremos el flujo de autenticación real con Keycloak para que el sistema opere con login real, JWT emitidos por el servidor de identidad y control de acceso basado en roles (RBAC):

### 5.1 Estado Actual de Keycloak (Docker Dev)
- Contenedor activo: `siga-keycloak-dev` en puerto `8080`.
- Realm configurado en `realm-export.json`: `siga-ipt-realm`.
- Clientes creados:
  - `siga-frontend`: Cliente público para la SPA (Web Origins `http://localhost:5173`, Standard Flow enabled).
  - `siga-backend`: Cliente Resource Server para el backend API en puerto `8081`.
- Roles definidos: `ADMIN`, `TEACHER`, `STUDENT`.
- Usuarios de prueba aprovisionados:
  - Administrador: `admin` (password: `admin123`, rol `ADMIN`).
  - Docente: `11111111-1` (password: `docente123`, rol `TEACHER`).
  - Estudiante: `22222222-2` (password: `alumno123`, rol `STUDENT`).

### 5.2 Tareas Backend (Spring Boot 4-capas)
1. **Dependencias Maven:**
   - Agregar `spring-boot-starter-oauth2-resource-server` y `spring-boot-starter-security` en `Backend/pom.xml`.
2. **Configuración de Properties:**
   - En `application.properties`:
     ```properties
     spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/siga-ipt-realm
     spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/certs
     ```
3. **Clase `SecurityConfig.java` (`cl.usach.tingeso.config`):**
   - Habilitar `@EnableWebSecurity` y `@EnableMethodSecurity`.
   - Configuración de CORS permitiendo `http://localhost:5173`.
   - Configurar `JwtAuthenticationConverter` para extraer los roles de Keycloak ubicados en `realm_access.roles` y mapearlos como `ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT`.
   - Reglas de autorización en endpoints `/api/students/**`:
     - `POST`, `PUT`, `DELETE`, `PATCH`: Exclusivo para `ROLE_ADMIN`.
     - `GET /api/students`: `ROLE_ADMIN` y `ROLE_TEACHER`.
     - `GET /api/students/{run}`: `ROLE_ADMIN`, `ROLE_TEACHER` y `ROLE_STUDENT` (validando que el estudiante autenticado solo consulte su propio RUN con `@PreAuthorize`).
     - Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`): Acceso público (`permitAll()`).
4. **Validación de Tests JaCoCo:**
   - Asegurar que `mvn test` mantenga cobertura $\ge 90\%$ usando `@WithMockUser` o simulando `SecurityContext` sin requerir conexión activa a Keycloak durante la ejecución de pruebas.

### 5.3 Tareas Frontend (React + Vite)
1. **Instalación de Dependencia:**
   - `npm install keycloak-js` en el directorio `Frontend`.
2. **Instancia de Keycloak (`Frontend/src/services/keycloak.js`):**
   - Inicializar cliente apuntando a `http://localhost:8080`, realm `siga-ipt-realm`, clientId `siga-frontend`.
3. **Migración de `AuthContext.jsx`:**
   - Reemplazar el conmutador mock por la inicialización real de Keycloak (`keycloak.init({ onLoad: 'check-sso', checkLoginIframe: false })`).
   - Extraer del token decodificado: `username` (RUN), nombre, correo y roles asignados (`isAdmin`, `isTeacher`, `isStudent`).
   - Funciones `login()` y `logout()` vinculadas a los métodos nativos de Keycloak.
4. **Sincronización con `http-common.js`:**
   - Configurar interceptor de solicitud en Axios para adjuntar el JWT activo: `Authorization: Bearer ${keycloak.token}`.
   - Refrescar automáticamente el token si está próximo a expirar (`keycloak.updateToken(30)`).
5. **Ajuste en `Navbar.jsx`:**
   - Mostrar datos del usuario autenticado (RUN, Nombre, Rol) y botón de cierre de sesión (Logout de Keycloak).
   - Botón de "Iniciar Sesión" si no está autenticado.

---

## 6. Instrucción para Continuar la Próxima Sesión
Al iniciar una nueva sesión en Antigravity desde otra computadora tras clonar/hacer `git pull`:
> *"Lee el archivo `PROJECT_CONTEXT.md` para cargar todo el contexto, arquitectura y estado de avance de SIGA IPT. Procedamos a configurar la autenticación e integración con Keycloak tanto en el backend como en el frontend según la sección 5."*
