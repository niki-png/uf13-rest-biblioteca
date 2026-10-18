package it.marconi.biblioteca.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibroDTO(

    @NotBlank(message = "ISBN campo obbligatorio")
    @Size(max = 13, message = "L'ISBN non può superare i 13 caratteri")
    String isbn,

    @NotBlank(message = "Il titolo non può essere vuoto")
    @Size(max = 50, message = "Il titolo non può superare i 50 caratteri")
    String titolo,

    String genere,

    Integer anno,

    @NotNull(message = "ID autore obbligatorio")
    Integer autore
) {}
