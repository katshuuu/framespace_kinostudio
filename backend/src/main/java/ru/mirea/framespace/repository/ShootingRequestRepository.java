package ru.mirea.framespace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.framespace.domain.ShootingRequest;
import ru.mirea.framespace.domain.RequestStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for shooting requests, smart specifications and compact analytics. */
public interface ShootingRequestRepository extends JpaRepository<ShootingRequest, UUID>, JpaSpecificationExecutor<ShootingRequest> {
    boolean existsByExternalIdIgnoreCase(String externalId);
    boolean existsByExternalIdIgnoreCaseAndIdNot(String externalId, UUID id);
    Optional<ShootingRequest> findByExternalIdIgnoreCase(String externalId);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndUrgentTrue();
    long countByArchivedFalseAndStatus(RequestStatus status);

    @Query("select coalesce(avg(s.budget), 0) from ShootingRequest s where s.archived = false and s.budget is not null")
    BigDecimal averageBudgetForActive();

    @Query("select coalesce(l.code, 'Без локации'), count(s) from ShootingRequest s left join s.location l " +
           "where s.archived = false group by l.code order by count(s) desc")
    List<Object[]> countActiveByLocation();

    @Query("select s.status, count(s) from ShootingRequest s where s.archived = false group by s.status order by count(s) desc")
    List<Object[]> countActiveByStatus();

    @Query("select s.priorityLevel, count(s) from ShootingRequest s where s.archived = false group by s.priorityLevel order by count(s) desc")
    List<Object[]> countActiveByPriority();
}
