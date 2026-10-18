package it.marconi.biblioteca.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Contratto unico per tutte le risposte dell'API
 *
 * <pre>
 * success -> { "status": "success", "data": { ... } }
 * fail    -> { "status": "fail",    "data": { "campo": "messaggio" } }
 * error   -> { "status": "error",   "data": null, "message": "..." }
 * </pre>
 *
 * Il campo {@code data} è sempre presente (anche quando vale {@code null}, ad es. dopo
 * una cancellazione), mentre {@code message} compare solo quando valorizzato
 *
 * @param <T> tipo esplicito del payload (un DTO, una lista di DTO, una mappa di errori o {@link Void})
 */
public record APIResponse<T>(

    APIStatus status,

    T data,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String message
) {

    // Risposta di successo con payload
    public static <T> APIResponse<T> success(T data) {
        return new APIResponse<>(APIStatus.SUCCESS, data, null);
    }

    // Risposta di successo con payload e messaggio informativo
    public static <T> APIResponse<T> success(T data, String message) {
        return new APIResponse<>(APIStatus.SUCCESS, data, message);
    }

    // Richiesta rifiutata per colpa del client (4xx): {@code data} spiega il motivo
    public static <T> APIResponse<T> fail(T data) {
        return new APIResponse<>(APIStatus.FAIL, data, null);
    }

    // Errore lato server (5xx): solo un messaggio generico, mai dettagli interni.
    public static APIResponse<Void> error(String message) {
        return new APIResponse<>(APIStatus.ERROR, null, message);
    }
}
