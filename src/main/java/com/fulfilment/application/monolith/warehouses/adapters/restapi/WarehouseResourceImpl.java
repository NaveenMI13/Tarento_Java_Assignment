package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.ApiMessage;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;

  @Inject private CreateWarehouseOperation createWarehouseOperation;

  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    List<Warehouse> result = new ArrayList<>();
    for (com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse :
        warehouseRepository.getAll()) {
      result.add(toWarehouseResponse(warehouse));
    }
    return result;
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        toDomain(data);
    try {
      createWarehouseOperation.create(warehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    return toWarehouseResponse(findWarehouse(id));
  }

  @Override
  public ApiMessage archiveAWarehouseUnitByID(String id) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        findWarehouse(id);

    // Replace already archives the old row. List only shows active ones,
    // so tell the caller clearly instead of a vague failure.
    if (warehouse.archivedAt != null) {
      throw new WebApplicationException(
          "Warehouse with id "
              + id
              + " is already archived. Call GET /warehouse and use an active id.",
          400);
    }

    try {
      archiveWarehouseOperation.archive(warehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }

    ApiMessage response = new ApiMessage();
    response.setMessage("Warehouse archived successfully");
    return response;
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    if (warehouseRepository.findByBusinessUnitCode(businessUnitCode) == null) {
      throw new WebApplicationException("Warehouse not found", 404);
    }

    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        toDomain(data);
    warehouse.businessUnitCode = businessUnitCode;

    try {
      replaceWarehouseOperation.replace(warehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
    return toWarehouseResponse(warehouse);
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse findWarehouse(
      String id) {
    DbWarehouse entity;
    try {
      entity = warehouseRepository.findById(Long.valueOf(id));
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Warehouse with id of " + id + " does not exist.", 404);
    }

    if (entity == null) {
      throw new WebApplicationException("Warehouse with id of " + id + " does not exist.", 404);
    }
    return entity.toWarehouse();
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomain(
      Warehouse data) {
    com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse =
        new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    Warehouse response = new Warehouse();
    if (warehouse.id != null) {
      response.setId(String.valueOf(warehouse.id));
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
