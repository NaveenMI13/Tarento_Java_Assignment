package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseEndpointTest {

  @Test
  public void testWarehouseEndpoints() {
    final String path = "warehouse";

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));

    given().when().get(path + "/1").then().statusCode(200).body(containsString("MWH.001"));

    String json =
        "{\"businessUnitCode\":\"MWH.200\",\"location\":\"ZWOLLE-002\",\"capacity\":20,\"stock\":4}";

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.200"), containsString("ZWOLLE-002"));

    given().when().delete(path + "/1").then().statusCode(204);

    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("ZWOLLE-001")), containsString("AMSTERDAM-001"));
  }
}
