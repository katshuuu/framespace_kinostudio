package ru.mirea.framespace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.framespace.domain.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for study groups. */
public interface LocationRepository extends JpaRepository<Location, UUID> {
    Optional<Location> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Location> findAllByOrderByCodeAsc();
    List<Location> findByActiveTrueOrderByCodeAsc();
}
