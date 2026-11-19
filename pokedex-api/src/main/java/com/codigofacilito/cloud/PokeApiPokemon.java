package com.codigofacilito.cloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Subconjunto de la respuesta de https://pokeapi.co/api/v2/pokemon/{nombre}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record PokeApiPokemon(int id, String name, int height, int weight,
                      List<TypeSlot> types, Sprites sprites) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TypeSlot(int slot, TypeRef type) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TypeRef(String name) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Sprites(@JsonProperty("front_default") String frontDefault) { }
}
