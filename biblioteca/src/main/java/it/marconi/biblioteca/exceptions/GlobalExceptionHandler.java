package it.marconi.biblioteca.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import it.marconi.biblioteca.responses.APIResponse;
import it.marconi.biblioteca.responses.APIStatus;

/**
 * Gestore centralizzato delle eccezioni
 *
 * Intercetta qualsiasi eccezione sollevata da controller e service e la trasforma
 * in una {@link APIResponse} conforme a JSend, così che:
 * <ul>
 *   <li>i controller non contengano blocchi try-catch per formattare gli errori</li>
 *   <li>il client riceva sempre la stessa struttura JSON</li>
 *   <li>nessuno stack trace o dettaglio interno arrivi all'esterno</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String MESSAGE_KEY = "message";
    static final String INTERNAL_ERROR_MESSAGE = "Si è verificato un errore interno. Riprova più tardi.";

    /**
     * Validazione fallita (@Valid sul @RequestBody): 400 Bad Request con mappa campo -> messaggio
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // se lo stesso campo viola più vincoli, i messaggi vengono concatenati
            errors.merge(fieldError.getField(), messageOf(fieldError), (a, b) -> a + "; " + b);
        }

        return ResponseEntity.badRequest().body(APIResponse.fail(errors));
    }

    /**
     * Eccezioni con stato HTTP esplicito (es. risorsa non trovata -> 404)
     * Gli stati 4xx diventano "fail", gli stati 5xx diventano "error"
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleResponseStatus(ResponseStatusException ex) {

        HttpStatusCode statusCode = ex.getStatusCode();

        if (statusCode.is5xxServerError()) {
            log.error("Errore lato server", ex);
            return ResponseEntity.status(statusCode)
                .body(new APIResponse<>(APIStatus.ERROR, null, INTERNAL_ERROR_MESSAGE));
        }

        String reason = ex.getReason() != null ? ex.getReason() : defaultReason(statusCode);
        return ResponseEntity.status(statusCode).body(APIResponse.fail(Map.of(MESSAGE_KEY, reason)));
    }

    /**
     * Corpo della richiesta assente o JSON malformato: 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleNotReadable(HttpMessageNotReadableException ex) {

        return ResponseEntity.badRequest()
            .body(APIResponse.fail(Map.of("body", "Il corpo della richiesta è assente o non è un JSON valido")));
    }

    /**
     * Parametro di path/query del tipo sbagliato (es. /autori/abc): 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        return ResponseEntity.badRequest()
            .body(APIResponse.fail(Map.of(ex.getName(), "Valore '" + ex.getValue() + "' non valido")));
    }

    /**
     * Parametro di query obbligatorio mancante (es. /libri/libro senza ?titolo=): 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleMissingParam(MissingServletRequestParameterException ex) {

        return ResponseEntity.badRequest()
            .body(APIResponse.fail(Map.of(ex.getParameterName(), "Parametro obbligatorio")));
    }

    /**
     * Endpoint inesistente: 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleNoResource(NoResourceFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(APIResponse.fail(Map.of(MESSAGE_KEY, "Endpoint non trovato")));
    }

    /**
     * Metodo HTTP non supportato dall'endpoint: 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(APIResponse.fail(Map.of(MESSAGE_KEY, "Metodo " + ex.getMethod() + " non supportato")));
    }

    /**
     * Content-Type non supportato (es. body non JSON): 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleMediaType(HttpMediaTypeNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(APIResponse.fail(Map.of(MESSAGE_KEY, "Content-Type non supportato, usare application/json")));
    }

    /**
     * Rete di sicurezza per qualsiasi altra eccezione: 500 con messaggio generico
     * Il dettaglio finisce solo nei log del server, mai nella risposta
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleGeneric(Exception ex) {

        log.error("Eccezione non gestita", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(APIResponse.error(INTERNAL_ERROR_MESSAGE));
    }

    private static String messageOf(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Valore non valido";
    }

    private static String defaultReason(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.getReasonPhrase() : "Richiesta non valida";
    }
}
