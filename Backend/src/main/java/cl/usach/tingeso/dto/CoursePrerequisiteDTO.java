package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CoursePrerequisiteDTO provides concise prerequisite course information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePrerequisiteDTO {
    private Long id;
    private String code;
    private String name;
    private Integer semester;
}
