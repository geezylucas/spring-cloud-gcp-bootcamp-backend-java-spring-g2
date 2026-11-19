package com.codigofacilito.cloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Cliente de la API pública PokéAPI. Recibe el Builder para poder simularlo en los tests. */
@Component
public class PokeApiClient {

    private final RestClient http;

    public PokeApiClient(RestClient.Builder builder,
                         @Value("${pokeapi.base-url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public PokemonSummary find(String nameOrId) {
        try {
            PokeApiPokemon p = http.get()
                    .uri("/pokemon/{name}", nameOrId.toLowerCase())
                    .retrieve()
                    .body(PokeApiPokemon.class);
            return toSummary(p);
        } catch (HttpClientErrorException.NotFound e) {
            throw new PokemonNotFoundException(nameOrId);
        }
    }

    private static PokemonSummary toSummary(PokeApiPokemon p) {
        List<String> types = p.types().stream().map(t -> t.type().name()).toList();
        // PokéAPI entrega altura en decímetros y peso en hectogramos
        return new PokemonSummary(p.id(), p.name(), p.height() / 10.0, p.weight() / 10.0,
                types, p.sprites() == null ? null : p.sprites().frontDefault());
    }
}
