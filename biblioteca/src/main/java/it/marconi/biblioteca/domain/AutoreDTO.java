package it.marconi.biblioteca.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO (Data Transfer Object)
public record AutoreDTO(

    // l'ID è ignorato in creazione: viene azzerato dal service e generato dal DB
    Integer id,

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 30, message = "Il nome non può superare i 30 caratteri")
    String nome,

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 30, message = "Il cognome non può superare i 30 caratteri")
    String cognome
) {}
