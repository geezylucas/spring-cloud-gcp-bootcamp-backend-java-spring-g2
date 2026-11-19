package com.codigofacilito.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** Slice web (mismo patrón del módulo 9): solo el controlador, el servicio se simula. */
@WebMvcTest(PokedexController.class)
class PokedexControllerTests {

    @Autowired MockMvcTester mvc;
    @MockitoBean PokedexService service;

    @Test
    void devuelveElPokemonComoJson() {
        given(service.byName("pikachu")).willReturn(
                new PokemonSummary(25, "pikachu", 0.4, 6.0, List.of("electric"), null));

        var result = mvc.get().uri("/api/pokemon/{name}", "pikachu").exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.name").isEqualTo("pikachu");
    }

    @Test
    void responde404SiNoExiste() {
        given(service.byName("missingno")).willThrow(new PokemonNotFoundException("missingno"));

        var result = mvc.get().uri("/api/pokemon/{name}", "missingno").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }
}
