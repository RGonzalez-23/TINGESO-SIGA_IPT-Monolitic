# SIGA IPT - Sistema Integrado de Gestión Académica (Monolítico)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![Keycloak](https://img.shields.io/badge/Keycloak-24.0.5-purple.svg)](https://www.keycloak.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![JaCoCo Coverage](https://img.shields.io/badge/Coverage->90%25-green.svg)]()

> **Taller de Integración de Software (TINGESO)**  
> **Universidad de Santiago de Chile (USACH) — Semestre 2026-2**  
> **Evaluación 1: Arquitectura Monolítica, Pruebas de Integración y Despliegue en Producción**

---

## 1. Descripción del Sistema

**SIGA IPT** es una plataforma integral de gestión académica y curricular desarrollada bajo una arquitectura monolítica modular. Cubre el ciclo de vida completo de la administración universitaria: desde la definición de planes de estudio y oferta de secciones con bloques horarios TEL (Teoría, Ejercicios, Laboratorio), hasta el proceso de inscripción académica con validación estricta de prerrequisitos, topes de horario y cupos, finalizando con el cierre semestral automático, cálculo de promedios ponderados y asignación de estados académicos (`REGULAR`, `ALERTA`, `ELIMINADO`, `EGRESADO`).

---

## 2. Épicas de Negocio Implementadas

| Épica | Módulo | Funcionalidades Principales |
| :--- | :--- | :--- |
| **Épica 1** | **Gestión de Docentes** | CRUD de docentes, validación de RUN chileno (módulo 11), control de estado contractual (`ACTIVO`/`INACTIVO`), sincronización de cuentas y restablecimiento de claves en Keycloak IAM. |
| **Épica 2** | **Asignaturas y Requisitos** | Catálogo de asignaturas (código, nombre, créditos SCT, horas TEL), relaciones de prerrequisitos obligatorios directos e indirectos sin ciclos. |
| **Épica 3** | **Períodos y Oferta Académica** | Apertura/cierre de períodos (`2027-1`), creación de secciones por asignatura con cupos máximos, asignación de docentes activos y configuración estricta de bloques de horario TEL sin choques docentes. |
| **Épica 4** | **Planes de Estudio** | Creación y versionado de planes curriculares por carrera, asociación de asignaturas por nivel formativo, activación atómica de plan único por carrera. |
| **Épica 5** | **Gestión de Estudiantes** | Registro de alumnos, asignación de carrera y plan activo, filtrado por estado de matrícula (`ACTIVA`/`INACTIVA`), cambio controlado de carrera condicionado a la ausencia de historial académico, aprovisionamiento automático en Keycloak. |
| **Épica 6** | **Inscripción de Asignaturas** | Inscripción en línea con control concurrente: validación de estado regular, prerrequisitos aprobados, control de topes de horario en bloques TEL, descuento atómico de cupos en sección y anulación con liberación inmediata. |
| **Épica 7** | **Cierre Semestral y Calificaciones** | Registro de actas docentes (calificación final 1.0 - 7.0), validación de actas completas, cálculo de reprobaciones consecutivas (doble repitencia genera estado `ELIMINADO`), detección de egreso (`EGRESADO`) y congelamiento inmutable del período cerrado. |

---

## 3. Arquitectura de Despliegue en Producción (Docker)

El sistema implementa de forma estricta la arquitectura de alta disponibilidad solicitada en la rúbrica de la Evaluación 1:

