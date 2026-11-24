# 06 · Kafka y mensajería

## Para qué lo usamos (casos reales, no de juguete)

| Tópico | Productor | Consumidor | Qué resuelve |
|---|---|---|---|
| `nutriplan.recipes.ingredients-changed` | recipe-service (outbox) | pricing-service | pricing sabe qué ingredientes cotizar sin consultar a recipe |
| `nutriplan.pricing.prices-refreshed` | pricing-service (job lunes) | planner-service | invalidar la caché de listas de compras |
| `nutriplan.planner.plan-changed` | planner-service (outbox) | (abierto: analytics, notificaciones) | auditoría de cambios del plan |

Verlos en vivo: **Kafka UI** en http://localhost:8090.

## Conceptos

- **Tópico / partición / offset**: el orden se garantiza dentro de una partición; la *key* (`userId`, `"catalog"`) decide la partición.
- **Consumer group**: las réplicas de planner comparten `groupId = planner-service` → cada mensaje lo procesa **una** réplica.
- **KRaft**: Kafka 4 ya no usa ZooKeeper.
- **Productor idempotente** (`enable.idempotence=true`, `acks=all`): no duplica por reintentos de red.

## Transactional Outbox

Problema (*dual write*): guardar en MySQL y publicar en Kafka son dos sistemas; si uno falla quedan inconsistentes.
Solución: en la **misma transacción** del caso de uso se inserta una fila en `outbox_event`; un *relay* (`OutboxRelay`, `@Scheduled` cada 5 s) la publica y la marca. Garantía **at-least-once**. Alternativa en producción: CDC con **Debezium** leyendo el binlog.

## Consumidores robustos

- **Idempotencia**: `processed_event` guarda los `eventId` ya procesados (planner). En pricing la operación es idempotente por naturaleza (agregar a un set).
- **`@RetryableTopic`**: reintentos con backoff en tópicos `-retry` sin bloquear la partición; al agotarse → **DLT** (`-dlt`) y `@DltHandler` lo loguea.
- Serialización: los eventos viajan como JSON (`String`) con contratos compartidos en `common.events` (records). Alternativa: Avro + Schema Registry.

## ¿Kafka o RabbitMQ / SQS?
Kafka: log persistente, re-lectura, alto volumen, event streaming. RabbitMQ: colas de trabajo, routing flexible. SQS/SNS: gestionado en AWS. Para este proyecto cualquiera sirve; Kafka es lo que más piden.
