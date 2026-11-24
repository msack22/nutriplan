package com.nutriplan.planner.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla outbox. Se inserta en la MISMA transacción que el cambio de negocio.
 * {@link OutboxRelay} la lee después, la publica en Kafka y marca {@code publishedAt}.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String topic;

    @Column(name = "message_key", length = 100)
    private String key;

    @Column(nullable = false, length = 150)
    private String type;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    private int attempts;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String key, String type, String payload) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.key = key;
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    public void registerFailedAttempt() {
        this.attempts++;
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }
}
