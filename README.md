# Pedidos360

Sistema de pedidos con arquitectura de microservicios (Java 17 + Spring Boot) y frontend Angular.
Autenticación con Azure AD (IDaaS), mensajería asíncrona con **RabbitMQ** (Evaluación Parcial N°2, DSY1107) y streaming de eventos con **Kafka** (Evaluación Parcial N°3).

## Arquitectura

| Componente | Puerto | Rol |
|---|---|---|
| `frontend` (Angular) | 4200 | Login con Azure AD y consumo de las APIs con el JWT |
| `ms-productos` | 8082 | Catálogo de productos |
| `ms-pedidos` | 8081 | Crea pedidos y **publica** eventos en RabbitMQ |
| `ms-clientes` | 8084 | Gestión de clientes |
| `ms-notificaciones` | 8083 | **Consume** eventos: genera notificaciones, tickets y documentos |
| `ms-rabbit-admin` | 8085 | Administración de colas, exchanges y bindings vía REST |
| `ms-kafka-admin` | 8086 | Administración de tópicos y configuraciones de Kafka vía REST |

Todos los microservicios validan el JWT emitido por Azure AD (filtro de Spring Security + validador de audience).

## Flujo de mensajería

```mermaid
flowchart LR
    P[ms-pedidos<br/>POST /api/pedidos] -->|pedido.creado| E{{pedidos.exchange<br/>topic}}
    E -->|pedido.*| QN[notificaciones.pedido.queue]
    E -->|pedido.creado| QT[tickets.pedido.queue]
    E -->|pedido.creado| QD[documentos.pedido.queue]
    QN --> CN[PedidoNotificacionConsumer]
    QT --> CT[TicketConsumer]
    QD --> CD[DocumentoConsumer]
    QN -. rechazado .-> D{{pedidos.dlx<br/>direct}}
    QT -. rechazado .-> D
    QD -. rechazado .-> D
    D -->|notificaciones.dead| DN[notificaciones.pedido.dlq]
    D -->|tickets.dead| DT[tickets.pedido.dlq]
    D -->|documentos.dead| DD[documentos.pedido.dlq]
```

| Elemento | Nombre | Definido en |
|---|---|---|
| Exchange de eventos | `pedidos.exchange` (topic) | `application.yml` de ms-pedidos y ms-notificaciones |
| Exchange de mensajes muertos | `pedidos.dlx` (direct) | `application.yml` de ms-notificaciones |
| Colas | `notificaciones.pedido.queue`, `tickets.pedido.queue`, `documentos.pedido.queue` (quorum) | `application.yml` de ms-notificaciones |
| DLQ | `notificaciones.pedido.dlq`, `tickets.pedido.dlq`, `documentos.pedido.dlq` | `application.yml` de ms-notificaciones |
| Routing keys | `pedido.creado`, `pedido.cancelado`, `pedido.*`, `notificaciones.dead`, `tickets.dead`, `documentos.dead` | `application.yml` |

Los beans `Queue`, `Exchange` y `Binding` están en clases `@Configuration` separadas de la lógica de negocio
(`NotificacionesRabbitConfig`, `TicketsRabbitConfig`, `DocumentosRabbitConfig`, `RabbitCommonConfig`, `RabbitMQConfig`).

### Política de errores en los consumidores

Los consumidores usan ACK manual. La política es única y está en `ConsumerErrorHandler`:

| Situación | Acción |
|---|---|
| Procesado correctamente | `ACK` |
| Dato inválido (`IllegalArgumentException`, p. ej. `clienteEmail` vacío) | `NACK` sin reencolar, va a la **DLQ** |
| Error recuperable, primera vez | `NACK` con reencolado (un reintento) |
| Error recuperable, mensaje ya reentregado | `NACK` sin reencolar, va a la **DLQ** |

Cada decisión queda registrada en el log. El `DeadLetterConsumer` (opcional, ver más abajo) registra además cada mensaje que llega a cualquiera de las tres DLQ.

Si RabbitMQ no está disponible, `ms-pedidos` igual guarda el pedido: el fallo de publicación se registra en el log y no interrumpe la operación.

