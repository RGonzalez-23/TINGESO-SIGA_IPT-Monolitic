package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AcademicPeriodResponseDTO representation of an academic period.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicPeriodResponseDTO {

    private String id;
    private Integer year;
    private Integer semester;
    private Boolean isOpen;
    private long sectionCount;
}
