package org.karthik;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.service.DeliveryService;

@QuarkusTest
@TestProfile(EventRelayTestProfile.class)
class DeliveryRetryTest {
    @Inject
    DeliveryService deliveryService;

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "eventrelay.worker.max-attempts")
    int maxAttempts;

    @Test
    void deliveryRetriesThenDeadLetters() {
        String destinationId = createDestination("RetryDest", "http://localhost:65535/webhook");
        String eventId = createEvent(destinationId);

        DeliveryEntity delivery = DeliveryEntity.find("eventId", UUID.fromString(eventId)).firstResult();
        UUID deliveryId = delivery.id;
        entityManager.clear();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<UUID> claimed = deliveryService.claimDueDeliveries(10);
            assertTrue(claimed.contains(deliveryId));
            deliveryService.dispatchDelivery(deliveryId);
        }

        DeliveryEntity afterSecond = DeliveryEntity.findById(deliveryId);
        assertEquals(DeliveryStatus.FAILED, afterSecond.status);
        assertEquals("Max attempts exceeded", afterSecond.lastError);
    }

    private String createDestination(String name, String url) {
        String uniqueName = name + "-" + UUID.randomUUID();
        return given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + uniqueName + "\",\"url\":\"" + url + "\"}")
                .when()
                .post("/destinations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id");
    }

    private String createEvent(String destinationId) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"destinationId\":\"" + destinationId + "\",\"payload\":{\"userId\":\"123\"}}")
                .when()
                .post("/events")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id");
    }
}
