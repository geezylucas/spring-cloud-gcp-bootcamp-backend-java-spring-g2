package com.codigofacilito.cloud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokedexServiceTests {

    @Mock PokeApiClient pokeApi;
    @Mock DailyPickClient dailyPick;

    private PokemonSummary pikachu() {
        return new PokemonSummary(25, "pikachu", 0.4, 6.0, List.of("electric"), null);
    }

    @Test
    void usaLaFuncionCuandoResponde() {
        when(dailyPick.todayId()).thenReturn(Optional.of(25));
        when(pokeApi.find("25")).thenReturn(pikachu());

        DailyPokemon result = new PokedexService(pokeApi, dailyPick).daily();

        assertThat(result.source()).isEqualTo("function");
        assertThat(result.pokemon().name()).isEqualTo("pikachu");
    }

    @Test
    void usaElPlanBLocalCuandoLaFuncionNoResponde() {
        when(dailyPick.todayId()).thenReturn(Optional.empty());
        when(pokeApi.find(anyString())).thenReturn(pikachu());

        DailyPokemon result = new PokedexService(pokeApi, dailyPick).daily();

        assertThat(result.source()).isEqualTo("local");
    }
}
