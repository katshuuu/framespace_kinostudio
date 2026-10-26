package ru.mirea.kinostudio.api;

import java.math.BigDecimal;

record RegisterBody(String username, String password, String fullName) {
}

record LoginBody(String username, String password) {
}

record ClientBody(String fullName, String email, String phone) {
}

record LocationBody(String name, String address, Integer capacity, String type) {
}

record ShootingRequestBody(
        Integer clientId,
        Integer locationId,
        String title,
        String description,
        String shootingDate,
        Integer durationHours,
        BigDecimal budget) {
}

record StatusBody(String status) {
}