## Requisitos

- Java 17 o superior
- Node.js y npm (frontend)
- MySQL accesible (cada microservicio usa su propia base)
- RabbitMQ con plugin de management (puertos 5672 y 15672)

## Variables de entorno

| Variable | Usada por | Descripción | Valor por defecto |
|---|---|---|---|
| `RABBITMQ_ADDRESSES` | pedidos, notificaciones, admin | `host:puerto` del broker | `localhost:5672` |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | pedidos, notificaciones, admin | Credenciales de RabbitMQ | `guest` / `guest` |
| `RABBITMQ_MGMT_HOST` | admin | Host de la API de management (puerto 15672) | `localhost` |
| `DB_URL` | microservicios con BD | URL JDBC base | `jdbc:mysql://localhost:3306/<bd>` |
| `DB_PASSWORD` | microservicios con BD | Contraseña del usuario de BD | (obligatoria) |
| `DLQ_LISTENER_ENABLED` | notificaciones | `true` registra y vacía las DLQ | `false` |

No se suben credenciales al repositorio: se pasan por variables de entorno.

## Cómo ejecutar

En PowerShell, reemplazando `<IP_EC2>` por la dirección de RabbitMQ:

```powershell
$env:RABBITMQ_ADDRESSES = '<IP_EC2>:5672'
$env:RABBITMQ_MGMT_HOST = '<IP_EC2>'
$env:RABBITMQ_USER = '<usuario>'
$env:RABBITMQ_PASS = '<password>'
$env:DB_PASSWORD   = '<password-bd>'
```

Levantar **en este orden**, cada uno en su terminal con las variables anteriores:

```powershell
cd backend\ms-notificaciones ; .\mvnw spring-boot:run   # 1ro: declara colas, exchanges y bindings
cd backend\ms-pedidos        ; .\mvnw spring-boot:run   # 2do: productor
cd backend\ms-productos      ; .\mvnw spring-boot:run
cd backend\ms-clientes       ; .\mvnw spring-boot:run
cd backend\ms-rabbit-admin   ; .\mvnw spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm start          # http://localhost:4200
```

Dashboard de RabbitMQ: `http://<IP_EC2>:15672`

## Pruebas automáticas

```powershell
cd backend\<microservicio> ; .\mvnw test
```

Los tests de contexto usan el perfil `test` (H2 en memoria, sin MySQL ni RabbitMQ).

## API de ms-rabbit-admin

Requieren `Authorization: Bearer <JWT>`.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/queues` | Lista colas con mensajes y consumidores |
| POST | `/api/v1/queues` | Crea una cola |
| DELETE | `/api/v1/queues/{name}` | Elimina una cola |
| DELETE | `/api/v1/queues/{name}/messages` | Purga los mensajes de una cola |
| GET | `/api/v1/exchanges` | Lista exchanges |
| POST | `/api/v1/exchanges` | Crea un exchange |
| DELETE | `/api/v1/exchanges/{name}` | Elimina un exchange |
| GET | `/api/v1/bindings` | Lista bindings |
| POST | `/api/v1/bindings` | Crea un binding |
| DELETE | `/api/v1/bindings` | Elimina un binding (datos en el body) |

Ejemplos de body:

```json
POST /api/v1/queues
{ "name": "demo.queue", "durable": true, "type": "quorum",
  "deadLetterExchange": "pedidos.dlx", "deadLetterRoutingKey": "demo.dead", "messageTtlMs": 60000 }

POST /api/v1/exchanges
{ "name": "demo.exchange", "type": "topic", "durable": true }

