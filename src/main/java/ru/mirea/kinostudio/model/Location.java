package ru.mirea.kinostudio.model;

import java.time.LocalDateTime;

/**
 * Локация киностудии — павильон, натурная площадка или студия.
 */
public class Location {
    private int id;
    private String name;
    private String address;
    private int capacity;
    private LocationType type;
    private LocalDateTime createdAt;

    public Location() {
    }

    public Location(String name, String address, int capacity, LocationType type) {
        this.name = name;
        this.address = address;
        this.capacity = capacity;
        this.type = type;
    }

    public Location(int id, String name, String address, int capacity, LocationType type, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.capacity = capacity;
        this.type = type;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public LocationType getType() {
        return type;
    }

    public void setType(LocationType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("#%d | %-15s | %-30s | вместимость: %-5d | %s", id, name, address, capacity, type);
    }
}
