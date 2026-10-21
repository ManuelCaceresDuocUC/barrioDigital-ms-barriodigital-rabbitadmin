# ms-barriodigital-rabbitadmin

Microservicio administrador de RabbitMQ de BarrioDigital. Crea, consulta y elimina colas,
exchanges y bindings mediante una API REST protegida con JWT (solo rol `Admin`), de modo que
los demás servicios no tengan que administrar el broker directamente.

## Configuración (variables de entorno)

| Variable | Descripción | Por defecto |
|---|---|---|
| `RABBITMQ_HOST` | Host del broker (IP privada de ec2-mq) | `localhost` |
| `RABBITMQ_PORT` | Puerto AMQP | `5672` |
| `RABBITMQ_USER` | Usuario de RabbitMQ (obligatoria) | — |
| `RABBITMQ_PASSWORD` | Contraseña de RabbitMQ (obligatoria) | — |
| `RABBITMQ_MGMT_URL` | URL del Management API (puerto 15672) | `http://localhost:15672` |

Puerto del servicio: `8086`. Health: `/actuator/health`.

## Ejecución local

Con `RABBITMQ_USER` y `RABBITMQ_PASSWORD` definidas en el entorno:

    ./mvnw spring-boot:run

## Pruebas

    ./mvnw clean test

No requieren un broker real.
