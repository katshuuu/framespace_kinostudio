package ru.mirea.framespace.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.framespace.domain.Role;
import ru.mirea.framespace.domain.ShootingRequest;
import ru.mirea.framespace.domain.RequestNote;
import ru.mirea.framespace.domain.UserAccount;
import ru.mirea.framespace.dto.NoteDtos.NoteRequest;
import ru.mirea.framespace.dto.NoteDtos.NoteResponse;
import ru.mirea.framespace.error.ConflictException;
import ru.mirea.framespace.error.NotFoundException;
import ru.mirea.framespace.repository.RequestNoteRepository;
import ru.mirea.framespace.repository.ShootingRequestRepository;
import ru.mirea.framespace.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Управляет заметками к заявкам и авторскими правами на их изменение. */
@Service
public class NoteService {
    private final RequestNoteRepository notes;
    private final ShootingRequestRepository requests;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(RequestNoteRepository notes, ShootingRequestRepository requests,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.requests = requests;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID requestId) {
        requireRequest(requestId);
        return notes.findByRequestIdOrderByPinnedDescCreatedAtDesc(requestId).stream().map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID requestId, NoteRequest request) {
        ShootingRequest entity = requireRequest(requestId);
        UserAccount actor = currentUser.requireEntity();
        RequestNote note = new RequestNote(UUID.randomUUID(), entity, actor, request.body().trim(), request.pinned());
        notes.saveAndFlush(note);
        audit.record("REQUEST_NOTE_CREATED", "REQUEST_NOTE", note.getId(), "Добавлена заметка к заявке",
                Map.of("requestId", requestId));
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID requestId, UUID noteId, NoteRequest request) {
        RequestNote note = requireNote(requestId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || request.version() != note.getVersion())
            throw new ConflictException("Заметка уже изменена. Обновите данные");
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("REQUEST_NOTE_UPDATED", "REQUEST_NOTE", noteId, "Изменена заметка", Map.of("requestId", requestId));
        notes.flush();
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID requestId, UUID noteId) {
        RequestNote note = requireNote(requestId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        notes.delete(note);
        audit.record("REQUEST_NOTE_DELETED", "REQUEST_NOTE", noteId, "Удалена заметка", Map.of("requestId", requestId));
    }

    private ShootingRequest requireRequest(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private RequestNote requireNote(UUID requestId, UUID noteId) {
        RequestNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (!note.getRequest().getId().equals(requestId)) throw new NotFoundException("Заметка не найдена");
        return note;
    }

    private static void assertOwnerOrAdmin(RequestNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Only note author or administrator can modify it");
        }
    }

    private static NoteResponse toResponse(RequestNote note) {
        return new NoteResponse(note.getId(), note.getRequest().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
