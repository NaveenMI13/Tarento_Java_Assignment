package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore store;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = new InMemoryWarehouseStore();
    useCase = new CreateWarehouseUseCase(store, new LocationGateway());
  }

  @Test
  public void createWhenRequestIsValid() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-002";
    warehouse.capacity = 20;
    warehouse.stock = 5;

    useCase.create(warehouse);

    assertEquals(1, store.getAll().size());
    assertEquals("MWH.100", store.getAll().get(0).businessUnitCode);
  }

  @Test
  public void failWhenBusinessUnitCodeAlreadyExists() {
    Warehouse first = new Warehouse();
    first.businessUnitCode = "MWH.100";
    first.location = "ZWOLLE-002";
    first.capacity = 10;
    first.stock = 1;
    useCase.create(first);

    Warehouse second = new Warehouse();
    second.businessUnitCode = "MWH.100";
    second.location = "AMSTERDAM-002";
    second.capacity = 10;
    second.stock = 1;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(second));
  }

  @Test
  public void failWhenLocationIsUnknown() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "NOWHERE-001";
    warehouse.capacity = 10;
    warehouse.stock = 1;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void failWhenLocationIsFull() {
    Warehouse first = new Warehouse();
    first.businessUnitCode = "MWH.100";
    first.location = "HELMOND-001";
    first.capacity = 10;
    first.stock = 1;
    useCase.create(first);

    Warehouse second = new Warehouse();
    second.businessUnitCode = "MWH.101";
    second.location = "HELMOND-001";
    second.capacity = 10;
    second.stock = 1;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(second));
  }

  @Test
  public void failWhenStockIsHigherThanCapacity() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-002";
    warehouse.capacity = 5;
    warehouse.stock = 10;

    assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
  }
}
