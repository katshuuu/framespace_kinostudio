package ru.mirea.framespace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.framespace.domain.RequestNote;

import java.util.List;
import java.util.UUID;

/** Persistence gateway for request notes. */
public interface RequestNoteRepository extends JpaRepository<RequestNote, UUID> {
    List<RequestNote> findByRequestIdOrderByPinnedDescCreatedAtDesc(UUID requestId);
}
