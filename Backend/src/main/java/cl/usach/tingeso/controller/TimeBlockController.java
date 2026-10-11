package cl.usach.tingeso.controller;

import cl.usach.tingeso.dto.TimeBlockResponseDTO;
import cl.usach.tingeso.service.TimeBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TimeBlockController exposes standardized schedule modules.
 * Corresponds to Epic 5: Academic Offer and Sections Management.
 * Layer: Controller.
 */
@RestController
@RequestMapping("/api/time-blocks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TimeBlockController {

    private final TimeBlockService timeBlockService;

    /**
     * Retrieves all standardized schedule blocks (L1-V6).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimeBlockResponseDTO>> getAllTimeBlocks() {
        return ResponseEntity.ok(timeBlockService.getAllTimeBlocks());
    }
}
