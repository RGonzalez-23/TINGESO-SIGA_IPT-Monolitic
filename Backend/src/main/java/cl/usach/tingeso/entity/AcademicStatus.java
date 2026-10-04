package cl.usach.tingeso.entity;

/**
 * Academic status enum for students in SIGA IPT.
 * Represents the current administrative and academic situation of a student.
 */
public enum AcademicStatus {
    /**
     * Student enabled academically to continue their studies.
     * Assigned automatically upon registration.
     */
    REGULAR,

    /**
     * Student who has postponed their studies.
     * Can be assigned by the Academic Administrator.
     */
    POSTERGACION,

    /**
     * Student who has temporarily withdrawn from their studies.
     * Can be assigned by the Academic Administrator.
     */
    RETIRO_TEMPORAL,

    /**
     * Student who has passed all subjects in their study plan.
     * Automatically determined during the academic semester close.
     */
    EGRESADO,

    /**
     * Student who failed the same subject for the second time.
     * Automatically determined during the academic semester close.
     */
    ELIMINADO
}
