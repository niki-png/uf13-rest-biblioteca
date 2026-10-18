package it.marconi.biblioteca.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.responses.APIResponse;
import it.marconi.biblioteca.services.AutoreService;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

/**
 * Tutti gli endpoint restituiscono un {@link APIResponse}.
 * I casi di errore (404, 400, ...) sono gestiti dal GlobalExceptionHandler:
 * qui non c'è nessun try-catch.
 */
@RestController
@RequestMapping("/autori")
public class AutoreController {

    private final LibroService libroService;
    private final AutoreService autoreService;

    public AutoreController(LibroService libroService, AutoreService autoreService) {
        this.libroService = libroService;
        this.autoreService = autoreService;
    }

    @GetMapping
    @Operation(summary = "Recupera tutti gli autori")
    public ResponseEntity<APIResponse<List<AutoreDTO>>> getAll() {

        return ResponseEntity.ok(APIResponse.success(autoreService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Cerca un autore dato il suo ID")
    public ResponseEntity<APIResponse<AutoreDTO>> getAutore(@PathVariable Integer id) {

        return ResponseEntity.ok(APIResponse.success(autoreService.getById(id)));
    }

    @GetMapping("/{id}/libri")
    @Operation(summary = "Recupera tutti i libri di un dato autore")
    public ResponseEntity<APIResponse<List<LibroDTO>>> getLibriByAutore(@PathVariable Integer id) {

        return ResponseEntity.ok(APIResponse.success(libroService.getByAutoreId(id)));
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo autore")
    public ResponseEntity<APIResponse<AutoreDTO>> addAutore(@Valid @RequestBody AutoreDTO autore) {

        return ResponseEntity.ok(APIResponse.success(autoreService.save(autore)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rimuove un autore dal database, e anche tutti i suoi libri")
    public ResponseEntity<APIResponse<Void>> deleteAutore(@PathVariable Integer id) {

        autoreService.deleteById(id);
        return ResponseEntity.ok(APIResponse.success(null, "Autore eliminato correttamente"));
    }
}
