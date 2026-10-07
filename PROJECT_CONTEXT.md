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
  - **Nuevos estudiantes registrados por Admin:** Aprovisionamiento automático desde el backend ([KeycloakUserService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/KeycloakUserService.java)) utilizando el Service Account `siga-backend-service`, creando la cuenta con `username` = RUN, correo institucional autogenerado, contraseña inicial temporal (`Siga2026!`, exigiendo cambio obligatorio al primer inicio de sesión mediante la pantalla nativa de Keycloak) y rol `STUDENT`.

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
| **Seguridad & IAM: Integración Keycloak** | Spring Security + OAuth2 Resource Server | **94% líneas** (34/34 tests passing) | `keycloak-js` + Aprovisionamiento + Cambio Clave | **COMPLETADO Y VALIDADO** |
| **Épica 2: Carreras y Planes** | Entidades base creadas | Pendiente | Pendiente | **SIGUIENTE PASO** |
| **Épica 3: Asignaturas y Prerrequisitos** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 4: Gestión de Docentes** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 5: Oferta Académica y Secciones** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 6: Inscripción Académica** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |
| **Épica 7: Calificaciones y Cierre** | Pendiente | Pendiente | Pendiente | **PENDIENTE** |

### Resumen de lo Implementado en Épica 1 y Seguridad (Keycloak):
- **Backend:**
  - Entidades: `StudentEntity`, `CareerEntity`, `StudyPlanEntity`, `AcademicStatus`.
  - Repositorios: `StudentRepository`, `CareerRepository`, `StudyPlanRepository`.
  - Precarga de datos: `DataInitializerConfig` (3 carreras y planes vigentes del Apéndice A).
  - DTOs: `StudentRegistrationDTO`, `StudentUpdateDTO`, `StudentResponseDTO`, `CareerResponseDTO`, `PasswordChangeDTO`.
  - Servicios: `StudentService`, `CareerService`, `KeycloakUserService` (Service Account `siga-backend-service`).
  - Controladores REST: `StudentController` (`/api/students`), `CareerController` (`/api/careers`), `UserController` (`/api/users`).
  - Seguridad & IAM: `SecurityConfig`, `KeycloakRoleConverter`, `RestClientConfig`. Endpoints protegidos con `@PreAuthorize`.
  - Aprovisionamiento Keycloak: Creación automática de usuarios en Keycloak con contraseña temporal `Siga2026!` (exigiendo cambio obligatorio en el primer inicio de sesión) y rol `STUDENT`.
  - Gestión de Contraseñas: Endpoint `PUT /api/users/{username}/password` (Admin puede cambiar clave a cualquiera; docentes y estudiantes solo la suya propia).
  - Pruebas unitarias: `StudentServiceTest` (19 tests), `CareerServiceTest` (1 test), `KeycloakUserServiceTest` (6 tests), `KeycloakRoleConverterTest` (4 tests), `SigaIptApplicationTests` (1 test) y de contexto. Total: **34 tests pasando (0 fallos)** con **94% de cobertura de líneas** en `cl.usach.tingeso.service`.
- **Frontend:**
  - Cliente HTTP: `http-common.js` con Axios e inyección automática del Bearer JWT activo y renovación automática (`updateToken(30)`).
  - Autenticación Real: `keycloak-js` y `AuthContext.jsx` con flujo SSO (`check-sso`, PKCE S256).
  - Navegación & Rutas: `Navbar.jsx` con datos del usuario autenticado, badges por rol y login/logout nativo; `App.jsx` con rutas protegidas (`ProtectedRoute`).
  - Vistas:
    - `WelcomePage.jsx`: Portal institucional con inicio de sesión Keycloak y guía de cuentas de prueba.
    - `StudentListPage.jsx`: Lista con buscador, filtros por estado y botón destacado **"Ver Ficha y Opciones"**.
    - `StudentDetailPage.jsx`: Expediente del estudiante que incluye el botón **"Modificar Estudiante"** para el Admin, abriendo un modal consolidado para actualizar datos personales, carrera, estado académico y opcionalmente restablecer su contraseña en Keycloak.
    - `StudentProfilePage.jsx`: Perfil del alumno con visualización de situación curricular y formulario para **cambiar su propia contraseña**.
    - `StudentRegisterPage.jsx`: Formulario de alta con validación Módulo 11 en tiempo real, previsualización de correo institucional y notificación de clave temporal `Siga2026!`.
  - Linter & Build: 0 errores ESLint; bundle de producción verificado con `vite build`.

---

