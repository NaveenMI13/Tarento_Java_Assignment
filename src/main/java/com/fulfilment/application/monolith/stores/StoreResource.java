package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.common.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  public Response create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    QuarkusTransaction.begin();
    try {
      store.persist();
      QuarkusTransaction.commit();
    } catch (RuntimeException e) {
      if (QuarkusTransaction.isActive()) {
        QuarkusTransaction.rollback();
      }
      throw e;
    }

    legacyStoreManagerGateway.createStoreOnLegacySystem(store);
    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity;
    QuarkusTransaction.begin();
    try {
      entity = Store.findById(id);
      if (entity == null) {
        throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
      }
      entity.name = updatedStore.name;
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
      QuarkusTransaction.commit();
    } catch (RuntimeException e) {
      if (QuarkusTransaction.isActive()) {
        QuarkusTransaction.rollback();
      }
      throw e;
    }

    legacyStoreManagerGateway.updateStoreOnLegacySystem(entity);
    return entity;
  }

  @PATCH
  @Path("{id}")
  public Store patch(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity;
    QuarkusTransaction.begin();
    try {
      entity = Store.findById(id);
      if (entity == null) {
        throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
      }
      if (entity.name != null) {
        entity.name = updatedStore.name;
      }
      if (entity.quantityProductsInStock != 0) {
        entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
      }
      QuarkusTransaction.commit();
    } catch (RuntimeException e) {
      if (QuarkusTransaction.isActive()) {
        QuarkusTransaction.rollback();
      }
      throw e;
    }

    legacyStoreManagerGateway.updateStoreOnLegacySystem(entity);
    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    return Response.ok(new ApiMessage("Store deleted successfully")).build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Inject ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
      Throwable root = unwrap(exception);

      int code = 500;
      String message = root.getMessage();

      if (root instanceof WebApplicationException webEx) {
        code = webEx.getResponse().getStatus();
        if (webEx.getMessage() != null) {
          message = webEx.getMessage();
        }
      } else if (isUniqueConstraintViolation(root)) {
        code = 409;
        message = "Duplicate value violates a unique constraint (e.g. name already exists).";
      }

      // 404 / other client errors are normal; don't spam ERROR logs
      if (code >= 500) {
        LOGGER.error("Failed to handle request", exception);
      }

      ObjectNode exceptionJson = objectMapper.createObjectNode();
      exceptionJson.put("exceptionType", root.getClass().getName());
      exceptionJson.put("code", code);

      if (message != null) {
        exceptionJson.put("error", message);
      }

      return Response.status(code).entity(exceptionJson).build();
    }

    private static Throwable unwrap(Throwable exception) {
      Throwable cursor = exception;
      while (cursor != null) {
        if (cursor instanceof WebApplicationException || isUniqueConstraintViolation(cursor)) {
          return cursor;
        }
        if (cursor.getCause() == null || cursor.getCause() == cursor) {
          break;
        }
        cursor = cursor.getCause();
      }
      return exception;
    }

    private static boolean isUniqueConstraintViolation(Throwable exception) {
      Throwable cursor = exception;
      while (cursor != null) {
        String name = cursor.getClass().getName();
        String msg = cursor.getMessage() == null ? "" : cursor.getMessage();
        if (name.contains("ConstraintViolationException")
            || msg.contains("unique constraint")
            || msg.contains("duplicate key")) {
          return true;
        }
        cursor = cursor.getCause();
      }
      return false;
    }
  }
}