POST /api/v1/bindings        (y DELETE /api/v1/bindings con el mismo body)
{ "exchange": "demo.exchange", "queue": "demo.queue", "routingKey": "demo.key" }
```

Validaciones: el nombre no puede estar vacío, solo admite letras, números, `.`, `_` y `-`, y no puede empezar con `amq.`.
`type` de cola: `classic` o `quorum` (quorum exige durable). `type` de exchange: `direct`, `topic`, `fanout` o `headers`.
Los errores de validación responden `400` con el detalle por campo; recursos inexistentes responden `404`.

## Cómo probar el flujo completo

1. Con todo levantado, crear un pedido desde el frontend (o `POST /api/pedidos` con `clienteEmail`).
2. En el log de `ms-pedidos`: `Evento publicado exchange=pedidos.exchange key=pedido.creado`.
3. En el log de `ms-notificaciones`: `ACK notificacion ...`, `ACK ticket TCK-<id>` y `ACK documento DOC-<id>`.
4. Consultar `GET /api/notificaciones/pedido/{id}`: aparecen la notificación (`PEDIDO_CREADO`), el ticket (`TICKET_GENERADO`) y el documento (`DOCUMENTO_GENERADO`). Los tres los genera RabbitMQ; el frontend ya no crea notificaciones por su cuenta, y también se ven en la vista Notificaciones.

### Probar la DLQ

1. Crear un pedido con `clienteEmail` vacío. El frontend siempre envía el correo de la cuenta, así que hay que hacerlo con Postman o curl:
   `POST /api/pedidos` con `{"clienteEmail":"","estado":"PENDIENTE","total":1000,"items":[]}` y el JWT en `Authorization`.
2. En el log de `ms-notificaciones`: `NACK->DLQ (no recuperable)`.
3. En el dashboard: `notificaciones.pedido.dlq` muestra 1 mensaje.
4. Para ver el registro de la DLQ en el log, reiniciar `ms-notificaciones` con `$env:DLQ_LISTENER_ENABLED='true'`:
   aparece `[DLQ] mensaje no entregado | colaOrigen=... motivo=...` y la DLQ queda vacía.

---

# Kafka (Evaluación Parcial N°3)

Kafka se agrega **en paralelo** a RabbitMQ: RabbitMQ sigue manejando notificaciones, tickets y documentos de los pedidos
(con su DLQ); Kafka se usa para streaming y analítica. Ninguna integración RabbitMQ fue modificada.

## Tópicos y casos de uso

| Tópico | Productor | Consumidor | Caso de uso |
|---|---|---|---|
| `pedidos.ventas` | `ms-pedidos` (`PedidoKafkaProducer`, al crear un pedido) | `ms-productos` (`PedidoVentaConsumer`) | Analítica de ventas: unidades e ingresos por producto, expuestos en `GET /api/analitica/ventas` |
| `clientes.registro` | `ms-clientes` (`ClienteEventProducer`, al crear un cliente) | `ms-notificaciones` (`ClienteEventConsumer`) | Genera la notificación de bienvenida (`CLIENTE_REGISTRADO`) |

La clave de cada mensaje es el id de la entidad (todos los eventos de un mismo pedido/cliente van a la misma partición).
Si Kafka no está disponible, el pedido o el cliente igual se guarda: el fallo se registra en el log (misma política que con RabbitMQ).

### Modelo de mensajes (JSON)

`pedidos.ventas`:

```json
{
  "pedidoId": 12,
  "clienteEmail": "cliente@duocuc.cl",
  "total": 25980.0,
  "estado": "PENDIENTE",
  "items": [ { "nombreProducto": "Teclado", "cantidad": 2, "precioUnitario": 12990.0 } ],
  "ocurridoEn": "2026-11-03T21:15:00.123"
}
```

`clientes.registro`:

```json
{
  "clienteId": 3,
  "nombre": "Ana Perez",
  "email": "ana@duocuc.cl",
  "tipo": "CLIENTE_REGISTRADO",
  "ocurridoEn": "2026-11-03T21:15:00.123"
}
```

Los consumidores usan `ErrorHandlingDeserializer`: un mensaje con JSON inválido se registra en el log y no bloquea el consumo.

## Infraestructura (docker-compose)

| Archivo | Levanta |
|---|---|
| `docker-compose.yml` (raíz) | Clúster RabbitMQ de 2 nodos (puertos 5672/5673, dashboard 15672/15673) |
| `kafka/docker-compose.kafka.yml` | Zookeeper + 2 brokers Kafka (puertos 9092 y 9093) |
| `kafka/docker-compose.kafka-ui.yml` | Kafka UI (puerto 8090) conectada a los brokers |

```powershell
cd kafka
docker compose -f docker-compose.kafka.yml up -d
docker compose -f docker-compose.kafka-ui.yml up -d      # después de que los brokers estén arriba
```

`KAFKA_HOST` define con qué dirección llegan los clientes externos a los brokers (por defecto `localhost`; en AWS debe ser la IP o DNS
de la EC2). Se configura copiando `kafka/env.example` a `kafka/.env`. Kafka UI: `http://localhost:8090` (usuario/clave `pedidos360` / `pedidos360`
por defecto, variables `KAFKA_UI_USER` / `KAFKA_UI_PASS`).

