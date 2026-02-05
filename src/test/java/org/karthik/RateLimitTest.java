package org.karthik;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(RateLimitTestProfile.class)
class RateLimitTest {
    @Test
    void createDestinationRateLimited() {
        String nameOne = "RateDest-" + java.util.UUID.randomUUID();
        String nameTwo = "RateDest-" + java.util.UUID.randomUUID();
        String nameThree = "RateDest-" + java.util.UUID.randomUUID();

        assertEquals(201, createDestination(nameOne));
        assertEquals(201, createDestination(nameTwo));
        assertEquals(429, createDestination(nameThree));
    }

    private int createDestination(String name) {
        return given()
                .contentType(ContentType.JSON)
                .header("X-API-Key", "dev-admin-key")
                .body("{\"name\":\"" + name + "\",\"url\":\"https://example.com\"}")
                .when()
                .post("/destinations")
                .then()
                .extract()
                .statusCode();
    }
}
