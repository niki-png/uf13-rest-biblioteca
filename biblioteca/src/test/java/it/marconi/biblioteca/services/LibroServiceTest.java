package it.marconi.biblioteca.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import it.marconi.biblioteca.domain.Autore;
import it.marconi.biblioteca.domain.Libro;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.domain.LibroMapper;
import it.marconi.biblioteca.repositories.AutoreRepository;
import it.marconi.biblioteca.repositories.LibroRepository;

/**
 * Unit test puri del Service: nessun contesto Spring, Repository e Mapper sono mock.
 */
@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    private static final String ISBN = "9788804668237";

    @Mock
    private LibroRepository libroRepo;

    @Mock
    private AutoreRepository autoreRepo;

    @Mock
    private LibroMapper mapper;

    @InjectMocks
    private LibroService libroService;

    @Test
    @DisplayName("save: il libro viene associato all'autore recuperato dal DB prima del salvataggio")
    void save_associaAutorePrimaDelSalvataggio() {

        Autore autore = new Autore(1, "Italo", "Calvino", new ArrayList<>());
        LibroDTO input = new LibroDTO(ISBN, "Il barone rampante", "Romanzo", 1957, 1);
        Libro entity = new Libro(ISBN, "Il barone rampante", "Romanzo", autore, 1957);

        when(autoreRepo.findById(1)).thenReturn(Optional.of(autore));
        when(mapper.toEntity(input, autore)).thenReturn(entity);
        when(libroRepo.save(any(Libro.class))).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(input);

        LibroDTO risultato = libroService.save(input);

        // regola di business: l'entità salvata è legata all'autore esistente con id 1
        verify(libroRepo).save(argThat((Libro l) ->
            ISBN.equals(l.getIsbn())
            && l.getAutore() != null
            && l.getAutore().getId() == 1));
        verify(mapper).toEntity(argThat((LibroDTO d) -> d.autore() == 1), argThat((Autore a) -> a.getId() == 1));

        assertThat(risultato.isbn()).isEqualTo(ISBN);
    }

    @Test
    @DisplayName("save: autore inesistente -> 404 e nessun salvataggio")
    void save_autoreInesistente_404() {

        LibroDTO input = new LibroDTO(ISBN, "Il barone rampante", "Romanzo", 1957, 99);
        when(autoreRepo.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> libroService.save(input));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(libroRepo, never()).save(any());
        verify(mapper, never()).toEntity(any(), any());
    }

    @Test
    @DisplayName("getByIsbn: libro inesistente -> 404")
    void getByIsbn_inesistente_404() {

        when(libroRepo.findById("000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libroService.getByIsbn("000"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("000");
    }

    @Test
    @DisplayName("getByTitolo: restituisce solo il libro con titolo esatto")
    void getByTitolo_filtraPerTitoloEsatto() {

        Autore autore = new Autore(1, "Italo", "Calvino", new ArrayList<>());
        Libro barone = new Libro(ISBN, "Il barone rampante", "Romanzo", autore, 1957);
        Libro visconte = new Libro("111", "Il visconte dimezzato", "Romanzo", autore, 1952);
        when(libroRepo.findAll()).thenReturn(List.of(visconte, barone));
        when(mapper.toDto(barone)).thenReturn(new LibroDTO(ISBN, "Il barone rampante", "Romanzo", 1957, 1));

        LibroDTO risultato = libroService.getByTitolo("Il barone rampante");

        assertThat(risultato.isbn()).isEqualTo(ISBN);
        verify(mapper, never()).toDto(visconte);
    }

    @Test
    @DisplayName("getByAutoreId: autore inesistente -> 404 senza interrogare i libri")
    void getByAutoreId_autoreInesistente_404() {

        when(autoreRepo.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> libroService.getByAutoreId(99))
            .isInstanceOf(ResponseStatusException.class);

        verify(libroRepo, never()).findByAutoreId(anyInt());
    }

    @Test
    @DisplayName("deleteByIsbn: libro esistente viene cancellato")
    void deleteByIsbn_esistente() {

        when(libroRepo.existsById(ISBN)).thenReturn(true);

        libroService.deleteByIsbn(ISBN);

        verify(libroRepo).deleteById(argThat((String isbn) -> ISBN.equals(isbn)));
    }

    @Test
    @DisplayName("deleteByIsbn: libro inesistente -> 404 e nessuna cancellazione")
    void deleteByIsbn_inesistente_404() {

        when(libroRepo.existsById("000")).thenReturn(false);

        assertThatThrownBy(() -> libroService.deleteByIsbn("000"))
            .isInstanceOf(ResponseStatusException.class);

        verify(libroRepo, never()).deleteById(anyString());
    }
}
