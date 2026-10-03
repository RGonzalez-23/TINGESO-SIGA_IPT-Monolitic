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
   - Datos: RUN (único), nombre completo, correo (único), carrera, plan vigente.
   - Estados: `REGULAR` (inicial por defecto), `POSTERGACIÓN`, `RETIRO TEMPORAL`, `EGRESADO` (automático en cierre), `ELIMINADO` (automático al reprobar por 2da vez).
   - Estudiantes con inscripciones o historial no pueden eliminarse físicamente.
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

## 4. Instrucción para Continuar Sesión en Otra Computadora
Al iniciar una nueva sesión en Antigravity desde otra computadora tras clonar/hacer `git pull`:
> *"Lee el archivo `PROJECT_CONTEXT.md` para cargar todo el contexto y decisiones de arquitectura del proyecto SIGA IPT, y retomemos el trabajo desde [indicar Épica o tarea actual]."*
