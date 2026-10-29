package ru.mirea.framespace.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.framespace.domain.Client;
import ru.mirea.framespace.domain.PriorityLevel;
import ru.mirea.framespace.domain.RequestStatus;
import ru.mirea.framespace.domain.ShootingRequest;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Версия в ответе API должна совпадать с версией в базе, иначе следующее изменение
 * получит ложный 409. UUID назначается заранее, поэтому save обязан выполнять persist.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityVersionTest {
    @Autowired
    private ClientRepository clients;
    @Autowired
    private ShootingRequestRepository requests;
    @Autowired
    private EntityManager entityManager;

    @Test
    void saveOfNewEntityReturnsSameManagedInstance() {
        Client client = new Client(UUID.randomUUID(), "Тестовый клиент", "version.test@mail.ru", "+7-900-000-00-00");
        assertThat(client.isNew()).isTrue();

        Client saved = clients.saveAndFlush(client);

        assertThat(saved).isSameAs(client);
        assertThat(saved.isNew()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void versionAfterCreateWithTagsMatchesDatabase() {
        Client client = clients.saveAndFlush(
                new Client(UUID.randomUUID(), "Клиент с заявкой", "tags.test@mail.ru", "+7-900-000-00-01"));
        ShootingRequest request = new ShootingRequest(UUID.randomUUID(), "Заявка", client, null);
        request.setDescription("Описание");
        request.setShootingDate(LocalDate.now().plusDays(10));
        request.setStatus(RequestStatus.NEW);
        request.setPriorityLevel(PriorityLevel.LOW);
        request.setTags(Set.of("клип", "реклама"));

        ShootingRequest saved = requests.saveAndFlush(request);
        long versionInResponse = saved.getVersion();
        entityManager.clear();

        assertThat(saved).isSameAs(request);
        assertThat(requests.findById(request.getId()).orElseThrow().getVersion()).isEqualTo(versionInResponse);
    }
}
