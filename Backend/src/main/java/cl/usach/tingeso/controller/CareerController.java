package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.CareerResponseDTO;
import cl.usach.tingeso.service.CareerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CareerController exposes REST endpoints for career queries.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/careers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class CareerController {

    private final CareerService careerService;

    /**
     * Retrieves all active careers.
     *
     * @return 200 OK with list of active careers
     */
    @GetMapping
    public ResponseEntity<List<CareerResponseDTO>> getActiveCareers() {
        return ResponseEntity.ok(careerService.getActiveCareers());
    }
}
