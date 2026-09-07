package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new IllegalArgumentException("Warehouse not found");
    }

    if (!existing.stock.equals(newWarehouse.stock)) {
      throw new IllegalArgumentException("Stock must match the warehouse being replaced");
    }

    if (newWarehouse.capacity < existing.stock) {
      throw new IllegalArgumentException("New capacity cannot hold current stock");
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Location is not valid");
    }

    int warehousesAtLocation = 0;
    int usedCapacity = 0;
    for (Warehouse current : warehouseStore.getAll()) {
      if (current.businessUnitCode.equals(existing.businessUnitCode)) {
        continue;
      }
      if (current.location.equals(newWarehouse.location)) {
        warehousesAtLocation++;
        usedCapacity = usedCapacity + current.capacity;
      }
    }

    if (warehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException("Maximum number of warehouses reached for this location");
    }

    if (usedCapacity + newWarehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException("Capacity exceeds location maximum");
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
