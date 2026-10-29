package ru.mirea.framespace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Локация съёмки, на которую ссылаются заявки. */
@Entity
@Table(name = "location")
public class Location extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;

    protected Location() {
    }

    public Location(UUID id, String code, String name, String address, Integer capacity) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.address = address;
        this.capacity = capacity;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
