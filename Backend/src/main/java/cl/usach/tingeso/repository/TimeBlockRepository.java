package cl.usach.tingeso.repository;

import cl.usach.tingeso.entity.TimeBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TimeBlockRepository handles persistence for standardized academic schedule blocks.
 * Layer: Repository.
 */
@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlockEntity, String> {

    /**
     * Retrieves all time blocks ordered by day and module index.
     */
    List<TimeBlockEntity> findAllByOrderByDayOfWeekAscModuleNumberAsc();
}
