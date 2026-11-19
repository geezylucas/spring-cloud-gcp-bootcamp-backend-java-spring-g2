package com.codigofacilito.cloud;

import java.util.List;

/** Lo que nuestra API expone: un resumen sencillo, no la respuesta cruda de PokéAPI. */
public record PokemonSummary(int id, String name, double heightMeters, double weightKg,
                             List<String> types, String sprite) { }
