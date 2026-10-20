# Contexto del Proyecto: SIGA IPT (Monolítico)
**Asignatura:** Técnicas de Ingeniería de Software (TINGESO) - USACH  
**Evaluación:** Evaluación 1 (2026-2)  
**Repositorio:** Monorepo con Backend (Spring Boot) y Frontend (React + Vite)

---

## 1. Visión General del Proyecto
SIGA IPT (Sistema Integrado de Gestión Académica) es una aplicación web monolítica por capas para el Instituto Profesional de Tecnología (IPT). Centraliza la gestión académica para carreras técnicas de 2 años (4 semestres):
- **10450:** Técnico Analista Programador (Plan 2021.3)
- **10451:** Técnico en Análisis de Datos (Plan 2022.3)
- **10452:** Técnico en Ciberseguridad (Plan 2021.3)
- **10453:** Técnico en Inteligencia Artificial (Plan 2026.1 - Carrera y plan configurable agregados para simulación y pruebas)

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
- **Herramienta de Cobertura:** **JaCoCo** configurado en Maven para auditar una cobertura de líneas de código $\ge 90\%$ en la capa `service` (alcanzado **93.96% de líneas** y **90.65% de instrucciones**).
- **Base de Datos para Tests:** **H2 Database en memoria** (con modo de compatibilidad PostgreSQL) para pruebas unitarias e integración rápidas e independientes de Docker.
- **Pruebas de Integración Formales (18 casos implementados y validados al 100%):**
  Implementadas en el paquete `cl.usach.tingeso.integration` con `@SpringBootTest` y `@Transactional` para los 3 procesos más relevantes:
  1. **Inscripción académica ([EnrollmentProcessIntegrationTest.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/test/java/cl/usach/tingeso/integration/EnrollmentProcessIntegrationTest.java) - 6 casos):**
     - *Éxito 1:* Inscripción estándar de 3 materias válidas incrementa cupos ocupados en BD (`enrolledCount++`).
     - *Éxito 2:* Inscripción de materia superior con prerrequisitos previamente aprobados en historial.
     - *Éxito 3:* Anulación de inscripción libera el cupo en la sección (`enrolledCount--`) y pasa a `ANULADA`.
     - *Rechazo 1:* Bloqueo por conflicto de horario entre secciones seleccionadas en el mismo bloque (`BusinessRuleException`).
     - *Rechazo 2:* Bloqueo por intento de inscripción sin cumplir prerrequisitos formativos.
     - *Rechazo 3:* Bloqueo por sección con capacidad máxima completada (sin cupos).
  2. **Cierre de período académico y calificaciones ([PeriodCloseProcessIntegrationTest.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/test/java/cl/usach/tingeso/integration/PeriodCloseProcessIntegrationTest.java) - 6 casos):**
     - *Éxito 1:* Cierre formal con 100% de notas congela el período en `CERRADO` y consolida `APROBADA` / `REPROBADA`.
     - *Éxito 2:* Cierre formal detecta aprobación total del plan y gradúa automáticamente a `EGRESADO`.
     - *Éxito 3:* Cierre formal detecta segunda reprobación en misma asignatura y actualiza automáticamente a `ELIMINADO`.
     - *Rechazo 1:* Bloqueo de cierre si existen actas o calificaciones pendientes.
     - *Rechazo 2:* Bloqueo de cierre sobre un período que ya está en estado `CERRADO`.
     - *Rechazo 3:* Bloqueo de ingreso o modificación de notas en secciones de un período cerrado.
  3. **Oferta académica y creación de secciones ([AcademicOfferProcessIntegrationTest.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/test/java/cl/usach/tingeso/integration/AcademicOfferProcessIntegrationTest.java) - 6 casos):**
     - *Éxito 1:* Creación de nuevo período académico cierra automáticamente cualquier período previo (regla de único período abierto).
     - *Éxito 2:* Creación de sección con docente activo y cantidad de bloques exactamente equivalente al TEL (`totalTEL / 2`).
     - *Éxito 3:* Alternancia de estado de período (`togglePeriodStatus`) reabre y cierra exclusivamente.
     - *Rechazo 1:* Bloqueo por choque de horario del docente en el mismo día y bloque en el mismo período.
     - *Rechazo 2:* Bloqueo por intento de crear dos secciones para la misma asignatura en un mismo período académico.
     - *Rechazo 3:* Bloqueo por intento de asignar un docente en estado `INACTIVO` a una nueva sección.

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
     - **Cambio de Carrera Condicional:** La carrera y el plan de estudios solo se pueden modificar si el estudiante **NO posee inscripciones ni historial académico** en el sistema (`hasEnrollmentsOrAcademicHistory(student)`). Si registra inscripciones o historial previo, el backend rechaza la operación con `BusinessRuleException` y en el frontend ([StudentDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentDetailPage.jsx)) el selector de carrera se bloquea (`disabled`) mostrando un badge `"Bloqueada"`. Si no tiene registros previos, el cambio de carrera está habilitado (badge `"Modificable"`) y al confirmarse se le asigna atómicamente el plan de estudio activo vigente de la nueva carrera.
   - **Reglas de Eliminación:**
     - Prohibida la eliminación física si el alumno tiene ramos inscritos o historial académico.
     - El borrado físico (`DELETE /api/students/{run}`) solo se permite si el alumno no registra inscripciones ni historial previo.
   - **Matriz de Acceso y Visibilidad:**
     - `STUDENT`: Solo puede consultar su **propia información, asignaturas en curso con horario semanal, malla curricular formativa con semáforo e historial académico por períodos** (filtrado por su RUN extraído del JWT o vía `/api/students/me/...`).
     - `TEACHER`: Puede acceder a la información de contacto/datos de estudiantes en estado `REGULAR` inscritos en sus secciones asignadas (vía nómina en `/teacher/sections`). No tiene acceso al historial académico ni expedientes globales de los alumnos.
     - `ADMIN`: Vista global con buscador de todos los estudiantes, filtro de matrícula activa/inactiva, edición de datos permitidos y cambio de estados habilitados. Acceso irrestricto al expediente completo de cualquier estudiante: Asignaturas en Curso con Horario Semanal (`/students/:run/current-courses`), Malla Curricular Formativa (`/students/:run/curriculum`) e Historial Académico por Períodos (`/students/:run/academic-history`).
   - **Vistas del Frontend:**
     - [StudentListPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentListPage.jsx): Nómina general con buscador por nombre/RUN, selector de filtro por estado de matrícula en el período actual (`Todas`, `Matrícula Activa`, `Matrícula Inactiva`) y badges de estado (`.matriculation-active`, `.matriculation-inactive`).
     - [StudentDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentDetailPage.jsx): Expediente detallado del estudiante que incluye la tarjeta de "Historial y Registro Académico" con accesos directos interactivos estilizados (`.siga-action-link`) a "Asignaturas en Curso", "Malla Curricular" e "Historial Académico", conmutador de matrícula activa/inactiva y selector de carrera condicionalmente bloqueado o editable.
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
| **Épica 2: Carreras y Planes** | CRUD carreras y planes, activación | **92% inst / 95% líneas** (55/55 tests) | Vistas admin/teacher, "Mi malla" | **COMPLETADO Y VALIDADO** |
| **Épica 3: Asignaturas y Prerrequisitos** | Endpoints REST, validaciones TEL/SCT, precarga Apéndice A | **93% inst / 96% líneas** (74/74 tests) | Malla interactiva, filtros, modales CRUD con cálculo TEL en vivo | **COMPLETADO Y VALIDADO** |
| **Épica 4: Gestión de Docentes** | Endpoints REST, resolución de colisiones, Keycloak | **94% inst / 96% líneas** (88/88 tests) | Nómina docente, previsualización email, perfil docente y clave | **COMPLETADO Y VALIDADO** |
| **Épica 5: Oferta Académica y Secciones** | Períodos, secciones, bloques TEL, control choque horario | **91% inst / 95.6% líneas** (121/121 tests) | Gestión de períodos, horario semanal visual, modal sección | **COMPLETADO Y VALIDADO** |
| **Épica 6: Inscripción Académica** | 10 reglas de negocio, CRUD inscripciones, endpoints teacher/student | **> 90% líneas** (145/145 tests) | Inscripción estudiante (batch, filtros, grilla semanal interactiva), secciones docente con nómina, toggle matrícula Admin | **COMPLETADO Y VALIDADO** |
| **Épica 7: Calificaciones y Cierre** | Calificaciones finales (1.0-7.0), auditoría y cierre formal, causal eliminación doble repitencia, graduación, historial cronológico y avance formativo | **> 90% líneas** (182/182 tests) | Acta docente con badges en vivo, modal auditoría y cierre formal Admin, vistas de estudiante (cursos en curso, malla semáforo, historial por períodos) y expediente completo para Admin | **COMPLETADO Y VALIDADO** |
| **Pruebas de Integración (Fase 1)** | 18 casos formales (3 procesos: Inscripción, Cierre, Oferta con 3 éxitos y 3 rechazos c/u) | **93.96% líneas** (182/182 tests) | N/A (Backend `@SpringBootTest` + H2 PostgreSQL) | **COMPLETADO Y VALIDADO** |
| **Dockerización Producción (Fase 2)** | Multi-stage Dockerfile (Temurin 21), 3 réplicas Backend, PostgreSQL persistente | N/A | Multi-stage Dockerfile (Node 22 + Nginx SPA) | **COMPLETADO Y VALIDADO** |
| **Balanceador de Carga Nginx (Fase 2)** | Upstream Round-Robin a `backend-1`, `backend-2`, `backend-3` en puerto 8081 | N/A | Proxy a `frontend:80`, punto de entrada único puerto 80 | **COMPLETADO Y VALIDADO** |

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

