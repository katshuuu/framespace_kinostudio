package ru.mirea.kinostudio.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Заявка на съёмку — основная сущность варианта "Киностудия".
 */
public class ShootingRequest {
    private int id;
    private int clientId;
    private int locationId;
    private String title;
    private String description;
    private LocalDate shootingDate;
    private int durationHours;
    private BigDecimal budget;
    private RequestStatus status;
    private LocalDateTime createdAt;

    public ShootingRequest() {
    }

    public ShootingRequest(int clientId, int locationId, String title, String description,
                            LocalDate shootingDate, int durationHours, BigDecimal budget) {
        this.clientId = clientId;
        this.locationId = locationId;
        this.title = title;
        this.description = description;
        this.shootingDate = shootingDate;
        this.durationHours = durationHours;
        this.budget = budget;
        this.status = RequestStatus.NEW;
    }

    public ShootingRequest(int id, int clientId, int locationId, String title, String description,
                            LocalDate shootingDate, int durationHours, BigDecimal budget,
                            RequestStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.locationId = locationId;
        this.title = title;
        this.description = description;
        this.shootingDate = shootingDate;
        this.durationHours = durationHours;
        this.budget = budget;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public LocalDate getShootingDate() {
        return shootingDate;
    }

    public void setShootingDate(LocalDate shootingDate) {
        this.shootingDate = shootingDate;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("#%d | %-20s | клиент:%d | локация:%d | %s | %2dч | %10.2f | %s",
                id, title, clientId, locationId, shootingDate, durationHours, budget, status);
    }
}
