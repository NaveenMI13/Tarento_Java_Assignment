package com.fulfilment.application.monolith.fulfilments;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fulfilment")
public class Fulfilment extends PanacheEntity {

  public Long productId;

  public Long storeId;

  public Long warehouseId;

  public Fulfilment() {}
}