### 4.3 Épica 3: Gestión de Asignaturas y Prerrequisitos (Completada)
- **Backend Implementado y Validado:**
  - **Entidad JPA:** [CourseEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/CourseEntity.java) con relación `@ManyToOne StudyPlanEntity` y `@ManyToMany Set<CourseEntity> prerequisites`, con restricción única compuesta `(study_plan_id, code)` para unicidad dentro del plan. Horas de trabajo semanal modeladas como enteros: `theoryHours`, `exerciseHours`, `laboratoryHours`.
  - **Repositorio:** [CourseRepository.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/repository/CourseRepository.java) con métodos de consulta por plan ordenados por semestre y código, y validaciones de prerrequisitos dependientes (`countByPrerequisitesContaining`).
  - **DTOs:** [CourseRegistrationDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CourseRegistrationDTO.java), [CourseUpdateDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CourseUpdateDTO.java), [CoursePrerequisiteDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CoursePrerequisiteDTO.java) y [CourseResponseDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/CourseResponseDTO.java) (con desglose individual, formato `T-E-L`, total de horas y lista de códigos de prerrequisitos).
  - **Servicio:** [CourseService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/CourseService.java) con validaciones rigurosas:
    - Semestre válido (1 al 4).
    - Horas TEL individuales no negativas ($\ge 0$), suma total $> 0$ y estrictamente menor a 8 horas semanales ($< 8$).
    - Créditos SCT válidos ($1 \le SCT \le 7$).
    - Prerrequisitos limitados entre 0 y 3 asignaturas ($\le 3$), pertenecientes al mismo plan de estudios y ubicadas estrictamente en semestres anteriores (`prereq.semester < course.semester`).
    - Prohibición de autoreferencia y ciclos directos.
    - Bloqueo de borrado si la asignatura es prerrequisito de otras asignaturas registradas.
  - **Controlador REST:** [CourseController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/CourseController.java) exponiendo `/api/courses` (`GET`, `POST`, `PUT`, `DELETE`) con autorización `@PreAuthorize`.
  - **Precarga de Datos (Apéndice A):** [DataInitializerConfig.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/config/DataInitializerConfig.java) actualizado con la precarga de asignaturas de semestres 1, 2 y 3 para las carreras `10450`, `10451` y `10452` con sus respectivos prerrequisitos (54 asignaturas en total). El semestre 4 se dejó deliberadamente sin asignaturas precargadas para habilitar pruebas manuales en frontend.
  - **Pruebas y Cobertura:**
    - 19 pruebas unitarias en [CourseServiceTest.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/test/java/cl/usach/tingeso/service/CourseServiceTest.java) cubriendo creación, actualización, validaciones de TEL, SCT, semestres, prerrequisitos, borrado y excepciones.
    - Cobertura global de JaCoCo en la capa `service`: **93% de instrucciones** y **96% de líneas** (`CourseService`: 95%). Total: **74/74 pruebas unitarias exitosas**.
