package org.karthik;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class SecurityTest {
    @Test
    void createDestinationRequiresApiKey() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"SecDest\",\"url\":\"https://example.com\"}")
                .when()
                .post("/destinations")
                .then()
                .statusCode(401);
    }

    @Test
    void createDestinationRejectsInvalidApiKey() {
        given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "wrong-key")
                .body("{\"name\":\"SecDest\",\"url\":\"https://example.com\"}")
                .when()
                .post("/destinations")
                .then()
                .statusCode(403);
    }

    @Test
    void createDestinationAcceptsValidApiKey() {
        String uniqueName = "SecDest-" + java.util.UUID.randomUUID();
        given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "dev-admin-key")
                .body("{\"name\":\"" + uniqueName + "\" ,\"url\":\"https://example.com\"}")
                .when()
                .post("/destinations")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    private io.restassured.response.Response createDestination(String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "dev-admin-key")
                .body("{\"name\":\"" + name + "\",\"url\":\"https://example.com\"}")
                .when()
                .post("/destinations")
                .then()
                .extract()
                .response();
    }
}
