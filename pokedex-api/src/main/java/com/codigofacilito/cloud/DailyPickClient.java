package com.codigofacilito.cloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/** Llama a la Cloud Run function "pokemon-of-the-day". Si no está configurada o falla, devuelve vacío. */
@Component
public class DailyPickClient {

    record DailyPick(int id, String date) { }

    private final RestClient http;
    private final String functionUrl;

    public DailyPickClient(RestClient.Builder builder, @Value("${daily.function-url:}") String functionUrl) {
        this.http = builder.build();
        this.functionUrl = functionUrl;
    }

    public Optional<Integer> todayId() {
        if (functionUrl == null || functionUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            DailyPick pick = http.get().uri(functionUrl).retrieve().body(DailyPick.class);
            return Optional.ofNullable(pick).map(DailyPick::id);
        } catch (RuntimeException e) {
            return Optional.empty();      // la API sigue viva aunque la función falle
        }
    }
}