- **Frontend Implementado y Validado:**
  - **Servicio:** [course.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/course.service.js).
  - **Panel Admin:** [HomeAdmin.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeAdmin.jsx) cuenta con tarjeta dedicada de "Gestión de Asignaturas" (Épica 3) con acceso directo a carreras y mallas curriculares.
  - **Malla Curricular Interactiva:** [StudyPlanDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudyPlanDetailPage.jsx) enriquecida con:
    - Selector/filtro de visualización ("Todos los semestres" o filtro individual por semestres 1 a 4).
    - Tarjetas de asignaturas con código, nombre, formato `T-E-L` y total de horas, créditos SCT, y badges informativos de prerrequisitos.
    - Modales de creación y edición con cálculo en tiempo real de la suma de horas TEL (bloqueo si $\ge 8$), validación de créditos SCT ($1-7$), y selector múltiple de prerrequisitos que solo lista asignaturas de semestres anteriores (máx. 3).
    - Botón de eliminación con confirmación para administradores.
  - **Calidad:** 0 errores en ESLint (`npm run lint`), compilación limpia en `npm run build`.

---

### 4.4 Épica 4: Gestión de Docentes (Completada)
- **Backend Implementado y Validado:**
  - **Entidad & Enums:** [TeacherEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/TeacherEntity.java) y [AcademicDegree.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/AcademicDegree.java) (`LICENCIATURA`, `MAGISTER`, `DOCTORADO`). RUN chileno como clave primaria (`@Id`), nombres desglosados (`firstName`, `paternalLastName`, `maternalLastName`), correo institucional único, título profesional, grado académico y estado contractual `isActive` (por defecto `true`/`ACTIVO`).
  - **Repositorio:** [TeacherRepository.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/repository/TeacherRepository.java).
  - **Servicio Unificado de Correo Institucional:** [InstitutionalEmailService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/InstitutionalEmailService.java) centraliza la generación de correos `@sigaipt.cl` para estudiantes y docentes con **remoción estricta de tildes y diacríticos** (NFD + `\p{M}`) y **resolución dinámica de colisiones** (añade iniciales progresivas del apellido materno `.x`, `.xy` y sufijos numéricos si se agotan, verificando duplicados simultáneamente en `StudentRepository` y `TeacherRepository`).
  - **DTOs:** [TeacherRegistrationDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/TeacherRegistrationDTO.java), [TeacherUpdateDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/TeacherUpdateDTO.java), [TeacherResponseDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/TeacherResponseDTO.java).
  - **Servicio de Docentes:** [TeacherService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/TeacherService.java) con validaciones de Módulo 11, unicidad de RUN y correo, aprovisionamiento automático en Keycloak con rol `TEACHER` y clave temporal `Siga2026!`, protección contra inactivación si tiene secciones en períodos abiertos y protección contra borrado físico si tiene secciones asociadas.
  - **Controlador REST:** [TeacherController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/TeacherController.java) con endpoints `/api/teachers` (`GET`, `POST`, `PUT`, `DELETE`, y `GET /preview-email`) protegidos por `@PreAuthorize` (`ADMIN` para administración global, `TEACHER` para consulta de su propio perfil).
  - **Precarga de Datos:** [DataInitializerConfig.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/config/DataInitializerConfig.java) precarga docentes de muestra, incluyendo al profesor de prueba en Keycloak *Pedro González* (`11111111-1`, `pedro.gonzalez@sigaipt.cl`).
  - **Seguridad y Cambio de Contraseña Propia:** [UserController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/UserController.java) y [KeycloakUserService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/KeycloakUserService.java) verifican de forma obligatoria la contraseña actual (`currentPassword`) contra Keycloak (ROPC direct access grant) cuando un docente o estudiante cambia su propia contraseña, garantizando que sea correcta y distinta a la nueva, mientras que los administradores pueden realizar reseteos administrativos directamente.
  - **Pruebas y Cobertura:** 98/98 pruebas unitarias exitosas (0 fallos) con cobertura global en la capa `service` de **94% de instrucciones** y **96% de líneas**.