## Variables de entorno adicionales

| Variable | Usada por | Descripción | Valor por defecto |
|---|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | pedidos, clientes, productos, notificaciones, kafka-admin | Brokers separados por coma | `localhost:9092,localhost:9093` |

## Cómo ejecutar los servicios con Kafka

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = '<IP_KAFKA>:9092,<IP_KAFKA>:9093'
cd backend\ms-kafka-admin ; .\mvnw spring-boot:run     # puerto 8086
```

Los demás microservicios se levantan igual que antes, con la variable `KAFKA_BOOTSTRAP_SERVERS` definida.

## API de ms-kafka-admin

Requieren `Authorization: Bearer <JWT>` (mismo filtro y validación de audience que el resto).

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/cluster` | Brokers y controlador del clúster |
| GET | `/api/topics` | Lista los tópicos (particiones y factor de replicación) |
| GET | `/api/topics/{name}` | Detalle: particiones, líder, réplicas, ISR y configuraciones |
| POST | `/api/topics` | Crea un tópico (con configuraciones opcionales) |
| PUT | `/api/topics/{name}` | Modifica: aumenta particiones y/o cambia configuraciones |
| DELETE | `/api/topics/{name}` | Elimina un tópico |
| GET | `/api/topics/{name}/configs` | Lista las configuraciones del tópico |
| PUT | `/api/topics/{name}/configs` | Cambia o restablece configuraciones |
| GET | `/api/consumer-groups` | Lista los consumer groups |
| GET | `/api/consumer-groups/{id}` | Detalle del grupo: estado, miembros y offsets |
| DELETE | `/api/consumer-groups/{id}` | Elimina un grupo (debe estar sin miembros activos) |

```json
POST /api/topics
{ "name": "demo.topic", "partitions": 2, "replicationFactor": 2, "configs": { "retention.ms": "86400000" } }

PUT /api/topics/demo.topic
{ "partitions": 4, "configs": { "retention.ms": "3600000" }, "resetConfigs": ["cleanup.policy"] }

PUT /api/topics/demo.topic/configs
{ "configs": { "retention.ms": "7200000" }, "resetConfigs": [] }
```

Validaciones: nombre obligatorio (letras, números, `.`, `_`, `-`), `partitions` y `replicationFactor` >= 1 y solo se pueden **aumentar** particiones
(Kafka no permite reducirlas). Respuestas: `400` datos inválidos, `404` tópico inexistente, `409` tópico ya existe, `502` Kafka no disponible.
Los tópicos internos (`__consumer_offsets`, etc.) no se pueden eliminar.

## Cómo probar el flujo Kafka

1. Con Kafka y los servicios levantados, crear un pedido desde el frontend (o `POST /api/pedidos`).
2. Log de `ms-pedidos`: `Evento Kafka publicado topic=pedidos.ventas ...`.
3. Log de `ms-productos`: `Venta procesada pedidoId=...`; `GET /api/analitica/ventas` muestra el ranking de productos.
4. Crear un cliente (`POST /api/clientes`): log de `ms-notificaciones` `Notificacion de bienvenida generada ...` y la notificación aparece en `GET /api/notificaciones`.
5. En Kafka UI (`http://localhost:8090`) → Topics → `pedidos.ventas` → Messages: se ve el JSON publicado y los offsets por partición.
