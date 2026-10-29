package ru.mirea.framespace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Общие технические поля изменяемых сущностей.
 *
 * <p>Поле {@link Version} используется для optimistic locking. При конфликте изменений
 * сервер возвращает 409 вместо перезаписи более новой версии данных.</p>
 *
 * <p>UUID назначается до сохранения, поэтому новизну сущности Spring Data определяет
 * через {@link #isNew()}: без этого {@code save} выполняет merge и возвращает копию.</p>
 */
@MappedSuperclass
public abstract class AuditedVersionedEntity implements Persistable<UUID> {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    /** Заполняет временные метки перед первой вставкой. */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Обновляет техническую метку перед изменением. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public boolean isNew() { return version == null; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version == null ? 0L : version; }
}
