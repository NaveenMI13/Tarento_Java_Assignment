package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    List<Warehouse> warehouses = new ArrayList<>();
    for (DbWarehouse dbWarehouse : list("archivedAt is null")) {
      warehouses.add(dbWarehouse.toWarehouse());
    }
    return warehouses;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    persist(dbWarehouse);
    // give the domain object the generated id so REST can return it
    warehouse.id = dbWarehouse.id;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = null;

    // prefer update by id when we have it (archive by id)
    if (warehouse.id != null) {
      dbWarehouse = findById(warehouse.id);
    }

    // otherwise find the active row by business unit code (replace flow)
    if (dbWarehouse == null) {
      dbWarehouse =
          find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
              .firstResult();
    }

    if (dbWarehouse == null) {
      return;
    }

    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
            .firstResult();
    if (dbWarehouse != null) {
      delete(dbWarehouse);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    if (dbWarehouse == null) {
      return null;
    }
    return dbWarehouse.toWarehouse();
  }
}
