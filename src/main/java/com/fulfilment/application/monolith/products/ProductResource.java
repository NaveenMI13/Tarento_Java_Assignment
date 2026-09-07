package com.fulfilment.application.monolith.products;

import com.fulfilment.application.monolith.common.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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

@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductResource {

  @Inject ProductRepository productRepository;

  private static final Logger LOGGER = Logger.getLogger(ProductResource.class.getName());

  @GET
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Product getSingle(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(Product product) {
    if (product.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    if (product.name == null || product.name.isBlank()) {
      throw new WebApplicationException("Product Name was not set on request.", 422);
    }
    if (productRepository.find("name", product.name).firstResult() != null) {
      throw new WebApplicationException("Product with name '" + product.name + "' already exists.", 409);
    }

    productRepository.persist(product);
    return Response.ok(product).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Product update(Long id, Product product) {
    if (product.name == null) {
      throw new WebApplicationException("Product Name was not set on request.", 422);
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }

    Product sameName = productRepository.find("name", product.name).firstResult();
    if (sameName != null && !sameName.id.equals(id)) {
      throw new WebApplicationException("Product with name '" + product.name + "' already exists.", 409);
    }

    entity.name = product.name;
    entity.description = product.description;
    entity.price = product.price;
    entity.stock = product.stock;

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    productRepository.delete(entity);
    return Response.ok(new ApiMessage("Product deleted successfully")).build();
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
      Throwable current = exception;
      while (current.getCause() != null && current.getCause() != current) {
        String name = current.getClass().getName();
        if (name.contains("ArcUndeclaredThrowableException")
            || name.contains("RollbackException")
            || name.contains("PersistenceException")
            || current instanceof RuntimeException) {
          // Keep unwrapping wrapper exceptions to reach ConstraintViolation / WebApplicationException
          if (current.getCause() instanceof WebApplicationException
              || isUniqueConstraintViolation(current.getCause())
              || name.contains("ArcUndeclaredThrowableException")
              || name.contains("RollbackException")) {
            current = current.getCause();
            continue;
          }
        }
        break;
      }
      // Prefer deepest unique-constraint cause when present
      Throwable cursor = exception;
      while (cursor != null) {
        if (isUniqueConstraintViolation(cursor) || cursor instanceof WebApplicationException) {
          return cursor;
        }
        cursor = cursor.getCause();
      }
      return current;
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
