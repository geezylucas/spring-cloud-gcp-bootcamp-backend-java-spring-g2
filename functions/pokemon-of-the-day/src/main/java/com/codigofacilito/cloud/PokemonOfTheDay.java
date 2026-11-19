package com.codigofacilito.cloud;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * FaaS: una función pequeña, sin estado, que responde a una petición HTTP.
 * Devuelve el "Pokémon del día": el mismo id para todos durante el día (UTC).
 */
public class PokemonOfTheDay implements HttpFunction {

    static final int TOTAL_POKEMON = 1025;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        response.setContentType("application/json");
        response.getWriter().write("{\"id\":%d,\"date\":\"%s\"}".formatted(pickFor(today), today));
    }

    /** Lógica pura y determinista: fácil de probar sin levantar nada. */
    static int pickFor(LocalDate day) {
        return (int) (Math.floorMod(day.toEpochDay() * 2654435761L, (long) TOTAL_POKEMON) + 1);
    }
}
