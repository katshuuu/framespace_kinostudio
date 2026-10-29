package ru.mirea.framespace.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Фиксирует граф переходов статусов заявки (бизнес-правило №6 из КР №1/№2). */
class RequestStatusTest {

    @Test
    void newRequestIsApprovedOrRejected() {
        assertThat(RequestStatus.NEW.nextStatuses())
                .containsExactlyInAnyOrder(RequestStatus.APPROVED, RequestStatus.REJECTED);
    }

    @Test
    void approvedGoesToWorkOrIsCancelled() {
        assertThat(RequestStatus.APPROVED.nextStatuses())
                .containsExactlyInAnyOrder(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED);
        assertThat(RequestStatus.IN_PROGRESS.nextStatuses())
                .containsExactlyInAnyOrder(RequestStatus.COMPLETED, RequestStatus.CANCELLED);
    }

    @Test
    void skippingStepsIsForbidden() {
        assertThat(RequestStatus.NEW.canMoveTo(RequestStatus.COMPLETED)).isFalse();
        assertThat(RequestStatus.NEW.canMoveTo(RequestStatus.IN_PROGRESS)).isFalse();
        assertThat(RequestStatus.APPROVED.canMoveTo(RequestStatus.NEW)).isFalse();
    }

    @Test
    void finalStatusesCannotChange() {
        for (RequestStatus status : new RequestStatus[]{
                RequestStatus.COMPLETED, RequestStatus.REJECTED, RequestStatus.CANCELLED}) {
            assertThat(status.isFinal()).isTrue();
            assertThat(status.canMoveTo(RequestStatus.NEW)).isFalse();
        }
    }

    @Test
    void keepingSameStatusIsAllowed() {
        assertThat(RequestStatus.COMPLETED.canMoveTo(RequestStatus.COMPLETED)).isTrue();
    }
}
