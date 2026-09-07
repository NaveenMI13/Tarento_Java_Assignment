package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing != null) {
      throw new IllegalArgumentException("Business unit code already exists");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Location is not valid");
    }

    int warehousesAtLocation = 0;
    int usedCapacity = 0;
    for (Warehouse current : warehouseStore.getAll()) {
      if (current.location.equals(warehouse.location)) {
        warehousesAtLocation++;
        usedCapacity = usedCapacity + current.capacity;
      }
    }

    if (warehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException("Maximum number of warehouses reached for this location");
    }

    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException("Capacity exceeds location maximum");
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException("Stock exceeds warehouse capacity");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(warehouse);
  }
}
