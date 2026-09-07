package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;

public class InMemoryWarehouseStore implements WarehouseStore {

  private List<Warehouse> warehouses = new ArrayList<>();
  private long nextId = 1;

  @Override
  public List<Warehouse> getAll() {
    List<Warehouse> result = new ArrayList<>();
    for (Warehouse warehouse : warehouses) {
      if (warehouse.archivedAt == null) {
        result.add(warehouse);
      }
    }
    return result;
  }

  @Override
  public void create(Warehouse warehouse) {
    warehouse.id = nextId;
    nextId = nextId + 1;
    warehouses.add(warehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    // nothing extra to do, we already hold the same object
  }

  @Override
  public void remove(Warehouse warehouse) {
    warehouses.remove(warehouse);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    for (Warehouse warehouse : warehouses) {
      if (warehouse.businessUnitCode.equals(buCode) && warehouse.archivedAt == null) {
        return warehouse;
      }
    }
    return null;
  }
}
