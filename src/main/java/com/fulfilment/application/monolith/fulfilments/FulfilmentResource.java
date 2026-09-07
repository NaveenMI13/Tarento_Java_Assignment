package com.fulfilment.application.monolith.fulfilments;

import com.fulfilment.application.monolith.common.ApiMessage;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject ProductRepository productRepository;

  @Inject WarehouseRepository warehouseRepository;

  @GET
  public List<Fulfilment> get() {
    return Fulfilment.listAll();
  }

  @POST
  @Transactional
  public Response create(Fulfilment fulfilment) {
    if (fulfilment.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    if (fulfilment.productId == null
        || fulfilment.storeId == null
        || fulfilment.warehouseId == null) {
      throw new WebApplicationException("productId, storeId and warehouseId are required.", 422);
    }

    Product product = productRepository.findById(fulfilment.productId);
    if (product == null) {
      throw new WebApplicationException(
          "Product with id of " + fulfilment.productId + " does not exist.", 404);
    }

    Store store = Store.findById(fulfilment.storeId);
    if (store == null) {
      throw new WebApplicationException(
          "Store with id of " + fulfilment.storeId + " does not exist.", 404);
    }

    DbWarehouse warehouse = warehouseRepository.findById(fulfilment.warehouseId);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new WebApplicationException(
          "Warehouse with id of " + fulfilment.warehouseId + " does not exist.", 404);
    }

    Fulfilment existing =
        Fulfilment.find(
                "productId = ?1 and storeId = ?2 and warehouseId = ?3",
                fulfilment.productId,
                fulfilment.storeId,
                fulfilment.warehouseId)
            .firstResult();
    if (existing != null) {
      throw new WebApplicationException(
          "This warehouse is already assigned to this product for this store.", 409);
    }

    if (countWarehousesForProductAtStore(
            fulfilment.productId, fulfilment.storeId, fulfilment.warehouseId)
        > 2) {
      throw new WebApplicationException(
          "A product can only be fulfilled by 2 warehouses per store.", 400);
    }

    if (countWarehousesForStore(fulfilment.storeId, fulfilment.warehouseId) > 3) {
      throw new WebApplicationException("A store can only be fulfilled by 3 warehouses.", 400);
    }

    if (countProductsForWarehouse(fulfilment.warehouseId, fulfilment.productId) > 5) {
      throw new WebApplicationException("A warehouse can only store 5 product types.", 400);
    }

    fulfilment.persist();
    return Response.ok(fulfilment).status(201).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Fulfilment entity = Fulfilment.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Fulfilment with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    return Response.ok(new ApiMessage("Fulfilment deleted successfully")).build();
  }

  private int countWarehousesForProductAtStore(
      Long productId, Long storeId, Long newWarehouseId) {
    List<Long> warehouses = new ArrayList<>();
    List<Fulfilment> rows = Fulfilment.list("productId = ?1 and storeId = ?2", productId, storeId);
    for (Fulfilment row : rows) {
      if (!warehouses.contains(row.warehouseId)) {
        warehouses.add(row.warehouseId);
      }
    }
    if (!warehouses.contains(newWarehouseId)) {
      warehouses.add(newWarehouseId);
    }
    return warehouses.size();
  }

  private int countWarehousesForStore(Long storeId, Long newWarehouseId) {
    List<Long> warehouses = new ArrayList<>();
    List<Fulfilment> rows = Fulfilment.list("storeId", storeId);
    for (Fulfilment row : rows) {
      if (!warehouses.contains(row.warehouseId)) {
        warehouses.add(row.warehouseId);
      }
    }
    if (!warehouses.contains(newWarehouseId)) {
      warehouses.add(newWarehouseId);
    }
    return warehouses.size();
  }

  private int countProductsForWarehouse(Long warehouseId, Long newProductId) {
    List<Long> products = new ArrayList<>();
    List<Fulfilment> rows = Fulfilment.list("warehouseId", warehouseId);
    for (Fulfilment row : rows) {
      if (!products.contains(row.productId)) {
        products.add(row.productId);
      }
    }
    if (!products.contains(newProductId)) {
      products.add(newProductId);
    }
    return products.size();
  }
}
