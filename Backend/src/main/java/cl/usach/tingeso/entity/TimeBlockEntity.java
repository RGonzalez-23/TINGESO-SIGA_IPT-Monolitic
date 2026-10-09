package cl.usach.tingeso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * TimeBlockEntity represents a standardized 80-minute schedule module on a specific day of the week.
 * Days: L (Lunes), M (Martes), W (Miércoles), J (Jueves), V (Viernes).
 * Modules: 1 to 6.
 * Example codes: "L1", "M2", "W4", "V6".
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 */
@Entity
@Table(name = "time_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeBlockEntity {

    /**
     * Unique schedule block code (e.g., "L1", "M2", "W3", "J4", "V6").
     */
    @Id
    @Column(name = "code", length = 5, nullable = false)
    private String code;

    /**
     * Day code: 'L', 'M', 'W', 'J', 'V'.
     */
    @Column(name = "day_of_week", length = 5, nullable = false)
    private String dayOfWeek;

    /**
     * Module index: 1, 2, 3, 4, 5, or 6.
     */
    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    /**
     * Module start time (e.g. 08:15:00).
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Module end time (e.g. 09:35:00).
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
