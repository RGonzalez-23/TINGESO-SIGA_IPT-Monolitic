package cl.usach.tingeso.config;

import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.CourseEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.repository.CareerRepository;
import cl.usach.tingeso.repository.CourseRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DataInitializerConfig initializes essential domain data on application startup.
 * Seeds the 3 professional careers and active study plans, plus initial subjects
 * from semesters 1 to 3 from Appendix A (leaving semester 4 empty for manual testing).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializerConfig {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final CourseRepository courseRepository;

    @Bean
    public CommandLineRunner initDatabaseData() {
        return args -> {
            if (careerRepository.count() == 0) {
                log.info("Seeding initial careers and study plans from Appendix A...");

                // 1. Técnico Analista Programador (10450)
                CareerEntity tap = CareerEntity.builder()
                        .code("10450")
                        .name("Técnico Analista Programador")
                        .description("Formación de técnicos especialistas en desarrollo de software, programación y aplicaciones.")
                        .durationSemesters(4)
                        .isActive(true)
                        .build();
                careerRepository.save(tap);

                StudyPlanEntity tapPlan = StudyPlanEntity.builder()
                        .code("2021.3")
                        .isActive(true)
                        .career(tap)
                        .build();
                studyPlanRepository.save(tapPlan);

                // 2. Técnico en Análisis de Datos (10451)
                CareerEntity tad = CareerEntity.builder()
                        .code("10451")
                        .name("Técnico en Análisis de Datos")
                        .description("Formación de técnicos capacitados en extracción, procesamiento, modelado y visualización de datos.")
                        .durationSemesters(4)
                        .isActive(true)
                        .build();
                careerRepository.save(tad);

                StudyPlanEntity tadPlan = StudyPlanEntity.builder()
                        .code("2022.3")
                        .isActive(true)
                        .career(tad)
                        .build();
                studyPlanRepository.save(tadPlan);

                // 3. Técnico en Ciberseguridad (10452)
                CareerEntity tcs = CareerEntity.builder()
                        .code("10452")
                        .name("Técnico en Ciberseguridad")
                        .description("Formación de técnicos dedicados a la protección de redes, sistemas y gestión de incidentes informáticos.")
                        .durationSemesters(4)
                        .isActive(true)
                        .build();
                careerRepository.save(tcs);

                StudyPlanEntity tcsPlan = StudyPlanEntity.builder()
                        .code("2021.3")
                        .isActive(true)
                        .career(tcs)
                        .build();
                studyPlanRepository.save(tcsPlan);

                log.info("Initial careers and study plans successfully seeded.");
            }

            // Seed courses if empty
            if (courseRepository.count() == 0) {
                log.info("Seeding subjects from semesters 1 to 3 from Appendix A...");

                studyPlanRepository.findByCareer_CodeAndCode("10450", "2021.3")
                        .or(() -> studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10450"))
                        .ifPresent(this::seedTapCourses);
                studyPlanRepository.findByCareer_CodeAndCode("10451", "2022.3")
                        .or(() -> studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10451"))
                        .ifPresent(this::seedTadCourses);
                studyPlanRepository.findByCareer_CodeAndCode("10452", "2021.3")
                        .or(() -> studyPlanRepository.findByCareer_CodeAndIsActiveTrue("10452"))
                        .ifPresent(this::seedTcsCourses);

                log.info("Appendix A subjects (Semesters 1 to 3) successfully seeded.");
            }
        };
    }

    private void seedTapCourses(StudyPlanEntity plan) {
        Map<String, CourseEntity> m = new HashMap<>();

        // Semestre 1
        m.put("TAP101", saveCourse("TAP101", "Fundamentos de Programación", 1, 2, 2, 2, 6, plan, Set.of()));
        m.put("TAP102", saveCourse("TAP102", "Matemática Aplicada", 1, 2, 2, 0, 5, plan, Set.of()));
        m.put("TAP103", saveCourse("TAP103", "Fundamentos de Computación", 1, 2, 0, 2, 4, plan, Set.of()));
        m.put("TAP104", saveCourse("TAP104", "Bases de Datos I", 1, 2, 0, 2, 5, plan, Set.of()));
        m.put("TAP105", saveCourse("TAP105", "Comunicación Efectiva", 1, 2, 2, 0, 4, plan, Set.of()));
        m.put("TAP106", saveCourse("TAP106", "Inglés Técnico I", 1, 2, 2, 0, 4, plan, Set.of()));

        // Semestre 2
        m.put("TAP201", saveCourse("TAP201", "Programación Orientada a Objetos", 2, 2, 2, 2, 6, plan, Set.of(m.get("TAP101"))));
        m.put("TAP202", saveCourse("TAP202", "Estructuras de Datos", 2, 2, 2, 2, 6, plan, Set.of(m.get("TAP101"))));
        m.put("TAP203", saveCourse("TAP203", "Bases de Datos II", 2, 2, 0, 2, 5, plan, Set.of(m.get("TAP104"))));
        m.put("TAP204", saveCourse("TAP204", "Desarrollo Web I", 2, 2, 0, 4, 6, plan, Set.of(m.get("TAP101"))));
        m.put("TAP205", saveCourse("TAP205", "Sistemas Operativos", 2, 2, 0, 2, 4, plan, Set.of(m.get("TAP103"))));
        m.put("TAP206", saveCourse("TAP206", "Inglés Técnico II", 2, 2, 2, 0, 3, plan, Set.of(m.get("TAP106"))));

        // Semestre 3
        saveCourse("TAP301", "Desarrollo Web II", 3, 2, 0, 4, 6, plan, Set.of(m.get("TAP201"), m.get("TAP204")));
        saveCourse("TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, plan, Set.of(m.get("TAP201")));
        saveCourse("TAP303", "Desarrollo de Aplicaciones Móviles", 3, 2, 0, 4, 6, plan, Set.of(m.get("TAP201")));
        saveCourse("TAP304", "Administración de Bases de Datos", 3, 2, 0, 2, 5, plan, Set.of(m.get("TAP203")));
        saveCourse("TAP305", "Redes y Comunicaciones", 3, 2, 0, 2, 4, plan, Set.of(m.get("TAP205")));
        saveCourse("TAP306", "Taller de Programación", 3, 0, 0, 4, 4, plan, Set.of(m.get("TAP201"), m.get("TAP202")));
    }

    private void seedTadCourses(StudyPlanEntity plan) {
        Map<String, CourseEntity> m = new HashMap<>();

        // Semestre 1
        m.put("TAD101", saveCourse("TAD101", "Fundamentos de Programación", 1, 2, 2, 2, 6, plan, Set.of()));
        m.put("TAD102", saveCourse("TAD102", "Matemática Aplicada", 1, 2, 2, 0, 5, plan, Set.of()));
        m.put("TAD103", saveCourse("TAD103", "Introducción al Análisis de Datos", 1, 2, 0, 2, 5, plan, Set.of()));
        m.put("TAD104", saveCourse("TAD104", "Bases de Datos I", 1, 2, 0, 2, 5, plan, Set.of()));
        m.put("TAD105", saveCourse("TAD105", "Herramientas Computacionales", 1, 0, 0, 4, 4, plan, Set.of()));
        m.put("TAD106", saveCourse("TAD106", "Comunicación Efectiva", 1, 2, 2, 0, 4, plan, Set.of()));

        // Semestre 2
        m.put("TAD201", saveCourse("TAD201", "Programación para Análisis de Datos", 2, 2, 0, 4, 6, plan, Set.of(m.get("TAD101"))));
        m.put("TAD202", saveCourse("TAD202", "Probabilidad y Estadística", 2, 2, 2, 2, 6, plan, Set.of(m.get("TAD102"))));
        m.put("TAD203", saveCourse("TAD203", "Bases de Datos II", 2, 2, 0, 2, 5, plan, Set.of(m.get("TAD104"))));
        m.put("TAD204", saveCourse("TAD204", "Preparación y Calidad de Datos", 2, 2, 0, 4, 5, plan, Set.of(m.get("TAD103"), m.get("TAD101"))));
        m.put("TAD205", saveCourse("TAD205", "Visualización de Datos", 2, 2, 0, 2, 5, plan, Set.of(m.get("TAD103"))));
        m.put("TAD206", saveCourse("TAD206", "Inglés Técnico", 2, 2, 2, 0, 3, plan, Set.of()));

        // Semestre 3
        saveCourse("TAD301", "Estadística Aplicada", 3, 2, 2, 2, 6, plan, Set.of(m.get("TAD202")));
        saveCourse("TAD302", "Minería de Datos", 3, 2, 0, 4, 6, plan, Set.of(m.get("TAD201"), m.get("TAD202")));
        saveCourse("TAD303", "Inteligencia de Negocios", 3, 2, 0, 4, 5, plan, Set.of(m.get("TAD203"), m.get("TAD205")));
        saveCourse("TAD304", "Integración y Procesamiento de Datos", 3, 2, 0, 4, 5, plan, Set.of(m.get("TAD201"), m.get("TAD203")));
        saveCourse("TAD305", "Visualización Avanzada de Datos", 3, 2, 0, 2, 4, plan, Set.of(m.get("TAD205")));
        saveCourse("TAD306", "Ética y Protección de Datos", 3, 2, 2, 0, 4, plan, Set.of(m.get("TAD103")));
    }

    private void seedTcsCourses(StudyPlanEntity plan) {
        Map<String, CourseEntity> m = new HashMap<>();

        // Semestre 1
        m.put("TCS101", saveCourse("TCS101", "Fundamentos de Programación", 1, 2, 2, 2, 6, plan, Set.of()));
        m.put("TCS102", saveCourse("TCS102", "Fundamentos de Sistemas Computacionales", 1, 2, 0, 2, 5, plan, Set.of()));
        m.put("TCS103", saveCourse("TCS103", "Redes de Computadores I", 1, 2, 0, 2, 5, plan, Set.of()));
        m.put("TCS104", saveCourse("TCS104", "Sistemas Operativos", 1, 2, 0, 2, 5, plan, Set.of()));
        m.put("TCS105", saveCourse("TCS105", "Matemática Aplicada", 1, 2, 2, 0, 5, plan, Set.of()));
        m.put("TCS106", saveCourse("TCS106", "Comunicación Efectiva", 1, 2, 2, 0, 4, plan, Set.of()));

        // Semestre 2
        m.put("TCS201", saveCourse("TCS201", "Programación para Ciberseguridad", 2, 2, 0, 4, 5, plan, Set.of(m.get("TCS101"))));
        m.put("TCS202", saveCourse("TCS202", "Redes de Computadores II", 2, 2, 0, 4, 6, plan, Set.of(m.get("TCS103"))));
        m.put("TCS203", saveCourse("TCS203", "Administración de Sistemas Linux", 2, 2, 0, 4, 5, plan, Set.of(m.get("TCS104"))));
        m.put("TCS204", saveCourse("TCS204", "Fundamentos de Ciberseguridad", 2, 2, 2, 2, 6, plan, Set.of(m.get("TCS102"), m.get("TCS103"))));
        m.put("TCS205", saveCourse("TCS205", "Bases de Datos", 2, 2, 0, 2, 5, plan, Set.of(m.get("TCS101"))));
        m.put("TCS206", saveCourse("TCS206", "Inglés Técnico", 2, 2, 2, 0, 3, plan, Set.of()));

        // Semestre 3
        saveCourse("TCS301", "Seguridad de Redes", 3, 2, 0, 4, 6, plan, Set.of(m.get("TCS202"), m.get("TCS204")));
        saveCourse("TCS302", "Seguridad de Sistemas Operativos", 3, 2, 0, 4, 5, plan, Set.of(m.get("TCS203"), m.get("TCS204")));
        saveCourse("TCS303", "Criptografía Aplicada", 3, 2, 2, 2, 5, plan, Set.of(m.get("TCS204")));
        saveCourse("TCS304", "Seguridad de Aplicaciones Web", 3, 2, 0, 4, 5, plan, Set.of(m.get("TCS201"), m.get("TCS204")));
        saveCourse("TCS305", "Monitoreo y Detección de Amenazas", 3, 2, 0, 4, 5, plan, Set.of(m.get("TCS202"), m.get("TCS204")));
        saveCourse("TCS306", "Legislación y Ética en Ciberseguridad", 3, 2, 2, 0, 4, plan, Set.of(m.get("TCS204")));
    }

    private CourseEntity saveCourse(
            String code, String name, int semester, int t, int e, int l, int sct,
            StudyPlanEntity plan, Set<CourseEntity> prerequisites) {
        CourseEntity course = CourseEntity.builder()
                .code(code)
                .name(name)
                .semester(semester)
                .theoryHours(t)
                .exerciseHours(e)
                .laboratoryHours(l)
                .sctCredits(sct)
                .studyPlan(plan)
                .prerequisites(new HashSet<>(prerequisites))
                .build();
        return courseRepository.save(course);
    }
}
