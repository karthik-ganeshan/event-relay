package org.karthik.eventrelay.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class DeliveryEntity extends PanacheEntityBase {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "event_id", nullable = false)
    public UUID eventId;

    @Column(name = "destination_id", nullable = false)
    public UUID destinationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    public int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    public Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    public Instant lastAttemptAt;

    @Column(name = "last_status_code")
    public Integer lastStatusCode;

    @Column(name = "last_error")
    public String lastError;

    @Column(name = "created_at", insertable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public Instant updatedAt;
}
