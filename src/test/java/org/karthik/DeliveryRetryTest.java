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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.karthik.eventrelay.domain.DeliveryEntity;
import org.karthik.eventrelay.domain.DeliveryStatus;
import org.karthik.eventrelay.domain.DestinationEntity;
import org.karthik.eventrelay.service.DeliveryService;

@QuarkusTest
@TestProfile(EventRelayTestProfile.class)
class DeliveryRetryTest {
    private static final String API_KEY = "dev-admin-key";

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

        given()
                .when()
                .get("/deliveries/" + deliveryId + "/attempts")
                .then()
                .statusCode(200)
                .body("size()", equalTo(maxAttempts));
    }

    @Test
    void redriveResetsFailedDelivery() {
        String destinationId = createDestination("RedriveDest", "http://localhost:65535/webhook");
        String eventId = createEvent(destinationId);

        DeliveryEntity delivery = DeliveryEntity.find("eventId", UUID.fromString(eventId)).firstResult();
        UUID deliveryId = delivery.id;
        entityManager.clear();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<UUID> claimed = deliveryService.claimDueDeliveries(10);
            assertTrue(claimed.contains(deliveryId));
            deliveryService.dispatchDelivery(deliveryId);
        }

        given()
                .header("X-API-Key", API_KEY)
                .when()
                .post("/deliveries/" + deliveryId + "/redrive")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/deliveries?eventId=" + eventId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].status", equalTo("PENDING"))
                .body("[0].attemptCount", equalTo(0));
    }

    @Test
    void perDestinationConcurrencyLimitRespected() {
        String destinationId = createDestination("ConcurrencyDest", "http://localhost:65535/webhook");
        String eventIdOne = createEvent(destinationId);
        String eventIdTwo = createEvent(destinationId);

        DeliveryEntity deliveryOne = DeliveryEntity.find("eventId", UUID.fromString(eventIdOne)).firstResult();
        DeliveryEntity deliveryTwo = DeliveryEntity.find("eventId", UUID.fromString(eventIdTwo)).firstResult();
        entityManager.clear();

        List<UUID> claimed = deliveryService.claimDueDeliveries(10);
        assertEquals(1, claimed.size());

        DeliveryEntity refreshedOne = DeliveryEntity.findById(deliveryOne.id);
        DeliveryEntity refreshedTwo = DeliveryEntity.findById(deliveryTwo.id);

        boolean oneClaimed = refreshedOne.status == DeliveryStatus.IN_PROGRESS && refreshedTwo.status == DeliveryStatus.PENDING;
        boolean twoClaimed = refreshedTwo.status == DeliveryStatus.IN_PROGRESS && refreshedOne.status == DeliveryStatus.PENDING;
        assertTrue(oneClaimed || twoClaimed);
    }

    @Test
    void destinationCooldownSkipsNewClaims() {
        String destinationId = createDestination("CooldownDest", "http://localhost:65535/webhook");
        String eventId = createEvent(destinationId);

        DeliveryEntity delivery = DeliveryEntity.find("eventId", UUID.fromString(eventId)).firstResult();
        UUID deliveryId = delivery.id;
        entityManager.clear();

        for (int attempt = 0; attempt < 2; attempt++) {
            List<UUID> claimed = deliveryService.claimDueDeliveries(10);
            assertTrue(claimed.contains(deliveryId));
            deliveryService.dispatchDelivery(deliveryId);
        }

        DestinationEntity destination = DestinationEntity.findById(UUID.fromString(destinationId));
        assertTrue(destination.cooldownUntil != null);

        String eventIdTwo = createEvent(destinationId);
        DeliveryEntity deliveryTwo = DeliveryEntity.find("eventId", UUID.fromString(eventIdTwo)).firstResult();
        entityManager.clear();

        List<UUID> claimedAfterCooldown = deliveryService.claimDueDeliveries(10);
        assertTrue(claimedAfterCooldown.isEmpty());

        DeliveryEntity refreshed = DeliveryEntity.findById(deliveryTwo.id);
        assertEquals(DeliveryStatus.PENDING, refreshed.status);
    }

    private String createDestination(String name, String url) {
        String uniqueName = name + "-" + UUID.randomUUID();
        return given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", API_KEY)
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
                .header("X-API-Key", API_KEY)
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