```
                            [ Cliente Web / Navegador ]
                                         │
                                         ▼ (Puerto 80 HTTP)
                           ┌───────────────────────────┐
                           │     siga-nginx-lb         │
                           │ (Nginx Reverse Proxy / LB)│
                           └─────────────┬─────────────┘
                                         │
                 ┌───────────────────────┼───────────────────────┐
                 │ (Round-Robin          │ (Round-Robin          │ (Round-Robin
                 │  /api/)               │  /api/)               │  /api/)
                 ▼                       ▼                       ▼
      ┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
      │   siga-backend-1    │ │   siga-backend-2    │ │   siga-backend-3    │
      │ (Spring Boot :8081) │ │ (Spring Boot :8081) │ │ (Spring Boot :8081) │
      └──────────┬──────────┘ └──────────┬──────────┘ └──────────┬──────────┘
                 │                       │                       │
                 ├───────────────────────┼───────────────────────┤
                 ▼                       ▼                       ▼
      ┌─────────────────────────────────────────────────────────────┐
      │              Red Compartida: siga-network                   │
      └──────┬───────────────────────────┬──────────────────────────┘
             │                           │
             ▼ (:5432)                   ▼ (:8080)
  ┌──────────────────────┐    ┌──────────────────────┐   ┌──────────────────────┐
  │    siga-postgres     │    │    siga-keycloak     │   │    siga-frontend     │
  │ (PostgreSQL 16 Alpine│    │  (Keycloak 24.0.5    │   │  (React 19 + Nginx   │
  │  Vol: siga_pgdata)   │    │   Realm siga-ipt)    │   │   SPA interna :80)   │
  └──────────────────────┘    └──────────────────────┘   └──────────────────────┘
```

### Componentes de la Arquitectura:
1. **Balanceador de Carga Nginx (`siga-nginx-lb`):** Expone el puerto `80:80` hacia el exterior. Distribuye las solicitudes del API (`/api/`) hacia las 3 réplicas del Backend en modo **Round-Robin** y enruta la interfaz de usuario (`/`) hacia el contenedor de Frontend.
2. **3 Réplicas de Backend (`siga-backend-1`, `siga-backend-2`, `siga-backend-3`):** Instancias de Spring Boot 3 / Java 21 corriendo de forma independiente y sin estado, conectadas a la base de datos y al servicio de autenticación.
3. **Única Instancia Frontend (`siga-frontend`):** Contenedor Nginx Alpine sirviendo la compilación optimizada de la SPA en React 19 con enrutamiento HTML5 (`try_files`).
4. **Base de Datos Persistente (`siga-postgres`):** PostgreSQL 16 Alpine con almacenamiento seguro en el volumen nombrado `siga_pgdata`.
5. **Servidor de Identidad (`siga-keycloak`):** Keycloak 24 IAM operando con OAuth2 / OpenID Connect y PKCE, conectado a la base de datos `keycloak_db`.

---

## 4. Guía de Ejecución Rápida

