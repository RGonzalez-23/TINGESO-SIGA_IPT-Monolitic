package cl.usach.tingeso.config;

import cl.usach.tingeso.entity.CareerEntity;
import cl.usach.tingeso.entity.StudyPlanEntity;
import cl.usach.tingeso.repository.CareerRepository;
import cl.usach.tingeso.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DataInitializerConfig initializes essential domain data on application startup.
 * Seeds the 3 professional careers and active study plans defined in Appendix A.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializerConfig {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;

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
        };
    }
}
