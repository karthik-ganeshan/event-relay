package org.karthik.eventrelay.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "destinations")
public class DestinationEntity extends PanacheEntityBase {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "name", nullable = false, unique = true)
    public String name;

    @Column(name = "url", nullable = false)
    public String url;

    @Column(name = "auth_type")
    public String authType;

    @Column(name = "auth_secret")
    public String authSecret;

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public Instant updatedAt;
}
