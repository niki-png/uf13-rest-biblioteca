package it.marconi.biblioteca.responses;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Stati previsti dallo standard JSend
 *
 * <ul>
 *   <li>{@code success}: operazione andata a buon fine (2xx)</li>
 *   <li>{@code fail}: richiesta rifiutata per colpa del client, es. dati non validi o risorsa inesistente (4xx)</li>
 *   <li>{@code error}: errore imprevisto lato server (5xx)</li>
 * </ul>
 */
public enum APIStatus {

    SUCCESS("success"),
    FAIL("fail"),
    ERROR("error");

    private final String value;

    APIStatus(String value) {
        this.value = value;
    }

    // nel JSON lo stato viene serializzato in minuscolo ("success", "fail", "error")
    @JsonValue
    public String getValue() {
        return value;
    }
}
