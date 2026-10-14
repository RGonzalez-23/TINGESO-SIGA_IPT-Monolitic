package cl.usach.tingeso.entity;

/**
 * EnrollmentStatus represents the operational and academic state of a course registration.
 * Corresponds to Epic 6: Academic Enrollment.
 */
public enum EnrollmentStatus {
    /**
     * The student is actively registered in the section for the current academic period.
     */
    INSCRITA,

    /**
     * The course was approved in a completed semester (grade >= 4.0).
     */
    APROBADA,

    /**
     * The course was failed in a completed semester (grade < 4.0).
     */
    REPROBADA,

    /**
     * The enrollment was voluntarily cancelled by the student or admin during an open period.
     * Seat capacity is released back to the section.
     */
    ANULADA
}
