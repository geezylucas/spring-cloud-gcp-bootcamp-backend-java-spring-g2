package com.codigofacilito.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Sin red: MockRestServiceServer reemplaza a PokéAPI. En CI nunca llamamos a internet:
 * un test que depende de un tercero es un test frágil (y lento).
 */
class PokeApiClientTests {

    private static final String BASE = "https://pokeapi.test/api/v2";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PokeApiClient client = new PokeApiClient(builder, BASE);

    @Test
    void convierteLaRespuestaDePokeApiEnUnResumen() {
        server.expect(requestTo(BASE + "/pokemon/pikachu"))
                .andRespond(withSuccess("""
                        {"id":25,"name":"pikachu","height":4,"weight":60,
                         "types":[{"slot":1,"type":{"name":"electric"}}],
                         "sprites":{"front_default":"https://img/25.png"},
                         "campo_que_no_usamos":true}
                        """, MediaType.APPLICATION_JSON));

        PokemonSummary p = client.find("Pikachu");

        assertThat(p.id()).isEqualTo(25);
        assertThat(p.heightMeters()).isEqualTo(0.4);
        assertThat(p.weightKg()).isEqualTo(6.0);
        assertThat(p.types()).containsExactly("electric");
        server.verify();
    }

    @Test
    void traduceElErrorHttp404AUnaExcepcionDeNegocio() {
        server.expect(requestTo(BASE + "/pokemon/missingno"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.find("missingno"))
                .isInstanceOf(PokemonNotFoundException.class)
                .hasMessageContaining("missingno");
    }
}
