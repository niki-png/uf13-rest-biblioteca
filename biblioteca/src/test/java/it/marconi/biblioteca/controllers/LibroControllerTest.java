package it.marconi.biblioteca.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import it.marconi.biblioteca.domain.LibroDTO;
import it.marconi.biblioteca.services.LibroService;

/**
 * Test di integrazione del solo Web Layer per LibroController.
 */
@WebMvcTest(LibroController.class)
class LibroControllerTest {

    private static final String ISBN = "9788804668237";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    private static LibroDTO libro() {
        return new LibroDTO(ISBN, "Il barone rampante", "Romanzo", 1957, 1);
    }

    // ---------- 200 OK ----------

    @Test
    @DisplayName("GET /libri -> 200 e lista in data")
    void getAll_200() throws Exception {

        when(libroService.findAll()).thenReturn(List.of(libro()));

        mockMvc.perform(get("/libri"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].isbn").value(ISBN));
    }

    @Test
    @DisplayName("GET /libri/{isbn} esistente -> 200 e status success")
    void getByIsbn_esistente_200() throws Exception {

        when(libroService.getByIsbn(ISBN)).thenReturn(libro());

        mockMvc.perform(get("/libri/" + ISBN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.titolo").value("Il barone rampante"))
            .andExpect(jsonPath("$.data.anno").value(1957));
    }

    @Test
    @DisplayName("GET /libri/libro?titolo= esistente -> 200 e status success")
    void getByTitolo_esistente_200() throws Exception {

        when(libroService.getByTitolo("Il barone rampante")).thenReturn(libro());

        mockMvc.perform(get("/libri/libro").param("titolo", "Il barone rampante"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.isbn").value(ISBN));
    }

    @Test
    @DisplayName("POST /libri/add valido -> 200 e libro salvato in data")
    void addLibro_valido_200() throws Exception {

        when(libroService.save(any(LibroDTO.class))).thenReturn(libro());

        mockMvc.perform(post("/libri/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "isbn": "9788804668237", "titolo": "Il barone rampante",
                      "genere": "Romanzo", "anno": 1957, "autore": 1 }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.autore").value(1));
    }

    @Test
    @DisplayName("DELETE /libri/{isbn} esistente -> 200 e data null (Void)")
    void delete_esistente_200() throws Exception {

        mockMvc.perform(delete("/libri/" + ISBN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data").value(nullValue()))
            .andExpect(jsonPath("$.message").value("Libro eliminato correttamente"));

        verify(libroService).deleteByIsbn(ISBN);
    }

    // ---------- 404 Not Found ----------

    @Test
    @DisplayName("GET /libri/{isbn} inesistente -> 404 e status fail")
    void getByIsbn_inesistente_404() throws Exception {

        when(libroService.getByIsbn("000"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro con ISBN 000 non trovato"));

        mockMvc.perform(get("/libri/000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.message").value("Libro con ISBN 000 non trovato"));
    }

    @Test
    @DisplayName("POST /libri/add con autore inesistente -> 404 e status fail")
    void addLibro_autoreInesistente_404() throws Exception {

        when(libroService.save(any(LibroDTO.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Autore con id 99 non trovato"));

        mockMvc.perform(post("/libri/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "isbn": "123", "titolo": "Titolo", "autore": 99 }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.message").value("Autore con id 99 non trovato"));
    }

    @Test
    @DisplayName("DELETE /libri/{isbn} inesistente -> 404 e status fail")
    void delete_inesistente_404() throws Exception {

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Libro con ISBN 000 non trovato"))
            .when(libroService).deleteByIsbn("000");

        mockMvc.perform(delete("/libri/000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value("fail"));
    }

    // ---------- 400 Bad Request ----------

    @Test
    @DisplayName("POST /libri/add non valido -> 400 con un messaggio per ogni campo")
    void addLibro_nonValido_400() throws Exception {

        mockMvc.perform(post("/libri/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "isbn": "", "titolo": "", "autore": null }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.isbn").value("ISBN campo obbligatorio"))
            .andExpect(jsonPath("$.data.titolo").value("Il titolo non può essere vuoto"))
            .andExpect(jsonPath("$.data.autore").value("ID autore obbligatorio"));

        verify(libroService, never()).save(any());
    }

    @Test
    @DisplayName("GET /libri/libro senza parametro titolo -> 400 e status fail")
    void getByTitolo_senzaParametro_400() throws Exception {

        mockMvc.perform(get("/libri/libro"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.data.titolo").value("Parametro obbligatorio"));
    }
}
