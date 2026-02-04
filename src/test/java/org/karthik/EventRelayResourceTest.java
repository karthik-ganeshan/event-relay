package org.karthik;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class EventRelayResourceTest {
    private static final String API_KEY = "dev-admin-key";

    @Test
    void createDestinationAndEventCreatesDelivery() {
        String destinationId = createDestination("DiscordBot", "https://example.com/webhook");

        Response eventResponse = given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", API_KEY)
                .body("{\"destinationId\":\"" + destinationId + "\",\"payload\":{\"userId\":\"123\"}}")
                .when()
                .post("/events")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .response();

        String eventId = eventResponse.jsonPath().getString("id");

        given()
                .when()
                .get("/events/" + eventId)
                .then()
                .statusCode(200)
                .body("id", CoreMatchers.equalTo(eventId))
                .body("destinationId", CoreMatchers.equalTo(destinationId));

        given()
                .when()
                .get("/deliveries?eventId=" + eventId)
                .then()
                .statusCode(200)
                .body("size()", CoreMatchers.equalTo(1))
                .body("[0].status", CoreMatchers.equalTo("PENDING"));
    }

    @Test
    void idempotencyReturnsExistingEventId() {
        String destinationId = createDestination("DiscordBot-Idem", "https://example.com/webhook");
        String idempotencyKey = UUID.randomUUID().toString();

        String body = "{\"destinationId\":\"" + destinationId + "\",\"idempotencyKey\":\"" + idempotencyKey + "\",\"payload\":{\"userId\":\"123\"}}";

        String firstId = given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", API_KEY)
                .body(body)
                .when()
                .post("/events")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");

        String secondId = given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", API_KEY)
                .body(body)
                .when()
                .post("/events")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("id");

        org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
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
}
