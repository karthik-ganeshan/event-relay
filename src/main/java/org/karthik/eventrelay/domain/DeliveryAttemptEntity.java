package org.karthik.eventrelay.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttemptEntity extends PanacheEntityBase {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "delivery_id", nullable = false)
    public UUID deliveryId;

    @Column(name = "attempt_no", nullable = false)
    public int attemptNo;

    @Column(name = "status_code")
    public Integer statusCode;

    @Column(name = "error")
    public String error;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    public Instant finishedAt;
}
