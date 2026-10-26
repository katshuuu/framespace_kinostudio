package ru.mirea.kinostudio.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Заявка вместе со связанными клиентом и локацией — то, что нужно показать в таблице.
 * В базе заявка хранит только client_id и location_id, а пользователю нужны имя клиента
 * и название локации, поэтому интерфейс работает с этим «составным» объектом.
 */
public final class RequestView {

    private final ShootingRequest request;
    private final Client client;      // может быть null, если запись не найдена
    private final Location location;  // может быть null, если запись не найдена

    public RequestView(ShootingRequest request, Client client, Location location) {
        this.request = request;
        this.client = client;
        this.location = location;
    }

    public ShootingRequest getRequest() {
        return request;
    }

    public Client getClient() {
        return client;
    }

    public Location getLocation() {
        return location;
    }

    public int getId() {
        return request.getId();
    }

    public String getTitle() {
        return request.getTitle();
    }

    public String getDescription() {
        return request.getDescription();
    }

    public LocalDate getShootingDate() {
        return request.getShootingDate();
    }

    public int getDurationHours() {
        return request.getDurationHours();
    }

    public BigDecimal getBudget() {
        return request.getBudget();
    }

    public RequestStatus getStatus() {
        return request.getStatus();
    }

    public String getClientName() {
        return client == null ? "—" : client.getFullName();
    }

    public String getLocationName() {
        return location == null ? "—" : location.getName();
    }
}
