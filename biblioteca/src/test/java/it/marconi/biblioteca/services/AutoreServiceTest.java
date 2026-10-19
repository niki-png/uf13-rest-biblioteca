package it.marconi.biblioteca.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.AutoreMapper;
import it.marconi.biblioteca.repositories.AutoreRepository;

/**
 * Unit test puri del Service: nessun contesto Spring, Repository e Mapper sono mock.
 */
@ExtendWith(MockitoExtension.class)
class AutoreServiceTest {

    @Mock
    private AutoreRepository autoreRepo;

    @Mock
    private AutoreMapper mapper;

    @InjectMocks
    private AutoreService autoreService;

    @Test
    @DisplayName("save: l'ID inviato dal client viene azzerato prima del salvataggio")
    void save_azzeraIdPrimaDelSalvataggio() {

        // il client prova a forzare l'ID 42
        AutoreDTO input = new AutoreDTO(42, "Italo", "Calvino");
        Autore entityDalMapper = new Autore(42, "Italo", "Calvino", new ArrayList<>());
        Autore entitySalvata = new Autore(1, "Italo", "Calvino", new ArrayList<>());
        AutoreDTO output = new AutoreDTO(1, "Italo", "Calvino");

        when(mapper.toEntity(input)).thenReturn(entityDalMapper);
        when(autoreRepo.save(any(Autore.class))).thenReturn(entitySalvata);
        when(mapper.toDto(entitySalvata)).thenReturn(output);

        AutoreDTO risultato = autoreService.save(input);

        // regola di business: al repository arriva un'entità con id == null
        verify(autoreRepo).save(argThat((Autore a) ->
            a.getId() == null
            && "Italo".equals(a.getNome())
            && "Calvino".equals(a.getCognome())));
        verify(mapper).toDto(entitySalvata);

        assertThat(risultato.id()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAll: ogni entità viene convertita in DTO tramite il mapper")
    void findAll_mappaTutteLeEntita() {

        Autore a1 = new Autore(1, "Italo", "Calvino", new ArrayList<>());
        Autore a2 = new Autore(2, "Elsa", "Morante", new ArrayList<>());
        when(autoreRepo.findAll()).thenReturn(List.of(a1, a2));
        when(mapper.toDto(a1)).thenReturn(new AutoreDTO(1, "Italo", "Calvino"));
        when(mapper.toDto(a2)).thenReturn(new AutoreDTO(2, "Elsa", "Morante"));

        List<AutoreDTO> risultato = autoreService.findAll();

        assertThat(risultato).extracting(AutoreDTO::cognome).containsExactly("Calvino", "Morante");
        verify(mapper).toDto(argThat((Autore a) -> a.getId() == 1));
        verify(mapper).toDto(argThat((Autore a) -> a.getId() == 2));
    }

    @Test
    @DisplayName("getById: autore esistente restituito come DTO")
    void getById_esistente() {

        Autore entity = new Autore(1, "Italo", "Calvino", new ArrayList<>());
        when(autoreRepo.findById(1)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(new AutoreDTO(1, "Italo", "Calvino"));

        AutoreDTO risultato = autoreService.getById(1);

        assertThat(risultato.nome()).isEqualTo("Italo");
    }

    @Test
    @DisplayName("getById: autore inesistente -> ResponseStatusException 404")
    void getById_inesistente_404() {

        when(autoreRepo.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> autoreService.getById(99));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(mapper, never()).toDto(any());
    }

    @Test
    @DisplayName("deleteById: autore esistente viene cancellato")
    void deleteById_esistente() {

        when(autoreRepo.existsById(1)).thenReturn(true);

        autoreService.deleteById(1);

        verify(autoreRepo).deleteById(argThat((Integer id) -> id == 1));
    }

    @Test
    @DisplayName("deleteById: autore inesistente -> 404 e nessuna cancellazione")
    void deleteById_inesistente_404() {

        when(autoreRepo.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> autoreService.deleteById(99))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("99");

        verify(autoreRepo, never()).deleteById(anyInt());
    }
}
