package com.codigofacilito.cloud;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pokemon")
public class PokedexController {

    private final PokedexService service;

    public PokedexController(PokedexService service) {
        this.service = service;
    }

    @GetMapping("/{name}")
    public PokemonSummary byName(@PathVariable String name) {
        return service.byName(name);
    }

    @GetMapping("/daily")
    public DailyPokemon daily() {
        return service.daily();
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    ProblemDetail notFound(PokemonNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