- **Frontend Implementado y Validado:**
  - **Servicios:** [teacher.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/teacher.service.js), [user.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/user.service.js), y utilidad [runValidator.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/utils/runValidator.js) con `sanitizeForEmail` para supresión en vivo de caracteres acentuados.
  - **Administración para Admin:** [TeacherListPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/TeacherListPage.jsx) en `/teachers` con KPIs de docentes, buscador en vivo, filtros por estado (`TODOS`, `ACTIVOS`, `INACTIVOS`), modales de registro con cálculo en vivo de correo institucional y validación Módulo 11, modal de edición con toggle de estado y restablecimiento de contraseña en Keycloak.
  - **Portales y Perfiles (Docente y Estudiante):** [TeacherProfilePage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/TeacherProfilePage.jsx) y [StudentProfilePage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentProfilePage.jsx) exigen el ingreso de la contraseña actual, validación cruzada y actualización segura en Keycloak.
  - **Calidad:** 0 errores en ESLint (`npm run lint`), compilación limpia en `npm run build`.

---

### 4.5 Épica 5: Gestión de Oferta Académica y Secciones (Completada)
- **Backend Implementado y Validado:**
  - **Entidades de Dominio:**
    - [AcademicPeriodEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/AcademicPeriodEntity.java): Clave canónica `id` en formato `"YYYY-S"` (ej. `"2027-1"`), columnas `academic_year` y `semester`, y bandera de estado `isOpen` (por defecto `true`/`ABIERTO`). Constraint de unicidad `uk_period_year_semester`.
    - [TimeBlockEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/TimeBlockEntity.java): 30 módulos estandarizados de 80 minutos cada uno (L1 a V6), días L, M, W, J, V y módulos 1 a 6 con rangos fijos de hora (M1 08:15-09:35, M2 09:50-11:10, M3 11:25-12:45, almuerzo 1h, M4 13:45-15:05, M5 15:20-16:40, M6 16:55-18:15).
    - [SectionEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/SectionEntity.java): Clave numérica autoincremental `Long id`, regla de unicidad estricta por asignatura por período (`uk_section_course_period`), código de sección generado como `"${course.code}-A-1"` (ej. `"TAP101-A-1"`), relación `@ManyToMany` con bloques horarios, validación de capacidad (1 a 60 cupos), contador de inscritos `enrolledCount` (inicia en 0) y docente asignado obligatorio en estado activo.
  - **Servicios & Reglas de Negocio:**
    - [AcademicPeriodService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/AcademicPeriodService.java): Creación de períodos y alternancia de estado ABIERTO/CERRADO garantizando que estrictamente un único período permanezca abierto a la vez (cierre automático de períodos previos al abrir o crear uno nuevo).
    - [TimeBlockService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/TimeBlockService.java): Consulta ordenada de los 30 bloques horarios estandarizados.
    - [SectionService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/SectionService.java): Única sección por asignatura por período, asignación de docente activo, bloques TEL requeridos (`totalTEL / 2`), control de choque de horarios docentes, código `"${course.code}-A-1"`, y protección contra borrado si tiene alumnos.
  - **Controladores REST:** [AcademicPeriodController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/AcademicPeriodController.java), [TimeBlockController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/TimeBlockController.java), y [SectionController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/SectionController.java).
  - **Pruebas y Cobertura:** 121/121 pruebas unitarias exitosas (0 fallos). Cobertura en capa service: **91% de instrucciones** y **95.6% de líneas**.
