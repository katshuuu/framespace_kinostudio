package ru.mirea.kinostudio.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.mirea.kinostudio.exception.BusinessException;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

final class ApiJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ApiJson() {
    }

    static byte[] write(Object body) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(body);
    }

    static <T> T read(HttpExchange exchange, Class<T> type) throws IOException, BusinessException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            throw new BusinessException("Тело запроса пустое.");
        }
        try {
            return MAPPER.readValue(bytes, type);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Некорректное JSON-тело.");
        }
    }
}
