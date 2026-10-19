# Relazione tecnica — Task 1: standardizzare le risposte e centralizzare gli errori

> Branch di sviluppo: `feature/task-1` · Branch di consegna: `deploy`

## 1. Obiettivo

All'inizio le API della biblioteca rispondevano in modi diversi tra loro:

| Endpoint (prima) | Risposta |
|---|---|
| `GET /autori`, `GET /libri` | array JSON "nudo" |
| `GET /autori/{id}` | oggetto DTO oppure 404 **con corpo vuoto** |
| `DELETE /autori/{id}`, `DELETE /libri/{isbn}` | **stringa di testo** (`"Autore eliminato correttamente!"`) |
| validazione fallita | JSON di errore di default di Spring (formato non controllato) |
| eccezione imprevista | errore di default di Spring (formato non controllato) |

L'obiettivo della Task 1 è avere **un unico contratto di risposta** (`APIResponse`) secondo lo standard **JSend**, e **gestire tutti gli errori in un solo punto**, senza mai mandare al client stack trace o dettagli interni.

## 2. Il contratto `APIResponse<T>`

Package `it.marconi.biblioteca.responses`.

```java
public record APIResponse<T>(APIStatus status, T data, String message) { ... }
```

- `status`: enum `APIStatus`, serializzato in minuscolo con `@JsonValue` → `"success"`, `"fail"`, `"error"`.
- `data`: il payload, **con tipo sempre esplicito** (un DTO, `List<DTO>`, `Map<String, String>` per gli errori, `Void` quando non c'è contenuto). C'è sempre nel JSON, anche quando vale `null`.
- `message`: compare solo se valorizzato (`@JsonInclude(NON_NULL)`).

Metodi factory statici: `success(data)`, `success(data, message)`, `fail(data)`, `error(message)`.

### Come si mappano gli stati JSend

| Stato JSend | Quando | HTTP | Contenuto |
|---|---|---|---|
| `success` | operazione andata a buon fine | 200 | `data` = risultato |
| `fail` | errore del client: dati non validi, risorsa inesistente, parametro sbagliato | 400 / 404 / 405 / 415 | `data` = mappa che spiega il motivo |
| `error` | errore imprevisto del server | 500 | `message` generico, nessun dettaglio interno |

### Esempi di risposta

**200: autore trovato**
```json
{ "status": "success", "data": { "id": 1, "nome": "Italo", "cognome": "Calvino" } }
```

**200: cancellazione (data di tipo `Void`)**
```json
{ "status": "success", "data": null, "message": "Autore eliminato correttamente" }
```

**400: validazione fallita (una voce per ogni campo non valido)**
```json
{ "status": "fail", "data": { "nome": "Il nome è obbligatorio", "cognome": "Il cognome è obbligatorio" } }
```

**404: risorsa non trovata**
```json
{ "status": "fail", "data": { "message": "Autore con id 99 non trovato" } }
```

**500: errore imprevisto**
```json
{ "status": "error", "data": null, "message": "Si è verificato un errore interno. Riprova più tardi." }
```

## 3. Gestione centralizzata degli errori: `GlobalExceptionHandler`

Package `it.marconi.biblioteca.exceptions`, classe annotata con **`@ControllerAdvice`**. Ogni metodo `@ExceptionHandler` restituisce un `ResponseEntity<APIResponse<...>>` con un tipo esplicito.

| Eccezione intercettata | Causa | HTTP | JSend |
|---|---|---|---|
| `MethodArgumentNotValidException` | `@Valid` sul body fallito | 400 | `fail`, mappa campo → messaggio |
| `ResponseStatusException` | lanciata dai service (es. risorsa non trovata) | stato dell'eccezione (es. 404) | `fail` se 4xx, `error` se 5xx |
| `HttpMessageNotReadableException` | JSON malformato o body assente | 400 | `fail` |
| `MethodArgumentTypeMismatchException` | es. `/autori/abc` | 400 | `fail` |
| `MissingServletRequestParameterException` | es. `/libri/libro` senza `?titolo=` | 400 | `fail` |
| `NoResourceFoundException` | endpoint inesistente | 404 | `fail` |
| `HttpRequestMethodNotSupportedException` | metodo HTTP sbagliato | 405 | `fail` |
| `HttpMediaTypeNotSupportedException` | Content-Type diverso da JSON | 415 | `fail` |
| `Exception` (catch-all) | qualsiasi altro errore | 500 | `error` |

Scelte progettuali:

- **Nessuna fuga di informazioni interne**: il catch-all registra l'eccezione completa nei log del server (`log.error`) ma al client manda solo un messaggio generico. Anche le `ResponseStatusException` con stato 5xx vengono trasformate in un `error` generico.
- **Più vincoli violati sullo stesso campo**: i messaggi vengono uniti con `"; "`, così nessun errore va perso.
- Le eccezioni del framework più comuni (JSON malformato, tipo sbagliato, parametro mancante, ecc.) hanno un handler dedicato: così il client riceve un `fail` 4xx comprensibile e non un generico 500.

## 4. Rifattorizzazione di controller e service

### Service (`AutoreService`, `LibroService`)
- I metodi di lettura non restituiscono più `Optional` e la cancellazione non restituisce più `boolean`: in caso di risorsa assente **lanciano `ResponseStatusException(HttpStatus.NOT_FOUND, "...")`**, con un messaggio specifico (es. `"Autore con id 99 non trovato"`).
- `LibroService.save` lancia 404 se l'autore indicato non esiste (prima restituiva un `Optional` vuoto).
- `LibroService.getByAutoreId` ora lancia 404 se l'autore non esiste, invece di restituire una lista vuota (che poteva essere confusa con "autore senza libri").
- La regola di business **"azzeramento dell'ID prima del salvataggio"** in `AutoreService.save` è rimasta invariata ed è verificata dai test.
- La dependency injection è passata **dai campi `@Autowired` al costruttore**: le dipendenze diventano `final` e i service si possono istanziare nei test con Mockito senza contesto Spring.
- Rimossi gli import inutili (`AutoreController` e `ResponseEntity` dentro `LibroService`).

### Controller (`AutoreController`, `LibroController`)
- Ogni endpoint restituisce `ResponseEntity<APIResponse<T>>` con `T` esplicito: `AutoreDTO`, `LibroDTO`, `List<AutoreDTO>`, `List<LibroDTO>` oppure `Void` per le DELETE.
- **Nessun blocco try-catch e nessun if/else per gli errori**: ogni metodo chiama il service e incapsula il risultato in `APIResponse.success(...)`. I casi di errore vengono gestiti dal `GlobalExceptionHandler`.
- Le entità JPA non vengono mai esposte: i controller usano solo i DTO.

### DTO e validazione
I DTO ora riproducono anche i limiti di lunghezza delle colonne del DB, così una richiesta non valida viene respinta con un 400 **prima** di arrivare al database:
- `AutoreDTO`: `nome` e `cognome` → `@NotBlank` + `@Size(max = 30)`.
- `LibroDTO`: `isbn` → `@NotBlank` + `@Size(max = 13)`; `titolo` → `@NotBlank` + `@Size(max = 50)`; `autore` → `@NotNull`.
- Ogni vincolo ha un messaggio specifico, che finisce nella mappa degli errori.

## 5. Test

Comando (dalla cartella `biblioteca/`):

```bash
./mvnw test
```

Per i test **non serve MySQL attivo**: in `src/test/resources/application.properties` c'è un database **H2 in memoria** (dipendenza `h2` con scope `test`), usato dal test `contextLoads` già presente. La configurazione di produzione (MySQL) resta invariata.

### 5.1 Test del Web Layer: `@WebMvcTest` + `MockMvc`

`AutoreControllerTest` e `LibroControllerTest` caricano solo il controller e il `GlobalExceptionHandler`; i service vengono sostituiti da mock con `@MockitoBean`. La struttura del JSON viene verificata con espressioni `jsonPath` (`$.status`, `$.data`, `$.data.<campo>`, `$.message`).

| Caso | Verifiche principali |
|---|---|
| **200 OK**: GET autore/libro, GET liste, GET libri di un autore, GET per titolo, POST valido, DELETE | `$.status == "success"`, contenuto di `$.data`, `$.data == null` e `$.message` per le DELETE, `$.message` assente nelle letture |
| **404 Not Found**: autore/libro inesistente in GET, DELETE, POST libro con autore inesistente | `$.status == "fail"`, `$.data.message` con il testo specifico |
| **400 Bad Request**: body non valido | `$.status == "fail"`, `$.data.<campo>` = messaggio di quel campo; il service **non** viene chiamato (`verify(..., never())`) |
| **400**: JSON malformato, id non numerico, parametro mancante | `$.status == "fail"` con la chiave che indica il problema |
| **500**: eccezione imprevista | `$.status == "error"`, `$.message` generico, il corpo **non contiene** né il messaggio né il nome dell'eccezione, nessun `$.trace` |

### 5.2 Unit test del Service: Mockito puro

`AutoreServiceTest` e `LibroServiceTest` usano `@ExtendWith(MockitoExtension.class)`, `@Mock` per Repository e Mapper e `@InjectMocks` per il service: nessun contesto Spring.

Regole di business verificate con `verify()` + `argThat()`:

- **Azzeramento dell'ID**: il client invia `id = 42`, e il test verifica che al repository arrivi un'entità con `id == null` (e nome/cognome corretti):
  ```java
  verify(autoreRepo).save(argThat((Autore a) ->
      a.getId() == null && "Italo".equals(a.getNome()) && "Calvino".equals(a.getCognome())));
  ```
- **Associazione libro → autore**: il `Libro` salvato è legato all'autore con id 1 recuperato dal DB.
- **Nessun effetto collaterale in caso di errore**: se l'autore non esiste, `libroRepo.save` e `mapper.toEntity` non vengono mai chiamati; se la risorsa da cancellare non esiste, `deleteById` non viene mai chiamato (`verify(..., never())`).
- Le eccezioni lanciate sono `ResponseStatusException` con stato `404 NOT_FOUND`.
- `getByTitolo` mappa solo il libro con titolo esatto; `getByAutoreId` non interroga i libri se l'autore non esiste.

### 5.3 Riepilogo della suite

| Classe | Tipo | N. test |
|---|---|---|
| `AutoreControllerTest` | `@WebMvcTest` + MockMvc | 13 |
| `LibroControllerTest` | `@WebMvcTest` + MockMvc | 10 |
| `AutoreServiceTest` | Mockito puro | 6 |
| `LibroServiceTest` | Mockito puro | 7 |
| `BibliotecaApplicationTests` | `@SpringBootTest` (H2) | 1 |
| **Totale** | | **37** |

## 6. Criteri di accettazione

| Requisito | Come è soddisfatto | Verificato da |
|---|---|---|
| Unico contratto `APIResponse` con stato `success`/`fail`/`error` | record generico `APIResponse<T>` + enum `APIStatus`, usato da tutti gli endpoint e da tutti gli handler | tutti i test del controller (`$.status`) |
| Uso del layer DTO, nessuna entità esposta | i controller ricevono e restituiscono solo `AutoreDTO`/`LibroDTO` | firme dei controller |
| Tipizzazione esplicita | `APIResponse<AutoreDTO>`, `APIResponse<List<LibroDTO>>`, `APIResponse<Map<String,String>>`, `APIResponse<Void>`; nessun `?`/`Object` | firme di controller e handler |
| Validazione preventiva → 400 con mappa campo → messaggio | `@Valid` + vincoli sui DTO + handler `MethodArgumentNotValidException` | `addAutore_nonValido_400`, `addAutore_nomeTroppoLungo_400`, `addLibro_nonValido_400` |
| Risorsa non trovata → 404 JSend `fail` | i service lanciano `ResponseStatusException(NOT_FOUND)` + handler dedicato | test `*_inesistente_404` |
| Nessuno stack trace verso il client | catch-all `Exception` → 500 `error` con messaggio generico, dettagli solo nei log | `eccezioneImprevista_500_senzaStackTrace` |
| Nessun try-catch nei controller | gli errori passano tutti dal `@ControllerAdvice` | codice dei controller |
| Test web con `@WebMvcTest` + MockMvc + `jsonPath` (200 e 404) | `AutoreControllerTest`, `LibroControllerTest` | — |
| Unit test del service con `MockitoExtension`, `@Mock`, `verify`, `argThat` | `AutoreServiceTest`, `LibroServiceTest` | `save_azzeraIdPrimaDelSalvataggio` |
| Branching `feature/task-x` → `deploy` | sviluppo su `feature/task-1`, merge `--no-ff` su `deploy` | `git log --graph` |

## 7. File modificati e aggiunti

```
biblioteca/pom.xml                                    (+ dipendenza h2 per i test)
biblioteca/src/main/java/it/marconi/biblioteca/
├── responses/APIResponse.java                        (nuovo)
├── responses/APIStatus.java                          (nuovo)
├── exceptions/GlobalExceptionHandler.java            (nuovo)
├── controllers/AutoreController.java                 (rifattorizzato)
├── controllers/LibroController.java                  (rifattorizzato)
├── services/AutoreService.java                       (rifattorizzato)
├── services/LibroService.java                        (rifattorizzato)
├── domain/AutoreDTO.java                             (vincoli @Size)
└── domain/LibroDTO.java                              (vincoli @Size + messaggi)
biblioteca/src/test/resources/application.properties  (nuovo, H2)
biblioteca/src/test/java/it/marconi/biblioteca/
├── controllers/AutoreControllerTest.java             (nuovo)
├── controllers/LibroControllerTest.java              (nuovo)
├── services/AutoreServiceTest.java                   (nuovo)
└── services/LibroServiceTest.java                    (nuovo)
docs/task-1-relazione.md                              (questa relazione)
README.md                                             (link alla relazione e formato delle risposte)
```