- **Frontend Implementado y Validado:**
  - **Servicios:** [academic-period.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/academic-period.service.js), [time-block.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/time-block.service.js), y [section.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/section.service.js).
  - **Página de Oferta Académica:** [AcademicOfferPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/AcademicOfferPage.jsx) (`/academic-offer`) con administración de períodos, filtros, KPIs, listado de secciones, horario semanal visual (5x6), y modal interactivo para creación/edición con código de sección `"${course.code}-A-1"`.
  - **Calidad:** 0 errores en ESLint y compilación en Vite.

---



### 4.6 Épica 6: Inscripción Académica (Backend Completado)

- **Backend Implementado:**

  - **Enum:** [EnrollmentStatus.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/EnrollmentStatus.java) con estados `INSCRITA`, `APROBADA`, `REPROBADA`, `ANULADA`.

  - **Entidad JPA:** [EnrollmentEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/EnrollmentEntity.java) — relaciones `@ManyToOne` con `StudentEntity`, `CourseEntity`, `SectionEntity` y `AcademicPeriodEntity`; campos `enrollmentDate`, `cancellationDate`, `status` (enum), `finalGrade` (nullable).

  - **Atributo `isMatriculated`:** Añadido a [StudentEntity.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/entity/StudentEntity.java) (boolean, por defecto `false`). Gestionado exclusivamente por `ADMIN`. DTOs `StudentResponseDTO`, `StudentRegistrationDTO` y `StudentUpdateDTO` actualizados.

  - **Repositorio:** [EnrollmentRepository.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/repository/EnrollmentRepository.java) con queries: `findByStudentRun`, `findByStudentRunAndPeriodId`, `countByStudentRunAndPeriodIdAndStatusIn`, `findByStudentRunAndCourseIdAndStatus`, `existsByStudentRunAndCourseIdAndPeriodId`, entre otras.

  - **DTOs de Inscripción:**

    - [EnrollmentRequestDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/EnrollmentRequestDTO.java): `studentRun`, `sectionId`.

    - [EnrollmentResponseDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/EnrollmentResponseDTO.java): datos completos de la inscripción.

    - [AvailableCourseOfferDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/AvailableCourseOfferDTO.java): oferta disponible para el estudiante (asignatura + sección + bloques + cupos).

    - [EnrolledStudentDTO.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/dto/EnrolledStudentDTO.java): nómina de alumnos de una sección para docentes.

  - **Servicio:** [EnrollmentService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/EnrollmentService.java) con **10 reglas de negocio** validadas al registrar:

    1. Período académico **ABIERTO**.

    2. Estudiante en estado **REGULAR** y con `isMatriculated = true`.

    3. Asignatura perteneciente al **plan de estudios** del estudiante.

    4. Asignatura **no aprobada** previamente (`APROBADA` bloquea; `REPROBADA` permite reinscripción).

    5. **Prerrequisitos** todos en estado `APROBADA`.

    6. **Sin duplicado** en el mismo período académico.

    7. Límite de asignaturas: mínimo 3, máximo 6 (excepto si faltan menos de 3 para egresar).

    8. **Progresión por semestre:** no se puede inscribir asignatura de semestre superior si existen asignaturas de semestres inferiores disponibles con sección y prerrequisitos cumplidos.

    9. **Cupo disponible** en la sección (`enrolledCount < capacity`). Al inscribir, se incrementa `enrolledCount`.

    10. **Sin choque de horario** entre los bloques de las secciones inscritas en el mismo período.

  - **Cancelación de inscripción:** Libera un cupo (`enrolledCount--`) y pasa el estado a `ANULADA`; solo ejecutable con período `ABIERTO`.

  - **Actualizaciones en otros servicios:**

    - [StudentService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/StudentService.java): Bloquea borrado físico y cambio de carrera si el estudiante tiene inscripciones.

    - [SectionService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/SectionService.java): Añadido `getSectionsByTeacher(run)` para consulta de secciones asignadas por docente.

  - **Controladores REST:**

    - [EnrollmentController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/EnrollmentController.java):

      - `POST /api/enrollments/register` — registrar inscripción (rol `STUDENT`).

      - `DELETE /api/enrollments/{id}` — cancelar inscripción (rol `STUDENT`).

      - `GET /api/enrollments/available-offer` — oferta disponible para el alumno (rol `STUDENT`).

      - `GET /api/enrollments/my-enrollments` — inscripciones activas del alumno (rol `STUDENT`).

      - `POST /api/enrollments/seed-past` — sembrar historial previo de prueba (rol `ADMIN`).

    - [SectionController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/SectionController.java) (endpoints adicionales):

      - `GET /api/sections/teacher/my-sections` — secciones asignadas al docente autenticado (rol `TEACHER`).

      - `GET /api/sections/{id}/enrolled-students` — nómina de alumnos inscritos (roles `TEACHER`, `ADMIN`).

    - [TeacherController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/TeacherController.java): `GET /api/teachers` habilitado para `hasAnyRole('ADMIN', 'TEACHER')` permitiendo consulta del catálogo docente institucional en `/academic-offer`, manteniendo creación, edición y borrado protegidos exclusivamente para `ADMIN`.
    - [StudentController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/StudentController.java): `GET /api/students` (listado completo) restringido a `ADMIN`.

  - **Pruebas:** [EnrollmentServiceTest.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/test/java/cl/usach/tingeso/service/EnrollmentServiceTest.java) — cobertura completa de las 10 reglas (éxito + rechazo) + cancelación. 100% passing (145/145 tests en verde con `mvnw test`).

