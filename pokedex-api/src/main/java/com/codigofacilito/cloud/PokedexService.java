package com.codigofacilito.cloud;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class PokedexService {

    private final PokeApiClient pokeApi;
    private final DailyPickClient dailyPick;

    public PokedexService(PokeApiClient pokeApi, DailyPickClient dailyPick) {
        this.pokeApi = pokeApi;
        this.dailyPick = dailyPick;
    }

    public PokemonSummary byName(String name) {
        return pokeApi.find(name);
    }

    public DailyPokemon daily() {
        return dailyPick.todayId()
                .map(id -> new DailyPokemon(pokeApi.find(String.valueOf(id)), "function"))
                .orElseGet(() -> new DailyPokemon(pokeApi.find(String.valueOf(localPick())), "local"));
    }

    /** Plan B: misma idea que la función, calculada dentro de la API. */
    static int localPick() {
        long day = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        return (int) (Math.floorMod(day * 2654435761L, 1025L) + 1);
    }
}
