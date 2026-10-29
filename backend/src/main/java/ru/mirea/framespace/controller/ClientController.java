package ru.mirea.framespace.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.framespace.dto.ClientDtos.ClientResponse;
import ru.mirea.framespace.dto.ClientDtos.ClientUpsert;
import ru.mirea.framespace.dto.PageResponse;
import ru.mirea.framespace.service.ClientService;
import ru.mirea.framespace.web.PageableFactory;

import java.util.UUID;

/** REST-контур клиентов — заказчиков съёмки. */
@RestController
@RequestMapping("/api/clients")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {
    private final ClientService clients;
    private final PageableFactory pageableFactory;

    public ClientController(ClientService clients, PageableFactory pageableFactory) {
        this.clients = clients;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
    public PageResponse<ClientResponse> list(@RequestParam(defaultValue = "") String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "25") int size,
                                             @RequestParam(defaultValue = "name,asc") String sort) {
        return PageResponse.from(clients.search(q, pageableFactory.clients(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
    public ClientResponse get(@PathVariable UUID id) {
        return clients.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientUpsert request) {
        return ResponseEntity.status(201).body(clients.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientUpsert request) {
        return clients.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clients.delete(id);
        return ResponseEntity.noContent().build();
    }
}