- **Frontend Implementado, Refinado y Validado (Completado y Probado):**
  - [enrollment.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/enrollment.service.js): Servicio cliente para oferta disponible con elegibilidad, inscripciones activas, registro por lote y anulación.
  - [section-roster.service.js](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/services/section-roster.service.js): Servicio para consulta de secciones asignadas al docente autenticado y nómina de alumnos inscritos.
  - [EnrollmentPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/EnrollmentPage.jsx): Módulo integral de auto-servicio de inscripción para estudiantes en `/enrollment`:
    - Selección múltiple de asignaturas (batch) cumpliendo la regla de mínimo 3 y máximo 6 asignaturas por período.
    - Filtros por semestre (1 a 4) y buscador en tiempo real por nombre, código o sección.
    - Panel lateral interactivo `sticky-lg-top` con scroll vertical propio y contenedor interno de asignaturas preseleccionadas con altura acotada (`maxHeight: 135px`), garantizando que el botón "Confirmar Inscripción" nunca quede cortado ni se desborde al sumar múltiples materias.
    - Detección visual en tiempo real de colisiones/topes de horario y advertencia de regla de progresión curricular.
    - Grilla semanal (5x6) interactiva colapsable mediante acordeón, diferenciando asignaturas confirmadas (verde), preseleccionadas (azul) y colisiones (rojo).
    - Indicador visual refinado y discreto para asignaturas ya inscritas: tarjeta con fondo blanco puro (`bg-white`), borde lateral verde sutil (`border-start border-4 border-success`) y badge pill en verde pastel suave (`bg-success bg-opacity-10 text-success border border-success border-opacity-25`).
    - Gestión de inscripciones activas con opción de anulación segura y liberación automática de cupos.
  - [TeacherSectionsPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/TeacherSectionsPage.jsx): Portal docente en `/teacher/sections` con resumen de cursos asignados, porcentaje de ocupación de cupos, horarios, desplegable de nómina oficial de alumnos con buscador interno interactivo en vivo (`Buscar en nómina (nombre, RUN)...`) activo incondicionalmente siempre que la sección tenga alumnos inscritos (`currentRoster.length > 0`) y botón de copiado de correos de la sección.
  - [AcademicOfferPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/AcademicOfferPage.jsx): Vista de consulta general de oferta académica institucional accesible para ADMIN y TEACHER, con carga defensiva no bloqueante de catálogos auxiliares mediante `.catch()`.
  - [StudentDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentDetailPage.jsx): Switch administrativo para conmutar `isMatriculated` (estudiante matriculado en el período actual).
  - [StudentListPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentListPage.jsx): Indicador badge de estado de matrícula en el período actual.
  - Navegación y Permisos: [Navbar.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/components/Navbar.jsx), [App.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/App.jsx) y [HomeTeacher.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeTeacher.jsx) ajustados para restringir `/students` únicamente a `ADMIN`, canalizando la consulta de estudiantes del docente exclusivamente a través de sus secciones asignadas (`/teacher/sections`).
  - Calidad: 0 errores en ESLint (`npm run lint`), compilación limpia en `vite build`.

---

## 5. Épica 7: Registro de Calificaciones Finales y Cierre de Semestre (100% Completa)

### 5.1 Reglas de Negocio Implementadas
1. **Registro de Calificaciones Finales por Docente:**
   - Durante un período académico `ABIERTO`, los docentes asignados a una sección registran la calificación final (`finalGrade`, escala `1.0` a `7.0`, redondeada a 1 decimal).
   - Validación estricta: sólo el docente titular de la sección (o un `ADMIN`) puede registrar/modificar notas.
   - En período abierto, las inscripciones permanecen en estado `INSCRITA`.
2. **Criterio de Aprobación y Reprobación:**
   - Si `finalGrade >= 4.0`, la condición es **APROBADO**.
   - Si `finalGrade < 4.0`, la condición es **REPROBADO**.
3. **Auditoría Previa y Cierre Formal del Semestre:**
   - Ejecutable exclusivamente por el Administrador Académico (`ADMIN`).
   - **Condición de Cierre:** El 100% de las inscripciones activas del período deben contar con calificación final registrada.
   - Si faltan notas (`canClose = false`), el sistema detalla todas las secciones incompletas (código, asignatura, docente asignado, cantidad de alumnos pendientes).
   - Al ejecutarse el cierre formal:
     - El período pasa a `CERRADO` (`isOpen = false`), congelando las calificaciones en modo sólo lectura.
     - Todas las inscripciones pasan a estado `APROBADA` o `REPROBADA`.
     - **Causal de Eliminación Automática:** Estudiantes que reprueban la **misma asignatura por segunda vez** pasan automáticamente a estado académico **`ELIMINADO`**.
     - **Graduación Automática:** Estudiantes que hayan aprobado la totalidad de las asignaturas de su plan de estudios pasan automáticamente a estado académico **`EGRESADO`**.

