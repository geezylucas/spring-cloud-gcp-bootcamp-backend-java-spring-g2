package com.codigofacilito.cloud;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PokemonOfTheDayTests {

    @Test
    void elMismoDiaDevuelveElMismoPokemon() {
        LocalDate day = LocalDate.of(2026, 11, 16);
        assertThat(PokemonOfTheDay.pickFor(day)).isEqualTo(PokemonOfTheDay.pickFor(day));
    }

    @Test
    void elIdSiempreEstaEnElRangoValido() {
        for (int i = 0; i < 2000; i++) {
            int id = PokemonOfTheDay.pickFor(LocalDate.of(2026, 1, 1).plusDays(i));
            assertThat(id).isBetween(1, PokemonOfTheDay.TOTAL_POKEMON);
        }
    }
}
