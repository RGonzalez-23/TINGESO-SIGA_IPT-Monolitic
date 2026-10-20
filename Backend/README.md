# SIGA IPT - Backend Monolítico (Spring Boot)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-OAuth2%20Resource%20Server-green.svg)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Tests](https://img.shields.io/badge/Tests-182%20passing-brightgreen.svg)]()
[![JaCoCo Coverage](https://img.shields.io/badge/Coverage->90%25-green.svg)]()

Servicio backend monolítico para el **Sistema Integrado de Gestión Académica (SIGA IPT)**, desarrollado con **Java 21** y **Spring Boot 3**. Implementa la lógica de negocio, validaciones transaccionales, seguridad OAuth2/OIDC con Keycloak y persistencia relacional con PostgreSQL.

---

## 1. Tecnologías y Librerías Principales

* **Lenguaje:** Java 21 (LTS)
* **Framework:** Spring Boot 3.4.x (Web, Data JPA, Security, Actuator, Validation)
* **Seguridad:** Spring Security OAuth2 Resource Server (Validación JWT con Keycloak JWKS y PKCE)
* **Base de Datos:** PostgreSQL 16 (Driver JDBC oficial de PostgreSQL, Hibernate ORM)
* **Pruebas Automatizadas:** JUnit 5, Mockito, Spring Boot Test, Base de datos en memoria H2 con dialecto PostgreSQL
* **Control de Cobertura:** JaCoCo 0.8.12 (Informe generado en `target/site/jacoco/`)
* **Documentación API:** SpringDoc OpenAPI 3 / Swagger UI

---

## 2. Estructura de Paquetes (`cl.usach.tingeso`)

```text
Backend/src/main/java/cl/usach/tingeso/
├── config/             # Seguridad Spring Security, CORS, OpenAPI Swagger, Seeder de datos
├── controller/         # Controladores REST expuestos bajo /api/*
├── dto/                # Objetos de transferencia de datos (Request y Response DTOs)
├── entity/             # Entidades JPA (Student, Teacher, Course, Career, Section, etc.)
├── exception/          # Manejador global de excepciones (GlobalExceptionHandler) y excepciones de negocio
├── repository/         # Interfaces Spring Data JPA con consultas JPQL especializadas
├── service/            # Capa de lógica de negocio (transacciones @Transactional, reglas académicas)
└── util/               # Utilidades de negocio (validación y normalización de RUN chileno módulo 11)
```

---

## 3. Variables de Entorno y Configuración

El archivo `src/main/resources/application.properties` se encuentra parametrizado para admitir ejecución local y en contenedores Docker mediante variables de entorno:

| Variable de Entorno | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8081` | Puerto HTTP en el que escucha la aplicación. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/siga_ipt_db` | URL JDBC de conexión a PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuario de la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Contraseña de la base de datos. |
| `KEYCLOAK_JWK_SET_URI` | `http://localhost:8080/realms/siga-ipt-realm/protocol/openid-connect/certs` | Endpoint JWKS para descargar claves públicas y validar JWT. |
| `KEYCLOAK_SERVER_URL` | `http://localhost:8080` | URL base de Keycloak para administración de cuentas. |
| `KEYCLOAK_REALM` | `siga-ipt-realm` | Nombre del Realm configurado en Keycloak. |
| `KEYCLOAK_CLIENT_ID` | `siga-backend-service` | Cliente confidencial para operaciones administrativas en Keycloak. |
| `KEYCLOAK_CLIENT_SECRET` | *(Secret configurado)* | Credencial del cliente confidencial de Keycloak. |

---

## 4. Ejecución Local para Desarrollo

### Requisitos:
* JDK 21 (Eclipse Temurin o compatible).
* Contenedores de Postgres y Keycloak en ejecución (`docker compose -f ../docker-compose.dev.yml up -d`).

### Comandos:
```bash
# Compilar el proyecto
./mvnw clean compile

# Ejecutar en modo desarrollo
./mvnw spring-boot:run

# Empaquetar el JAR ejecutable
./mvnw clean package -DskipTests
```

---

## 5. Suite de Pruebas Automatizadas

El proyecto cuenta con **182 pruebas automatizadas** que abarcan:
* Pruebas unitarias completas de cada servicio con mocks (@ExtendWith(MockitoExtension.class)).
* Pruebas de integración de punta a punta con base de datos H2 en modo PostgreSQL (@SpringBootTest).
* Cobertura de código superior al **90%** en la capa de servicios (`cl.usach.tingeso.service`).

### Ejecución de Pruebas y Reporte de Cobertura:
```bash
./mvnw clean test
```

Para consultar el informe de cobertura JaCoCo tras la ejecución:
Abre en tu navegador el archivo generado:
`Backend/target/site/jacoco/index.html`

### Pruebas de Integración de Procesos Complejos (`src/test/java/cl/usach/tingeso/integration/`):
1. **`EnrollmentProcessIntegrationTest` (Proceso de Inscripción de Asignaturas):**
   * *Éxito 1:* Inscripción regular con cupos disponibles y descuento atómico.
   * *Éxito 2:* Inscripción validando el cumplimiento previo de prerrequisitos aprobados.
   * *Éxito 3:* Anulación de asignatura inscrita con liberación inmediata de cupo en la sección.
   * *Rechazo 1:* Rechazo por conflicto de horario en bloques TEL coincidentes.
   * *Rechazo 2:* Rechazo por prerrequisito no aprobado o pendiente en el historial académico.
   * *Rechazo 3:* Rechazo por sección sin cupos disponibles (capacidad máxima alcanzada).
2. **`PeriodCloseProcessIntegrationTest` (Proceso de Cierre de Semestre y Calificaciones):**
   * *Éxito 1:* Cierre exitoso de período académico con actas docentes 100% completas.
   * *Éxito 2:* Detección y transición automática a estado `EGRESADO` al aprobar la totalidad del plan.
   * *Éxito 3:* Detección y transición automática a estado `ELIMINADO` por reprobación consecutiva doble de una asignatura.
   * *Rechazo 1:* Rechazo del cierre por existencia de actas de notas pendientes o incompletas.
   * *Rechazo 2:* Rechazo por intento de cierre de un período que ya fue cerrado previamente.
   * *Rechazo 3:* Rechazo de modificación o ingreso de calificaciones sobre un período en estado cerrado.
3. **`AcademicOfferProcessIntegrationTest` (Proceso de Gestión de Oferta Académica):**
   * *Éxito 1:* Creación de nuevo período académico cerrando atómicamente el período previo activo.
   * *Éxito 2:* Creación de sección con coincidencia exacta de bloques TEL según el catálogo de la asignatura.
   * *Éxito 3:* Alternancia y control de estado de período académico (`togglePeriodStatus`).
   * *Rechazo 1:* Rechazo de asignación docente por choque de horario en otra sección en el mismo bloque.
   * *Rechazo 2:* Rechazo por duplicidad de sección con la misma letra para la misma asignatura y período.
   * *Rechazo 3:* Rechazo al intentar asignar un docente con estado contractual `INACTIVO`.

---

## 6. Dockerfile Multi-Stage

El archivo [Backend/Dockerfile](file:///c:/Users/Raul/Desktop/USACH/Tingeso%202nd%20Try/Monol%C3%ADtico/TINGESO-SIGA_IPT-Monolitic/Backend/Dockerfile) utiliza un esquema de compilación multi-etapa optimizado:
* **Etapa 1 (Builder):** `maven:3.9.9-eclipse-temurin-21-alpine` resuelve dependencias en caché (`mvn dependency:go-offline`) y compila el JAR sin ejecutar pruebas para velocidad (`mvn clean package -DskipTests`).
* **Etapa 2 (Runtime):** `eclipse-temurin:21-jre-alpine` crea un contenedor liviano con usuario no-root (`appuser`), opciones de memoria de JVM para contenedores (`-XX:+UseContainerSupport`) y puerto `8081` expuesto.