### 4.2 Épica 2: Gestión de Carreras y Planes de Estudio (Completada)
- **Backend Implementado y Validado:**
  - **Entidades JPA:** [CareerEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/CareerEntity.java) y [StudyPlanEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/StudyPlanEntity.java).
  - **Repositorios:** [CareerRepository.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/repository/CareerRepository.java) y [StudyPlanRepository.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/repository/StudyPlanRepository.java).
  - **DTOs:** [CareerRegistrationDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CareerRegistrationDTO.java), [CareerUpdateDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CareerUpdateDTO.java), [CareerResponseDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CareerResponseDTO.java), [StudyPlanRegistrationDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/StudyPlanRegistrationDTO.java), [StudyPlanResponseDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/StudyPlanResponseDTO.java).
  - **Servicios:**
    - [CareerService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/CareerService.java): CRUD integral, validación de 4 semestres (2 años), unicidad de código, creación del plan inicial vigente y protección contra borrado físico si tiene planes o estudiantes asociados.
    - [StudyPlanService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/StudyPlanService.java): Control de planes por carrera, unicidad de código dentro de la carrera, transición atómica de vigencia (el plan anterior pasa a inactivo sin modificar a los estudiantes ya matriculados) y bloqueo de borrado de planes con alumnos inscritos.
  - **Controladores REST:**
    - [CareerController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/CareerController.java): Endpoints `/api/careers` con control `@PreAuthorize`.
    - [StudyPlanController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/StudyPlanController.java): Endpoints `/api/study-plans` (`GET`, `POST`, `PUT /activate`, `DELETE`).
  - **Pruebas y Cobertura:**
    - 55/55 pruebas unitarias exitosas (0 fallos).
    - Cobertura global de JaCoCo en la capa `service`: **92% de instrucciones** y **95% de líneas** (CareerService: 97%, StudyPlanService: 93%, StudentService: 95%).
- **Frontend Implementado y Validado:**
  - **Arquitectura de Home por Rol (Opción B):**
    - [HomePage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomePage.jsx): Despachador en `/` según rol o [WelcomePage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/WelcomePage.jsx) si no está autenticado.
    - [HomeAdmin.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeAdmin.jsx): Panel de control con tarjetas interactivas de "Gestión de Estudiantes" y "Carreras y Planes de Estudio", resumen y directrices del IPT.
    - [HomeTeacher.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeTeacher.jsx): Dashboard del profesor para consulta de cursos y nóminas de alumnos.
    - [HomeStudent.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeStudent.jsx): Dashboard del alumno con acceso a perfil académico, credenciales y oferta.
  - **Servicios:**
    - [career.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/career.service.js) y [study-plan.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/study-plan.service.js).
  - **Vistas y Seguridad por Rol:**
    - [CareerManagementPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/CareerManagementPage.jsx): Vista protegida exclusivamente para `ADMIN` y `TEACHER` en `/careers`. Los estudiantes no tienen acceso a esta vista ni a los endpoints correspondientes de carreras.
    - [StudyPlanDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudyPlanDetailPage.jsx): Vista dedicada en `/study-plans/:id` para la administración y consulta de planes de estudio individuales:
      - `ADMIN`: Puede ver y administrar el plan (activar vigencia) y cuenta con la base lista para la malla curricular de 4 semestres.
      - `TEACHER`: Puede consultar los planes de estudio en modo solo lectura.
      - `STUDENT`: Solo puede acceder a su **propio plan de estudios asignado** a través del botón **"Mi malla"** en [HomeStudent.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeStudent.jsx) o desde su perfil; si intenta acceder a otro plan por URL, el sistema restringe el acceso de forma segura.
    - Desde [CareerManagementPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/CareerManagementPage.jsx), el botón "Planes" ahora permite acceder directamente a la vista individual del plan de estudios con el botón "Administrar / Ver Malla".
  - **Navegación:**
    - Actualizados [Navbar.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/components/Navbar.jsx) y [App.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/App.jsx).
  - **Calidad de Código Frontend:** 0 errores en ESLint (`npm run lint`), bundle verificado con `npm run build`.

---

## 5. Hoja de Ruta Próxima Sesión: Épica 3 (Gestión de Asignaturas y Malla Curricular)

1. **Modelado de Dominio y Reglas de Negocio:**
   - Asignaturas con código único, nombre, créditos SCT ($0 < SCT \le 7$), horas TEL (Teoría, Ejercicios, Laboratorio con suma $< 8$ hrs/semana), semestre de ubicación (1 al 4).
   - Asociación a Planes de Estudio de la Carrera.
   - Prerrequisitos de asignaturas (0 a 3 asignaturas del mismo plan en semestres estrictamente anteriores).
2. **Backend:**
   - Entidad `SubjectEntity`, repositorio, DTOs, servicio y controlador con `@PreAuthorize`.
   - Pruebas unitarias asegurando JaCoCo $\ge 90\%$.
3. **Frontend:**
   - Servicio `subject.service.js` e integración de la malla curricular con asignaturas y prerrequisitos en la vista [StudyPlanDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudyPlanDetailPage.jsx).

---

## 6. Instrucción para Continuar la Próxima Sesión
Al iniciar una nueva sesión en Antigravity desde otra computadora tras clonar/hacer `git pull`:
> *"Lee el archivo `PROJECT_CONTEXT.md` para cargar todo el contexto, arquitectura y estado de avance de SIGA IPT. Procedamos a desarrollar la Épica 3 (Gestión de Asignaturas y Malla Curricular) siguiendo la sección 5."*
