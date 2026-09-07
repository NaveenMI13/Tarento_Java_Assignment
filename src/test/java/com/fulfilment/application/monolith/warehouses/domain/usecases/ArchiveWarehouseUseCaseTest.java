package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private InMemoryWarehouseStore store;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    store = new InMemoryWarehouseStore();
    useCase = new ArchiveWarehouseUseCase(store);
  }

  @Test
  public void archiveSetsArchivedAt() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-002";
    warehouse.capacity = 20;
    warehouse.stock = 5;
    store.create(warehouse);

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertNull(store.findByBusinessUnitCode("MWH.100"));
    assertEquals(0, store.getAll().size());
  }

  @Test
  public void failWhenAlreadyArchived() {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-002";
    warehouse.capacity = 20;
    warehouse.stock = 5;
    store.create(warehouse);
    useCase.archive(warehouse);

    assertThrows(IllegalArgumentException.class, () -> useCase.archive(warehouse));
  }
}
