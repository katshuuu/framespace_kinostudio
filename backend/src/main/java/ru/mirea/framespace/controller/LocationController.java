package ru.mirea.framespace.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.framespace.dto.LocationDtos.LocationUpsert;
import ru.mirea.framespace.dto.LocationDtos.LocationResponse;
import ru.mirea.framespace.service.LocationService;

import java.util.List;
import java.util.UUID;

/** REST-контур локаций съёмки. */
@RestController
@RequestMapping("/api/locations")
@SecurityRequirement(name = "bearerAuth")
public class LocationController {
    private final LocationService locations;

    public LocationController(LocationService locations) { this.locations = locations; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
    public List<LocationResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return locations.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationUpsert request) {
        return ResponseEntity.status(201).body(locations.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public LocationResponse update(@PathVariable UUID id, @Valid @RequestBody LocationUpsert request) {
        return locations.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public LocationResponse setActive(@PathVariable UUID id, @RequestParam boolean active, @RequestParam long version) {
        return locations.setActive(id, active, version);
    }
}
