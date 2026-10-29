package ru.mirea.framespace.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.framespace.domain.PriorityLevel;
import ru.mirea.framespace.domain.RequestStatus;
import ru.mirea.framespace.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет русские и английные команды умного фильтра. */
class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesMixedSmartQuery() {
        RequestFilter result = parser.parse("реклама локация:ПАВ-1 бюджет>200000 срочно:да приоритет:высокий тег:реклама");
        assertThat(result.freeText()).isEqualTo("реклама");
        assertThat(result.location()).isEqualTo("ПАВ-1");
        assertThat(result.status()).isNull();
        assertThat(result.urgent()).isTrue();
        assertThat(result.priority()).isEqualTo(PriorityLevel.HIGH);
        assertThat(result.tag()).isEqualTo("реклама");
        assertThat(result.archived()).isFalse();
        assertThat(result.budgets()).hasSize(1);
    }

    @Test
    void supportsQuotedFreeTextAndStatus() {
        RequestFilter result = parser.parse("\"Съёмка клипа\" status:in_progress archived:all");
        assertThat(result.freeText()).isEqualTo("Съёмка клипа");
        assertThat(result.status()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsOutOfRangeBudget() {
        assertThatThrownBy(() -> parser.parse("бюджет>100000000"))
                .isInstanceOf(BadRequestException.class);
    }
}
