package it.marconi.biblioteca.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.Libro;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.domain.LibroMapper;
import it.marconi.biblioteca.repositories.AutoreRepository;
import it.marconi.biblioteca.repositories.LibroRepository;

@Service
public class LibroService {

    private final LibroRepository libroRepo;
    private final AutoreRepository autoreRepo;
    private final LibroMapper mapper;

    // dependency injection tramite costruttore (facilita i test con Mockito)
    public LibroService(LibroRepository libroRepo, AutoreRepository autoreRepo, LibroMapper mapper) {
        this.libroRepo = libroRepo;
        this.autoreRepo = autoreRepo;
        this.mapper = mapper;
    }

    public List<LibroDTO> findAll() {

        // converto la lista di libri in una lista di DTO
        return libroRepo.findAll().stream().map(mapper::toDto).toList();
    }

    public LibroDTO getByIsbn(String isbn) {

        return libroRepo.findById(isbn)
            .map(mapper::toDto)
            .orElseThrow(() -> libroNotFound("ISBN " + isbn));
    }

    public LibroDTO getByTitolo(String titolo) {

        return libroRepo.findAll().stream()
            .filter(libro -> libro.getTitolo().equals(titolo)).findFirst()
            .map(mapper::toDto)
            .orElseThrow(() -> libroNotFound("titolo '" + titolo + "'"));
    }

    // ricerca dei libri per autore_id (query custom); 404 se l'autore non esiste
    public List<LibroDTO> getByAutoreId(int autoreId) {

        if (!autoreRepo.existsById(autoreId))
            throw AutoreService.notFound(autoreId);

        return libroRepo.findByAutoreId(autoreId).stream().map(mapper::toDto).toList();
    }

    public LibroDTO save(LibroDTO libro) {

        // cerco l'autore nel db da associare al libro
        Autore autore = autoreRepo.findById(libro.autore())
            .orElseThrow(() -> AutoreService.notFound(libro.autore()));

        Libro entity = mapper.toEntity(libro, autore);
        entity = libroRepo.save(entity);
        return mapper.toDto(entity);
    }

    public void deleteByIsbn(String isbn) {

        if (!libroRepo.existsById(isbn))
            throw libroNotFound("ISBN " + isbn);

        libroRepo.deleteById(isbn);
    }

    private static ResponseStatusException libroNotFound(String criterio) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro con " + criterio + " non trovato");
    }
}