### 5.2 Componentes Backend Desarrollados
- **DTOs Creados:**
  - `StudentGradeEntryDTO`: Entrada individual de nota con validaciones `@DecimalMin("1.0")` y `@DecimalMax("7.0")`.
  - `SectionGradesSubmissionDTO`: Lote de calificaciones para envío masivo por sección.
  - `IncompleteSectionDTO`: Auditoría de sección incompleta con desglose de pendientes.
  - `PeriodCloseValidationDTO`: Estado general de auditoría de cierre (`canClose`, pendientes, total activas).
  - `PeriodCloseResultDTO`: Resumen consolidado del cierre (procesadas, aprobadas, reprobadas, egresados, eliminados).
  - `CourseHistoryEntryDTO`, `PeriodHistoryDTO`, `StudentAcademicHistoryDTO`: Estructura jerárquica del expediente académico cronológico por período con cálculo de promedio ponderado semestral y acumulado.
  - `CurriculumCourseProgressDTO`, `StudentCurriculumProgressDTO`: Avance formativo de la malla con estados de colores (`APPROVED`, `FAILED`, `ENROLLED`, `PENDING`), conteo de reprobaciones y prerrequisitos.
- **Servicios:**
  - [GradeService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/GradeService.java): Consulta y registro de calificaciones con control de rol y período abierto/cerrado.
  - [PeriodCloseService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/PeriodCloseService.java): Auditoría de completitud y ejecución del cierre con transición de estados y actualización masiva de situaciones académicas.
  - [AcademicHistoryService.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/service/AcademicHistoryService.java): Cálculo de GPA semestral/acumulado y mapeo del avance curricular formativo.
- **Controladores REST:**
  - [GradeController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/GradeController.java): `GET` y `PUT /api/sections/{sectionId}/grades`.
  - [AcademicPeriodCloseController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/AcademicPeriodCloseController.java): `GET /api/academic-periods/{id}/close-readiness` y `POST /api/academic-periods/{id}/close`.
  - [StudentAcademicHistoryController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/StudentAcademicHistoryController.java):
    - Estudiante: `GET /api/students/me/academic-history` y `GET /api/students/me/curriculum-progress`.
    - Administrador Académico: `GET /api/students/{run}/academic-history` y `GET /api/students/{run}/curriculum-progress`.
  - [EnrollmentController.java](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/src/main/java/cl/usach/tingeso/controller/EnrollmentController.java):
    - Estudiante: `GET /api/enrollments/my-enrollments` (asignaturas activas en curso).
    - Administrador Académico: `GET /api/enrollments/student/{run}/active` (consulta de asignaturas cursando actualmente con horario por cualquier estudiante).
- **Pruebas Automatizadas:** 164 pruebas unitarias y de integración pasando al 100% (`mvn test`), 0 fallas, 0 errores, con cobertura JaCoCo en capa service superior al 90%.

### 5.3 Componentes Frontend Desarrollados y Refinados
- **Servicios:**
  - `grade.service.js`: Conexión a endpoints de calificaciones.
  - `period-close.service.js`: Conexión a auditoría y ejecución de cierre de período.
  - `academic-history.service.js`: Conexión al historial académico y avance de malla del estudiante.
- **Vistas del Estudiante:**
  - [StudentCurrentCoursesPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentCurrentCoursesPage.jsx) (`/student/my-courses`): Detalle de asignaturas actualmente cursadas (estado `INSCRITA`), créditos, docentes asignados y **Malla Horaria Semanal** oficial.
  - [StudentCurriculumPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentCurriculumPage.jsx) (`/student/curriculum`): Malla curricular completa con semáforo formativo:
    - **Verde:** Asignatura Aprobada.
    - **Rojo:** Asignatura Reprobada (indicando contador de reprobaciones, ej. *Reprobada (1 vez)*).
    - **Amarillo:** Asignatura Inscrita en curso actual.
    - **Fondo Blanco:** Asignatura Pendiente.
    - Muestra además los **prerrequisitos formativos** de cada asignatura.
  - [StudentAcademicHistoryPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentAcademicHistoryPage.jsx) (`/student/academic-history`): Historial académico completo ordenado cronológicamente y **dividido por períodos académicos**, con detalle de calificaciones (1.0 - 7.0), condición de aprobación, créditos SCT y promedio ponderado (PPA y PAA).
  - [HomeStudent.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/HomeStudent.jsx): Tarjetas de acceso directo a Cursos en Curso, Malla Curricular, Historial Académico y Perfil.
  - [StudentProfilePage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentProfilePage.jsx): Tarjeta y modal placeholder de "Mi Historial Curricular" eliminados exitosamente.