### Requisitos Previos:
* [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución en Windows, Linux o macOS.
* Git instalado.

### Paso 1: Clonar el Repositorio
```bash
git clone https://github.com/RGonzalez-23/TINGESO-SIGA_IPT-Monolitic.git
cd TINGESO-SIGA_IPT-Monolitic
```

### Paso 2: Iniciar el Entorno de Producción Completo
```bash
docker compose up -d --build
```
> *Nota: La primera ejecución compilará los Dockerfiles multi-stage de Backend y Frontend, descargará las imágenes base y provisionará los contenedores.*

### Paso 3: Verificar el Estado del Clúster
```bash
docker compose ps
```
Los 7 contenedores deben encontrarse en estado `Up` (y PostgreSQL como `healthy`):
* `siga-nginx-lb` (`0.0.0.0:80->80/tcp`)
* `siga-backend-1`, `siga-backend-2`, `siga-backend-3` (`8081/tcp`)
* `siga-frontend` (`80/tcp`)
* `siga-keycloak` (`0.0.0.0:8080->8080/tcp`)
* `siga-postgres` (`0.0.0.0:5432->5432/tcp`)

---

## 5. Puntos de Acceso del Sistema

| Servicio | URL Local | Descripción |
| :--- | :--- | :--- |
| **Aplicación Web SIGA IPT** | [http://localhost](http://localhost) | Portal web completo servido a través del balanceador Nginx (puerto 80). |
| **API REST / Swagger UI** | [http://localhost/swagger-ui/index.html](http://localhost/swagger-ui/index.html) | Documentación interactiva OpenAPI de los endpoints del backend. |
| **Consola Keycloak Admin** | [http://localhost:8080/admin](http://localhost:8080/admin) | Panel de administración de reinos, roles, clientes y credenciales. |

---

## 6. Cuentas de Acceso Preconfiguradas (Keycloak)

Para iniciar sesión en la aplicación ([http://localhost](http://localhost)), puedes utilizar cualquiera de las siguientes credenciales preconfiguradas:

| Rol | RUN / Usuario | Contraseña | Descripción / Permisos |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin` | `admin123` | Control total del sistema: docentes, asignaturas, planes, estudiantes, oferta y cierre semestral. |
| **Docente** | `11111111-1` | `docente123` | Acceso a sus secciones asignadas, visualización de estudiantes inscritos y digitación de actas de notas. |
| **Estudiante** | `22222222-2` | `alumno123` | Acceso a su portal de notas, avance curricular e inscripción/anulación de asignaturas. |
| **Keycloak Master** | `admin` | `admin` | Administrador general de la consola Keycloak en el puerto 8080. |

---

## 7. Estrategia de Pruebas y Cobertura (Fase 1)

El backend cuenta con una completa suite automatizada que valida tanto la lógica de negocio unitaria como los flujos de integración de punta a punta:

* **Total de Pruebas:** **182 pruebas automatizadas** (0 fallos, 0 errores).
* **Cobertura JaCoCo en capa de Servicios:** **> 90%** de líneas e instrucciones.
* **Pruebas de Integración de Procesos Clave (18 casos en total):**
  1. **Proceso de Inscripción de Asignaturas (`EnrollmentProcessIntegrationTest`):** 3 éxitos (inscripción regular, con prerrequisitos cumplidos, anulación con liberación de cupo) y 3 rechazos (conflicto de bloque horario TEL, prerrequisito pendiente, sección sin cupos disponibles).
  2. **Proceso de Cierre de Semestre (`PeriodCloseProcessIntegrationTest`):** 3 éxitos (cierre con actas completas, graduación a `EGRESADO`, eliminación a `ELIMINADO` por doble reprobación) y 3 rechazos (bloqueo por notas pendientes, período ya cerrado previamente, intento de modificación de notas en período cerrado).
  3. **Proceso de Oferta Académica (`AcademicOfferProcessIntegrationTest`):** 3 éxitos (creación atómica de nuevo período cerrando el anterior, sección con distribución horaria TEL exacta, reapertura/cierre controlado) y 3 rechazos (choque de horario de docente en misma franja, sección duplicada para misma materia en el período, asignación de docente con estado `INACTIVO`).

Para ejecutar toda la suite de pruebas localmente:
```bash
cd Backend
./mvnw clean test
```

---

## 8. Detención y Reinicio de Contenedores

* **Detener los servicios manteniendo los datos:**
  ```bash
  docker compose down
  ```
* **Reiniciar los servicios con los datos existentes:**
  ```bash
  docker compose up -d
  ```
* **Eliminar todos los contenedores y los volúmenes de datos (Reset total):**
  ```bash
  docker compose down -v
  ```

---

## 9. Estructura del Repositorio

```text
TINGESO-SIGA_IPT-Monolitic/
├── Backend/                    # Código fuente del Backend (Spring Boot 3 / Java 21)
│   ├── src/                    # Controladores, Servicios, Repositorios, Entidades, DTOs y Tests
│   ├── Dockerfile              # Dockerfile multi-stage (Maven Builder + JRE 21 Runtime)
│   ├── pom.xml                 # Descriptor de dependencias Maven y plugins (JaCoCo, Surefire)
│   └── README.md               # Documentación técnica específica del Backend
├── Frontend/                   # Código fuente del Frontend (React 19 / Vite)
│   ├── src/                    # Componentes, Páginas, Contexto de Autenticación y Servicios
│   ├── Dockerfile              # Dockerfile multi-stage (Node Builder + Nginx SPA)
│   ├── nginx.conf              # Configuración Nginx para soporte SPA (try_files)
│   └── README.md               # Documentación técnica específica del Frontend
├── docker/                     # Configuraciones complementarias de contenedores
│   ├── keycloak/               # Exportación del Realm de autenticación (realm-export.json)
│   ├── nginx/                  # Configuración del Balanceador de Carga (Round-Robin)
│   └── postgres/               # Script SQL de inicialización de múltiples bases de datos
├── docker-compose.yml          # Orquestación de producción (LB, 3 Réplicas, SPA, DB, IAM)
├── docker-compose.dev.yml      # Entorno ligero de soporte para desarrollo local
├── PROJECT_CONTEXT.md          # Bitácora detallada del proyecto y decisiones arquitectónicas
└── README.md                   # Documentación principal del proyecto
```
