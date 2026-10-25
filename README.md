# ms-barriodigital-rabbitadmin

Microservicio administrador de RabbitMQ de BarrioDigital. Crea, consulta y elimina colas,
exchanges y bindings mediante una API REST protegida con JWT (solo rol `Admin`), de modo que
los demás servicios no tengan que administrar el broker directamente.

- Puerto: `8086`. Health: `/actuator/health`.
- Documentación interactiva (OpenAPI/Swagger UI): `http://localhost:8086/swagger-ui.html`
  y definición en `http://localhost:8086/v3/api-docs`.
- Colección de Postman y archivo `.http` en [`docs/`](docs/).

## Arquitectura

| Capa | Responsabilidad |
|---|---|
| `controller` | Endpoints REST y códigos HTTP. No conocen la librería de mensajería. |
| `service` (`RabbitAdminService`) | Reglas de administración y protección de la topología. Único uso de `AmqpAdmin`. |
| `client` | Lectura desde el Management API de RabbitMQ (listados y bindings). |
| `dto` / `exception` | Contratos de entrada y salida, validación y errores en JSON (`ApiError`). |
| `config` / `security` | Propiedades, seguridad JWT (rol `Admin`) y OpenAPI. |

## Configuración (variables de entorno)

| Variable | Descripción | Por defecto |
|---|---|---|
| `RABBITMQ_HOST` | Host del broker (IP privada de ec2-mq) | `localhost` |
| `RABBITMQ_PORT` | Puerto AMQP | `5672` |
| `RABBITMQ_USER` | Usuario de RabbitMQ (obligatoria) | — |
| `RABBITMQ_PASSWORD` | Contraseña de RabbitMQ (obligatoria) | — |
| `RABBITMQ_MGMT_URL` | URL del Management API (puerto 15672) | `http://localhost:15672` |
| `JWT_ISSUER_URI` | Emisor de los tokens de Entra ID | tenant de BarrioDigital |
| `OPENAPI_ENABLED` | `false` apaga `/v3/api-docs` y Swagger UI | `true` |

## Ejecución local

Con `RABBITMQ_USER` y `RABBITMQ_PASSWORD` definidas en el entorno:

    ./mvnw spring-boot:run

## Pruebas

    ./mvnw clean test

No requieren un broker real ni acceso a Entra ID: `AmqpAdmin` y el cliente del Management API
se simulan con Mockito, y el decoder JWT de los tests rechaza cualquier token (los roles se
inyectan con `spring-security-test`).

| Qué se prueba | Clases | Cobertura |
|---|---|---|
| Servicio de administración con `AmqpAdmin` simulado | `RabbitAdminServiceImplTest` | Crear, consultar, eliminar y vaciar colas; exchanges y bindings; protección de `q.cmd.*` y `cmd.*`; nombres reservados; fallas del broker |
| Cliente del Management API | `RabbitManagementClientTest` | Listados, 404 como vacío y errores aguas arriba |
| Controladores (MockMvc con seguridad real) | `QueueControllerTest`, `ExchangeControllerTest`, `BindingControllerTest` | 201, 204, 400, 401, 403 (rol no Admin), 404, 409 y 502 por endpoint |
| Validación de entrada | `RequestValidationTest`, `ValidationErrorsTest` | Nombre vacío, con espacios o con `amq.`, tipo inexistente, TTL no positivo, routing key larga |
| Seguridad y roles | `SecurityConfigTest`, `JwtRolesConversionTest`, `OpenApiAccessTest` | 401/403 en JSON, conversión del claim `roles`, docs públicas y API protegida |
| Configuración y arranque | `ManagementPropertiesTest`, `ProtectedResourcesTest`, `OpenApiConfigTest`, `RabbitAdminApplicationTests` | Propiedades, patrones protegidos, esquema OpenAPI y carga del contexto |

Los tests de controladores usan `@SpringJUnitWebConfig` con `@EnableWebMvc` y el filtro de
seguridad real (`springSecurity()`), en lugar de la anotación de slice `@WebMvcTest`: así prueban
el mismo contrato HTTP sin depender de los paquetes de auto-configuración de test de Spring Boot 4.

## Seguridad

Todas las rutas de `/api/rabbit/**` exigen un JWT de Entra ID con el rol `Admin`
(claim `roles`). Sin token responde **401** y con otro rol (Operador, Cliente, Auditor) **403**,
ambos con cuerpo JSON. Solo `/actuator/health`, `/v3/api-docs` y Swagger UI son públicos; el
puerto 8086 se publica únicamente en `127.0.0.1` de ec2-apps.

## Endpoints

| Método | Ruta | Respuesta | Descripción |
|---|---|---|---|
| POST | `/api/rabbit/queues` | 201 | Crea una cola (`Location` relativa) |
| GET | `/api/rabbit/queues` | 200 | Lista las colas |
| GET | `/api/rabbit/queues/{name}` | 200 | Detalle con mensajes y consumidores en tiempo real |
| DELETE | `/api/rabbit/queues/{name}` | 204 | Elimina una cola |
| DELETE | `/api/rabbit/queues/{name}/messages` | 204 | Vacía una cola |
| GET | `/api/rabbit/queues/{name}/messages?count=` | 200 | Lee los mensajes de una DLQ sin consumirlos (correo y RUT enmascarados) |
| POST | `/api/rabbit/exchanges` | 201 | Crea un exchange (`DIRECT`, `TOPIC`, `FANOUT`, `HEADERS`) |
| GET | `/api/rabbit/exchanges` | 200 | Lista los exchanges |
| DELETE | `/api/rabbit/exchanges/{name}` | 204 | Elimina un exchange |
| POST | `/api/rabbit/bindings` | 201 | Crea un binding exchange → cola |
| GET | `/api/rabbit/bindings?queue=` | 200 | Lista bindings (filtro por cola opcional) |
| DELETE | `/api/rabbit/bindings?exchange=&queue=&routingKey=` | 204 | Elimina un binding |