- **Vista Docente:**
  - [TeacherSectionsPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/TeacherSectionsPage.jsx):
    - Nómina oficial de alumnos con columna interactiva de calificación final (1.0 a 7.0) e indicador badge automático en vivo (*Aprobado* en verde si $\ge 4.0$, *Reprobado* en rojo si $< 4.0$, o *Pendiente*).
    - Botón *"Guardar Calificaciones"* con feedback visual y mensajes de éxito/error.
    - En períodos cerrados, actas congeladas en modo sólo lectura con aviso visual oficial.
- **Vista Administrador:**
  - [AcademicOfferPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/AcademicOfferPage.jsx):
    - Botón *"Cerrar Semestre"* con modal de **Auditoría Previa**.
    - Si faltan calificaciones, bloquea el cierre y despliega la nómina de secciones incompletas con docentes y alumnos pendientes.
    - Si el 100% de las notas están registradas, permite la confirmación formal del cierre y despliega el modal de resumen de resultados (aprobados, reprobados, egresados y alumnos eliminados por doble repitencia).
  - [StudentDetailPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentDetailPage.jsx):
    - Tarjeta "Historial y Registro Académico" con botones de navegación con clase `.siga-action-link` (efecto hover con fondo claro `#eff6ff`, borde azul y sombra sin pérdida de legibilidad de texto):
      - **"Asignaturas en Curso":** redirige a `/students/:run/current-courses` permitiendo al Admin visualizar las asignaturas inscritas y horario semanal del estudiante consultado.
      - **"Malla Curricular":** redirige a `/students/:run/curriculum` mostrando la malla con semáforo formativo del estudiante consultado.
      - **"Historial Académico":** redirige a `/students/:run/academic-history` con el expediente completo por períodos académicos.
    - **Bloqueo dinámico de carrera:** selector de carrera deshabilitado si `student.hasAcademicHistory` es verdadero con badge de alerta "Bloqueada" y validación atómica en backend.
  - [StudentListPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentListPage.jsx):
    - Selector interactivo de filtro por estado de matrícula (`Todas`, `Matrícula Activa`, `Matrícula Inactiva`) con badges distintivos `.matriculation-active` e `.matriculation-inactive`.
  - **Reutilización Multirrol de Vistas Académicas:**
    - [StudentCurrentCoursesPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentCurrentCoursesPage.jsx), [StudentCurriculumPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentCurriculumPage.jsx) y [StudentAcademicHistoryPage.jsx](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/src/pages/StudentAcademicHistoryPage.jsx) detectan dinámicamente si la URL incluye `:run` (modo Administrador Académico) o si se accede desde la ruta del estudiante (`/student/...`), adaptando automáticamente las llamadas a API, encabezados, alertas y botones de retorno.

---

## 6. Estado Consolidado del Proyecto
- **Épica 1:** Autenticación y Autorización (Keycloak + Spring Security + SPA) $\rightarrow$ **100% Completa y Validada**.
- **Épica 2:** Gestión de Carreras y Planes de Estudio $\rightarrow$ **100% Completa y Validada**.
- **Épica 3:** Gestión de Asignaturas y Mallas Curriculares $\rightarrow$ **100% Completa y Validada**.
- **Épica 4:** Gestión Docente $\rightarrow$ **100% Completa y Validada**.
- **Épica 5:** Oferta Académica y Secciones $\rightarrow$ **100% Completa y Validada**.
- **Épica 6:** Inscripción Académica y Secciones Docentes $\rightarrow$ **100% Completa y Validada**.
- **Épica 7:** Calificaciones Finales y Cierre de Semestre $\rightarrow$ **100% Completa y Validada**.
- **Pruebas de Integración (Requisito 3.4 Pauta - Fase 1):** 18 casos implementados y validados (3 procesos $\times$ [3 éxitos + 3 rechazos]) $\rightarrow$ **100% Completo y Validado**.
- **Dockerización y Producción (Requisito 3.3 Pauta - Fase 2):** Docker Compose de producción, 3 réplicas explícitas de Backend, Nginx Load Balancer (Round-Robin), Frontend Nginx, PostgreSQL con volumen persistente y Keycloak IAM $\rightarrow$ **100% Completo y Validado**.
- **Persistencia y Migración de Datos:** Los datos históricos ingresados en desarrollo (25 estudiantes, 14 docentes y 42 cuentas Keycloak) fueron migrados de forma íntegra desde `siga_pgdata_dev` al volumen nombrado de producción `siga_pgdata`.
- **Documentación Técnica del Repositorio:** Actualizados y profesionalizados los tres archivos README ([README.md](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/README.md), [Backend/README.md](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/README.md) y [Frontend/README.md](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Frontend/README.md)).
- **Calidad y Tests:**
  - Backend: **182 / 182 pruebas unitarias y de integración pasando en verde** (164 unitarias + 18 de integración, `./mvnw test`), BUILD SUCCESS. Cobertura JaCoCo en capa service: **93.96% de líneas** y **90.65% de instrucciones** ($\ge 90\%$).
  - Frontend: **0 errores de linting** (`npm run lint`), compilación limpia (`npm run build`).


