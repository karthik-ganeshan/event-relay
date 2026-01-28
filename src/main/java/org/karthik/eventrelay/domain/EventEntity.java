package org.karthik.eventrelay.domain;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "events")
public class EventEntity extends PanacheEntityBase {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "destination_id", nullable = false)
    public UUID destinationId;

    @Column(name = "idempotency_key")
    public String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    public JsonNode payload;

    @Column(name = "received_at", insertable = false, updatable = false)
    public Instant receivedAt;

    @Column(name = "request_id")
    public String requestId;

    @Column(name = "source_ip")
    public String sourceIp;

    @Column(name = "user_agent")
    public String userAgent;
}
