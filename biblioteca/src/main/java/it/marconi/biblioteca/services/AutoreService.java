package it.marconi.biblioteca.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.AutoreMapper;
import it.marconi.biblioteca.repositories.AutoreRepository;

@Service
public class AutoreService {

    private final AutoreRepository autoreRepo;
    private final AutoreMapper mapper;

    // dependency injection tramite costruttore (facilita i test con Mockito)
    public AutoreService(AutoreRepository autoreRepo, AutoreMapper mapper) {
        this.autoreRepo = autoreRepo;
        this.mapper = mapper;
    }

    public AutoreDTO save(AutoreDTO autore) {

        Autore entity = mapper.toEntity(autore);
        entity.setId(null);     // regola di business: l'ID lo genera il DB, mai quello inviato dalle API
        entity = autoreRepo.save(entity);
        return mapper.toDto(entity);
    }

    public List<AutoreDTO> findAll() {

        return autoreRepo.findAll().stream().map(mapper::toDto).toList();
    }

    public AutoreDTO getById(int id) {

        return autoreRepo.findById(id)
            .map(mapper::toDto)
            .orElseThrow(() -> notFound(id));
    }

    public void deleteById(int id) {

        if (!autoreRepo.existsById(id))
            throw notFound(id);

        autoreRepo.deleteById(id);
    }

    static ResponseStatusException notFound(int id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore con id " + id + " non trovato");
    }
}
