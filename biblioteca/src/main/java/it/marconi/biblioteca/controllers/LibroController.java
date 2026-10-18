package it.marconi.biblioteca.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.responses.APIResponse;
import it.marconi.biblioteca.services.LibroService;
import jakarta.validation.Valid;

/**
 * Tutti gli endpoint restituiscono un {@link APIResponse}.
 * I casi di errore (404, 400, ...) sono gestiti dal GlobalExceptionHandler:
 * qui non c'è nessun try-catch.
 */
@RestController
@RequestMapping("/libri")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    @Operation(summary = "Recupera la lista di tutti i libri")
    public ResponseEntity<APIResponse<List<LibroDTO>>> getAll() {

        return ResponseEntity.ok(APIResponse.success(libroService.findAll()));
    }

    @GetMapping("/{isbn}")
    @Operation(summary = "Cerca un libro dal suo ISBN")
    public ResponseEntity<APIResponse<LibroDTO>> getLibroByIsbn(@PathVariable String isbn) {

        return ResponseEntity.ok(APIResponse.success(libroService.getByIsbn(isbn)));
    }

    @GetMapping("/libro")
    @Operation(summary = "Cerca un libro per titolo esatto")
    public ResponseEntity<APIResponse<LibroDTO>> getLibroByTitolo(@RequestParam("titolo") String titolo) {

        return ResponseEntity.ok(APIResponse.success(libroService.getByTitolo(titolo)));
    }

    @PostMapping("/add")
    @Operation(summary = "Aggiunge un nuovo libro, dato l'autore")
    public ResponseEntity<APIResponse<LibroDTO>> addLibro(@Valid @RequestBody LibroDTO libro) {

        return ResponseEntity.ok(APIResponse.success(libroService.save(libro)));
    }

    @DeleteMapping("/{isbn}")
    @Operation(summary = "Elimina un libro dato il suo ISBN")
    public ResponseEntity<APIResponse<Void>> deleteLibro(@PathVariable String isbn) {

        libroService.deleteByIsbn(isbn);
        return ResponseEntity.ok(APIResponse.success(null, "Libro eliminato correttamente"));
    }
}
