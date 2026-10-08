package cl.usach.tingeso.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AcademicDegree represents the standard academic degrees recognized in Chile.
 * Layer: Entity.
 */
@Getter
@RequiredArgsConstructor
public enum AcademicDegree {
    LICENCIATURA("Licenciatura"),
    MAGISTER("Magíster"),
    DOCTORADO("Doctorado");

    private final String displayName;
}
