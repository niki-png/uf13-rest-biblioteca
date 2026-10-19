package it.marconi.biblioteca.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import it.marconi.biblioteca.domain.AutoreDTO;
import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.services.AutoreService;
import it.marconi.biblioteca.services.LibroService;

/**
 * Test di integrazione del solo Web Layer: il contesto carica AutoreController e il
 * GlobalExceptionHandler (@ControllerAdvice), mentre i service sono sostituiti da mock.
 */
@WebMvcTest(AutoreController.class)
class AutoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutoreService autoreService;

    @MockitoBean
    private LibroService libroService;

    // ---------- 200 OK ----------

    @Test
    @DisplayName("GET /autori/{id} esistente -> 200 e status success con il DTO in data")
    void getAutore_esistente_200() throws Exception {

        when(autoreService.getById(1)).thenReturn(new AutoreDTO(1, "Italo", "Calvino"));

        mockMvc.perform(get("/autori/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.nome").value("Italo"))
            .andExpect(jsonPath("$.data.cognome").value("Calvino"))
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("GET /autori -> 200 e lista di autori in data")
    void getAll_200() throws Exception {

        when(autoreService.findAll()).thenReturn(List.of(
            new AutoreDTO(1, "Italo", "Calvino"),
            new AutoreDTO(2, "Elsa", "Morante")));

        mockMvc.perform(get("/autori"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[1].cognome").value("Morante"));
    }

    @Test
    @DisplayName("GET /autori/{id}/libri -> 200 e libri dell'autore in data")
    void getLibriByAutore_200() throws Exception {

        when(libroService.getByAutoreId(1)).thenReturn(List.of(
            new LibroDTO("9788804668237", "Il barone rampante", "Romanzo", 1957, 1)));

        mockMvc.perform(get("/autori/1/libri"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data[0].titolo").value("Il barone rampante"))
            .andExpect(jsonPath("$.data[0].autore").value(1));
    }

    @Test
    @DisplayName("POST /autori/add valido -> 200 e autore salvato in data")
    void addAutore_valido_200() throws Exception {

        when(autoreService.save(any(AutoreDTO.class))).thenReturn(new AutoreDTO(5, "Italo", "Calvino"));

        mockMvc.perform(post("/autori/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "nome": "Italo", "cognome": "Calvino" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    @DisplayName("DELETE /autori/{id} esistente -> 200, data null (Void) e messaggio")
    void deleteAutore_esistente_200() throws Exception {

        mockMvc.perform(delete("/autori/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").value(nullValue()))
            .andExpect(jsonPath("$.message").value("Autore eliminato correttamente"));

        verify(autoreService).deleteById(1);
    }

    // ---------- 404 Not Found ----------

    @Test
    @DisplayName("GET /autori/{id} inesistente -> 404 e status fail")
    void getAutore_inesistente_404() throws Exception {

        when(autoreService.getById(99))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore con id 99 non trovato"));

        mockMvc.perform(get("/autori/99"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.message").value("Autore con id 99 non trovato"));
    }

    @Test
    @DisplayName("DELETE /autori/{id} inesistente -> 404 e status fail")
    void deleteAutore_inesistente_404() throws Exception {

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore con id 99 non trovato"))
            .when(autoreService).deleteById(99);

        mockMvc.perform(delete("/autori/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.message").value("Autore con id 99 non trovato"));
    }

    @Test
    @DisplayName("GET /autori/{id}/libri con autore inesistente -> 404 e status fail")
    void getLibriByAutore_inesistente_404() throws Exception {

        when(libroService.getByAutoreId(99))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore con id 99 non trovato"));

        mockMvc.perform(get("/autori/99/libri"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("fail"));
    }

    // ---------- 400 Bad Request ----------

    @Test
    @DisplayName("POST /autori/add non valido -> 400, status fail e mappa campo -> messaggio")
    void addAutore_nonValido_400() throws Exception {

        mockMvc.perform(post("/autori/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "nome": "", "cognome": null }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.nome").value("Il nome è obbligatorio"))
            .andExpect(jsonPath("$.data.cognome").value("Il cognome è obbligatorio"));

        // la validazione avviene prima del service: il service non deve essere chiamato
        verify(autoreService, never()).save(any());
    }

    @Test
    @DisplayName("POST /autori/add con nome troppo lungo -> 400 con messaggio specifico")
    void addAutore_nomeTroppoLungo_400() throws Exception {

        String nomeLungo = "A".repeat(31);

        mockMvc.perform(post("/autori/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"" + nomeLungo + "\", \"cognome\": \"Rossi\" }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.nome").value("Il nome non può superare i 30 caratteri"))
            .andExpect(jsonPath("$.data.cognome").doesNotExist());
    }

    @Test
    @DisplayName("POST /autori/add con JSON malformato -> 400 e status fail")
    void addAutore_jsonMalformato_400() throws Exception {

        mockMvc.perform(post("/autori/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ nome: "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.body").exists());
    }

    @Test
    @DisplayName("GET /autori/abc (id non numerico) -> 400 e status fail")
    void getAutore_idNonNumerico_400() throws Exception {

        mockMvc.perform(get("/autori/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.id").exists());
    }

    // ---------- 500 Internal Server Error ----------

    @Test
    @DisplayName("Eccezione imprevista -> 500, status error e nessun dettaglio interno")
    void eccezioneImprevista_500_senzaStackTrace() throws Exception {

        when(autoreService.findAll()).thenThrow(new IllegalStateException("dettaglio interno segreto"));

        mockMvc.perform(get("/autori"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(content().string(not(containsString("dettaglio interno segreto"))))
            .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
