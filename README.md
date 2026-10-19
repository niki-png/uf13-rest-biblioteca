# Spring Biblioteca

Webservice REST in Springboot per la gestione di una biblioteca

## Dependencies
- Spring Web
- Spring DevTools
- Spring Data JPA
- MySQL Driver
- Validator
- Lombok
- SpringDoc Open API

## Swagger UI

Swagger è un tool via browser che permette il testing delle nostre API.  

L'interfaccia Swagger è disponibile al seguente indirizzo:

```
http://localhost:8080/swagger-ui/index.html
```

## Formato delle risposte (JSend)

Tutti gli endpoint rispondono con lo stesso contratto `APIResponse`:

```json
{ "status": "success | fail | error", "data": { }, "message": "opzionale" }
```

- `success`: operazione riuscita (200)
- `fail`: errore del client, es. validazione (400) o risorsa non trovata (404); `data` descrive il problema
- `error`: errore interno (500); solo un messaggio generico, nessuno stack trace

## Test

```
cd biblioteca
./mvnw test
```

I test usano un database H2 in memoria, quindi non serve avere MySQL attivo.

## Documentazione

- [Relazione tecnica — Task 1: standardizzazione delle risposte e gestione centralizzata degli errori](docs/task-1-relazione.md)
