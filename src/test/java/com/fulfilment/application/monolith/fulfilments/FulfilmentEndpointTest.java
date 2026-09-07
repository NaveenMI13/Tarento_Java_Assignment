package com.fulfilment.application.monolith.fulfilments;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FulfilmentEndpointTest {

  @Test
  public void assignTwoWarehousesThenRejectAThirdForTheSameProductAndStore() {
    // product 2 / store 2 are not touched by the other smoke tests
    given()
        .contentType("application/json")
        .body("{\"productId\":2,\"storeId\":2,\"warehouseId\":1}")
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body("{\"productId\":2,\"storeId\":2,\"warehouseId\":2}")
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body("{\"productId\":2,\"storeId\":2,\"warehouseId\":3}")
        .when()
        .post("/fulfilment")
        .then()
        .statusCode(400)
        .body(containsString("2 warehouses per store"));
  }
}
