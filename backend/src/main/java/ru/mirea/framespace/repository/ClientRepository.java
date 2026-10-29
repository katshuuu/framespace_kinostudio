package ru.mirea.framespace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.framespace.domain.Client;

import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for clients. */
public interface ClientRepository extends JpaRepository<Client, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<Client> findByEmailIgnoreCase(String email);

    @Query("select c from Client c where :pattern = '%%' " +
           "or lower(c.fullName) like :pattern escape '\\' " +
           "or lower(c.email) like :pattern escape '\\' " +
           "or c.phone like :pattern escape '\\'")
    Page<Client> search(@Param("pattern") String pattern, Pageable pageable);
}
