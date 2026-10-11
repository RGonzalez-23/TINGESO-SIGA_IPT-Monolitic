package cl.usach.tingeso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * TimeBlockResponseDTO represents schedule block details.
 * Layer: DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeBlockResponseDTO {

    private String code;
    private String dayOfWeek;
    private String dayName;
    private Integer moduleNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private String scheduleRange;

    public static String getSpanishDayName(String dayOfWeek) {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek.toUpperCase()) {
            case "L" -> "Lunes";
            case "M" -> "Martes";
            case "W" -> "Miércoles";
            case "J" -> "Jueves";
            case "V" -> "Viernes";
            default -> dayOfWeek;
        };
    }
}