El borrado de bindings usa parámetros de URL porque un binding no tiene identificador propio.
Una routing key con `#` se envía como `%23`.

### Lectura de mensajes de las DLQ

`GET /api/rabbit/queues/{name}/messages?count=20` devuelve el contenido de los mensajes de una cola de
mensajes fallidos para diagnosticar por qué fallaron, sin tener que entrar al servidor.

- **Solo DLQ.** Únicamente las colas cuyo nombre coincide con `barriodigital.rabbitadmin.inspection.queues`
  (por defecto `*.dlq`). En cualquier otra cola responde **403**, porque leer una cola con consumidores
  activos podría interferir con ellos.
- **No consume.** Los mensajes se devuelven a la cola, pero RabbitMQ los marca como *reentregados*
  (`redelivered: true`).
- **Datos personales enmascarados.** El correo (`m***@gmail.com`) y el RUT (`[RUT]`) se ocultan antes de
  responder, con las mismas reglas que los logs de notify.
- **Límites.** `count` por defecto 20 y máximo 50 (`default-messages`, `max-messages`); el cuerpo se
  recorta a `max-payload-bytes` (20 000) y se indica con `payloadTruncated`. Un contenido binario no se entrega.
- **Motivo del fallo.** El objeto `death` trae la cola y el exchange de origen, las routing keys, el motivo
  (`rejected` = NACK sin reencolar), el número de veces y la hora (cabecera `x-death`). El texto exacto de la
  excepción no viaja en el mensaje: está en los logs de notify.

Ejemplo de respuesta:

    [{
      "exchange": "cmd.dead.dlx",
      "routingKey": "crew.dlq",
      "redelivered": true,
      "payload": "{\"type\":\"CREW_TICKET\",...}",
      "binary": false,
      "payloadBytes": 215,
      "payloadTruncated": false,
      "death": {
        "queue": "q.cmd.crew",
        "exchange": "cmd.direct",
        "routingKeys": ["crew.ticket"],
        "reason": "rejected",
        "count": 1,
        "time": "2026-10-25T21:10:00Z"
      }
    }]

### Validación de entrada

| Campo | Regla |
|---|---|
| `name` (cola/exchange), `exchange` y `queue` de un binding | Obligatorio; `^[A-Za-z0-9._-]{1,255}$`; no puede empezar con `amq.` |
| `deadLetterExchange` | Opcional; mismo patrón de caracteres |
| `type` del exchange | Obligatorio; `DIRECT`, `TOPIC`, `FANOUT` o `HEADERS` (en mayúsculas) |
| `routingKey`, `deadLetterRoutingKey` | Hasta 255 caracteres (puede ser vacía) |
| `messageTtlMs` | Entero positivo (milisegundos) |

### Formato de error

    {
      "timestamp": "2026-10-21T17:03:24.114Z",
      "status": 400,
      "error": "Bad Request",
      "message": "Datos inválidos: name: El nombre debe tener …",
      "fieldErrors": { "name": "El nombre debe tener …" }
    }

`fieldErrors` solo aparece en errores de validación. Códigos: 400 entrada inválida,
401 sin token, 403 sin rol Admin, 404 recurso inexistente, 409 ya existe o protegido,
502 RabbitMQ no responde.

## Topología del sistema: qué pasa al eliminar

| Acción | Resultado |
|---|---|
| Eliminar un exchange `amq.*` o el exchange por defecto | 400 |
| Eliminar un exchange `cmd.*` o una cola `q.cmd.*` | 409, no se elimina |
| Vaciar una cola `q.cmd.*` (por ejemplo una DLQ) | 204, se permite |
| Eliminar un binding entre una cola `q.cmd.*` y un exchange `cmd.*` | 409 |
| Eliminar recursos propios (cualquier otro nombre) | 204 |

Los patrones protegidos se configuran en `barriodigital.rabbitadmin.protection` (`application.yaml`).

## Ejemplos con curl

Todos los comandos necesitan un token con rol `Admin`:

    export TOKEN='<JWT de Entra ID con rol Admin>'
    H="Authorization: Bearer $TOKEN"
    B=http://localhost:8086/api/rabbit

Colas:

    curl -i -X POST $B/queues -H "$H" -H 'Content-Type: application/json' \
      -d '{"name":"q.prueba","messageTtlMs":60000}'
    curl -s $B/queues -H "$H"
    curl -s $B/queues/q.prueba -H "$H"
    curl -i -X DELETE $B/queues/q.prueba/messages -H "$H"
    curl -i -X DELETE $B/queues/q.prueba -H "$H"

Exchanges:

    curl -i -X POST $B/exchanges -H "$H" -H 'Content-Type: application/json' \
      -d '{"name":"ex.prueba","type":"TOPIC"}'
    curl -s $B/exchanges -H "$H"
    curl -i -X DELETE $B/exchanges/ex.prueba -H "$H"

Bindings:

    curl -i -X POST $B/bindings -H "$H" -H 'Content-Type: application/json' \
      -d '{"exchange":"ex.prueba","queue":"q.prueba","routingKey":"prueba.#"}'
    curl -s "$B/bindings?queue=q.prueba" -H "$H"
    curl -i -X DELETE "$B/bindings?exchange=ex.prueba&queue=q.prueba&routingKey=prueba.%23" -H "$H"

Definición OpenAPI (sin token):

    curl -s http://localhost:8086/v3/api-docs
