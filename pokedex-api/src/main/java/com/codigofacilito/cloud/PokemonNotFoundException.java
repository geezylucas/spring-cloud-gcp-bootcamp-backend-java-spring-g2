package com.codigofacilito.cloud;

public class PokemonNotFoundException extends RuntimeException {
    public PokemonNotFoundException(String name) {
        super("No encontré al Pokémon '" + name + "'");
    }
}
