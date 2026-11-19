package com.codigofacilito.cloud;

/** source = "function" (vino de la Cloud Run function) o "local" (plan B de la API). */
public record DailyPokemon(PokemonSummary pokemon, String source) { }
