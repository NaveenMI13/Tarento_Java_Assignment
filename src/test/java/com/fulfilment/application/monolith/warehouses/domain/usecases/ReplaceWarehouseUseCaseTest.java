package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore store;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = new InMemoryWarehouseStore();
    useCase = new ReplaceWarehouseUseCase(store, new LocationGateway());

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "MWH.100";
    existing.location = "ZWOLLE-002";
    existing.capacity = 20;
    existing.stock = 8;
    store.create(existing);
  }

  @Test
  public void replaceWhenRequestIsValid() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.100";
    replacement.location = "ZWOLLE-002";
    replacement.capacity = 30;
    replacement.stock = 8;

    useCase.replace(replacement);

    Warehouse active = store.findByBusinessUnitCode("MWH.100");
    assertEquals(30, active.capacity);
    assertNotNull(active.createdAt);
  }

  @Test
  public void failWhenStockDoesNotMatch() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.100";
    replacement.location = "ZWOLLE-002";
    replacement.capacity = 30;
    replacement.stock = 3;

    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void failWhenCapacityCannotHoldStock() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.100";
    replacement.location = "VETSBY-001";
    replacement.capacity = 5;
    replacement.stock = 8;

    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void failWhenWarehouseDoesNotExist() {
    Warehouse replacement = new Warehouse();
    replacement.businessUnitCode = "MWH.999";
    replacement.location = "ZWOLLE-002";
    replacement.capacity = 20;
    replacement.stock = 8;

    assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
  }
}
